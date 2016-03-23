/*******************************************************************************
 * Copyright 2016 General Electric Company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.ge.predix.acs.service.policy.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.commons.policy.condition.ConditionScript;
import com.ge.predix.acs.commons.policy.condition.ConditionShell;
import com.ge.predix.acs.commons.policy.condition.ResourceHandler;
import com.ge.predix.acs.commons.policy.condition.SubjectHandler;
import com.ge.predix.acs.commons.policy.condition.groovy.AttributeMatcher;
import com.ge.predix.acs.commons.policy.condition.groovy.GroovyConditionShell;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Condition;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.model.Policy;
import com.ge.predix.acs.model.PolicySet;
import com.ge.predix.acs.model.Target;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationCacheCircuitBreaker;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationRequestCacheKey;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationRequestCacheKey.Builder;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.acs.service.policy.admin.PolicyManagementService;
import com.ge.predix.acs.service.policy.matcher.MatchResult;
import com.ge.predix.acs.service.policy.matcher.PolicyMatchCandidate;
import com.ge.predix.acs.service.policy.matcher.PolicyMatcher;
import com.ge.predix.acs.service.policy.validation.PolicySetValidationException;
import com.ge.predix.acs.service.policy.validation.PolicySetValidator;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.resolver.ZoneResolver;

@Component
@SuppressWarnings({ "javadoc", "nls" })
public class PolicyEvaluationServiceImpl implements PolicyEvaluationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyEvaluationServiceImpl.class);

    @Autowired
    private PolicyEvaluationCacheCircuitBreaker cache;
    @Autowired
    private PolicyManagementService policyService;
    @Autowired
    private PolicyMatcher policyMatcher;
    @Autowired
    private PolicySetValidator policySetValidator;
    @Autowired
    private PrivilegeManagementService privilegeService;
    @Autowired
    private ZoneResolver zoneResolver;

    @Override
    public PolicyEvaluationResult evalPolicy(final PolicyEvaluationRequestV1 request) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        PolicyEvaluationRequestCacheKey key = new Builder().zoneId(zone.getName()).policySetId("default")
                .request(request).build();
        PolicyEvaluationResult result = this.cache.get(key);
        if (null == result) {
            result = evalPolicy(request.getResourceIdentifier(), request.getSubjectIdentifier(), request.getAction(),
                    request.getSubjectAttributes());
            this.cache.set(key, result);
        }
        return result;
    }

    @Override
    public PolicyEvaluationResult evalPolicy(final String resourceURI, final String subjectIdentifier,
            final String action, final List<Attribute> attributes) {

        if (resourceURI == null || subjectIdentifier == null || action == null) {

            LOGGER.error(String.format(
                    "PolicyEvaluationResult input paramters cannot be null, "
                            + "resourceURI=[%s] subjectIdentifier=[%s] action=[%s]",
                    resourceURI, subjectIdentifier, action));

            throw new IllegalArgumentException(
                    "ACS Internal Error: PolicyEvaluationResult input paramters cannot be null.");
        }

        List<PolicySet> allPolicySets = this.policyService.getAllPolicySets();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format(
                    "Policy Evaluation in the context of: " + "resourceUri=[%s], subjectIdentifier=[%s], action=[%s],"
                            + " number of policy sets found=[%s]",
                    resourceURI, subjectIdentifier, action, allPolicySets.size()));
        }

        if (allPolicySets.isEmpty()) {
            return new PolicyEvaluationResult(Effect.NOT_APPLICABLE);
        } else if (allPolicySets.size() > 1) {
            LOGGER.error("Found more than one policy set during policy evaluation. Subject: " + subjectIdentifier
                    + ", Resource: " + resourceURI);
            throw new IllegalArgumentException(
                    "Request to create policy set rejected. Only one policy set is supported.");
        } else {

            Set<Attribute> subjectAttributes = resolveSubjectAttributes(subjectIdentifier, attributes);

            // NOTE: When multiple policy sets are supported, this code needs to
            // delegate to a "InterPolicySetDecisionAggregator"
            return this.evalPolicySet(allPolicySets.get(0), subjectIdentifier, resourceURI, action, subjectAttributes);
        }
    }

    /**
     * @param subjectID
     * @param policyEvaluationRequestAttributes
     * @return
     */
    private Set<Attribute> resolveSubjectAttributes(final String subjectIdentifier,
            final List<Attribute> policyEvaluationRequestAttributes) {
        Set<Attribute> mergedSubjectAttributes = new HashSet<Attribute>();

        BaseSubject subject = this.privilegeService.getBySubjectIdentifier(subjectIdentifier);
        if (subject != null) {
            mergedSubjectAttributes.addAll(subject.getAttributes());
        }

        if (policyEvaluationRequestAttributes != null) {
            mergedSubjectAttributes.addAll(policyEvaluationRequestAttributes);
        }

        return mergedSubjectAttributes;
    }

    private PolicyEvaluationResult evalPolicySet(final PolicySet policySet, final String subjectIdentifier,
            final String resourceURI, final String action, final Set<Attribute> subjectAttributes) {

        PolicyEvaluationResult result;
        try {
            // Set<Attribute> resourceAttributes = getResourceAttributes(resourceURI);
            MatchResult matchResult = matchPolicies(subjectAttributes, subjectIdentifier, resourceURI, action,
                    policySet.getPolicies());

            Effect effect = Effect.NOT_APPLICABLE;
            Set<String> resolvedResourceUris = matchResult.getResolvedResourceUris();
            if (null == resolvedResourceUris) {
                resolvedResourceUris = new HashSet<>();
            }
            resolvedResourceUris.add(resourceURI);

            Set<Attribute> resourceAttributes = Collections.emptySet();
            ConditionShell groovyShell = null;
            List<MatchedPolicy> matchedPolicies = matchResult.getMatchedPolicies();
            for (MatchedPolicy matchedPolicy : matchedPolicies) {
                Policy policy = matchedPolicy.getPolicy();
                resourceAttributes = matchedPolicy.getResourceAttributes();
                Target target = policy.getTarget();
                String resourceURITemplate = null;
                if (target != null && target.getResource() != null) {
                    resourceURITemplate = target.getResource().getUriTemplate();
                }

                boolean conditionEvaluationResult = true;
                if (0 < policy.getConditions().size()) {
                    if (null == groovyShell) {
                        groovyShell = new GroovyConditionShell();
                    }
                    conditionEvaluationResult = evaluateConditions(subjectAttributes, resourceAttributes, resourceURI,
                            policy.getConditions(), resourceURITemplate, groovyShell);
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format(
                            "Checking condition of policy [%s]: Condition evaluated to ? -> %s, policy effect %s",
                            policy.getName(), conditionEvaluationResult, policy.getEffect()));
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Condition Eval: " + policy.getConditions() + " Result: " + conditionEvaluationResult);
                }
                if (conditionEvaluationResult) {
                    effect = policy.getEffect();
                    LOGGER.info(String.format(
                            "Policy Evaluation decision : " + "policy set name=[%s], policy name=[%s], effect=[%s]",
                            policySet.getName(), policy.getName(), policy.getEffect()));
                    break;
                }
            }

            result = new PolicyEvaluationResult(effect, new ArrayList<Attribute>(subjectAttributes),
                    new ArrayList<Attribute>(resourceAttributes), resolvedResourceUris);

        } catch (Throwable e) {
            StringBuilder builder = new StringBuilder();
            builder.append("Exception occured while evaulating the policy set. Policy Set ID:")
                    .append(policySet.getName()).append(" subject:").append(subjectIdentifier)
                    .append(", Resource URI: ").append(resourceURI);
            LOGGER.error(builder.toString(), e);
            result = new PolicyEvaluationResult(Effect.INDETERMINATE);
        }

        return result;
    }

    /**
     * @param subjectHandler
     * @param resourceHandler
     * @param conditions
     */
    boolean evaluateConditions(final Set<Attribute> subjectAttributes, final Set<Attribute> resourceAttributes,
            final String resourceURI, final List<Condition> conditions, final String resourceURITemplate,
            final ConditionShell groovyShell) {
        List<ConditionScript> validatedConditionScripts;
        try {
            validatedConditionScripts = this.policySetValidator.validatePolicyConditions(conditions);
        } catch (PolicySetValidationException e) {
            LOGGER.error("Unable to validate conditions: " + e.getMessage());
            throw new PolicyEvaluationException("Condition Validation failed", e);
        }

        debugAttributes(subjectAttributes, resourceAttributes);

        Map<String, Object> attributeBindingsMap = this.getAttributeBindingsMap(subjectAttributes, resourceAttributes,
                resourceURI, resourceURITemplate);

        boolean result = true;
        for (int i = 0; i < validatedConditionScripts.size(); i++) {
            ConditionScript conditionScript = validatedConditionScripts.get(i);
            try {
                result = result && conditionScript.execute(attributeBindingsMap);
            } catch (Throwable e) {
                LOGGER.error("Unable to evualate condition: " + conditions.get(i).toString(), e);
                throw new PolicyEvaluationException("Condition Evaluation failed", e);
            }
        }

        return result;
    }

    Set<Attribute> getSubjectAttributes(final String subjectIdentifier) {
        Set<Attribute> subjectAttributes = Collections.emptySet();
        BaseSubject subject = this.privilegeService.getBySubjectIdentifier(subjectIdentifier);
        if (subject != null) {
            subjectAttributes = subject.getAttributes();
        }
        return subjectAttributes;
    }

    private Map<String, Object> getAttributeBindingsMap(final Set<Attribute> subjectAttributes,
            final Set<Attribute> resourceAttributes, final String resourceURI, final String resourceURITemplate) {
        SubjectHandler subjectHandler = new SubjectHandler(subjectAttributes);
        ResourceHandler resourceHandler = new ResourceHandler(resourceAttributes, resourceURI, resourceURITemplate);

        Map<String, Object> attributeHandler = new HashMap<>();
        attributeHandler.put("resource", resourceHandler);
        attributeHandler.put("subject", subjectHandler);
        attributeHandler.put("match", new AttributeMatcher());
        return attributeHandler;
    }

    private MatchResult matchPolicies(final Set<Attribute> subjectAttributes, final String subjectIdentifier,
            final String resourceURI, final String action, final List<Policy> allPolicies) {

        PolicyMatchCandidate criteria = new PolicyMatchCandidate(action, resourceURI, subjectIdentifier,
                new ArrayList<Attribute>(subjectAttributes));

        return this.policyMatcher.matchForResult(criteria, allPolicies);
    }

    private void debugAttributes(final Set<Attribute> subjectAttributes, final Set<Attribute> resourceAttributes) {
        if (LOGGER.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Subject Attributes :\n");
            Iterator<Attribute> subjectAttributesItr = subjectAttributes.iterator();
            while (subjectAttributesItr.hasNext()) {
                sb.append(subjectAttributesItr.next().toString() + "\n");
            }
            sb.append("Resource Attributes :\n");
            Iterator<Attribute> resourceAttributesIte = resourceAttributes.iterator();
            while (resourceAttributesIte.hasNext()) {
                sb.append(resourceAttributesIte.next().toString() + "\n");
            }
            LOGGER.debug(sb.toString());
        }
    }

}
