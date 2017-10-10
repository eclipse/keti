/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.attribute.readers.AttributeRetrievalException;
import com.ge.predix.acs.commons.policy.condition.ConditionAssertionFailedException;
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
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationCache;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationRequestCacheKey;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationRequestCacheKey.Builder;
import com.ge.predix.acs.privilege.management.dao.AttributeLimitExceededException;
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
    private PolicyEvaluationCache cache;
    @Autowired
    private PolicyManagementService policyService;
    @Autowired
    private PolicyMatcher policyMatcher;
    @Autowired
    private PolicySetValidator policySetValidator;
    @Autowired
    private ZoneResolver zoneResolver;

    @Override
    public PolicyEvaluationResult evalPolicy(final PolicyEvaluationRequestV1 request) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        String uri = request.getResourceIdentifier();
        String subjectIdentifier = request.getSubjectIdentifier();
        String action = request.getAction();
        LinkedHashSet<String> policySetsEvaluationOrder = request.getPolicySetsEvaluationOrder();

        if (uri == null || subjectIdentifier == null || action == null) {
            LOGGER.error("Policy evaluation request is missing required input parameters: "
                    + "resourceURI='{}' subjectIdentifier='{}' action='{}'", uri, subjectIdentifier, action);

            throw new IllegalArgumentException("Policy evaluation request is missing required input parameters. "
                    + "Please review and resubmit the request.");
        }

        List<PolicySet> allPolicySets = this.policyService.getAllPolicySets();

        if (allPolicySets.isEmpty()) {
            return new PolicyEvaluationResult(Effect.NOT_APPLICABLE);
        }

        LinkedHashSet<PolicySet> filteredPolicySets = filterPolicySetsByPriority(subjectIdentifier, uri, allPolicySets,
                policySetsEvaluationOrder);

        // At this point empty evaluation order means we have only one policy set.
        // Fixing policy evaluation order so we could build a cache key.
        PolicyEvaluationRequestCacheKey key;
        if (policySetsEvaluationOrder.isEmpty()) {
            key = new Builder().zoneId(zone.getName())
                    .policySetIds(Stream.of(filteredPolicySets.iterator().next().getName())
                            .collect(Collectors.toCollection(LinkedHashSet::new)))
                    .request(request).build();
        } else {
            key = new Builder().zoneId(zone.getName()).request(request).build();
        }

        PolicyEvaluationResult result = null;
        try {
            result = this.cache.get(key);
        } catch (Exception e) {
            LOGGER.error(String.format("Unable to get cache key '%s'", key), e);
        }

        if (null == result) {
            result = new PolicyEvaluationResult(Effect.NOT_APPLICABLE);

            HashSet<Attribute> supplementalResourceAttributes;
            if (null == request.getResourceAttributes()) {
                supplementalResourceAttributes = new HashSet<>();
            } else {
                supplementalResourceAttributes = new HashSet<>(request.getResourceAttributes());
            }
            HashSet<Attribute> supplementalSubjectAttributes;
            if (null == request.getSubjectAttributes()) {
                supplementalSubjectAttributes = new HashSet<>();
            } else {
                supplementalSubjectAttributes = new HashSet<>(request.getSubjectAttributes());
            }

            for (PolicySet policySet : filteredPolicySets) {
                result = evalPolicySet(policySet, subjectIdentifier, uri, action, supplementalResourceAttributes,
                        supplementalSubjectAttributes);
                if (result.getEffect() != Effect.NOT_APPLICABLE) {
                    break;
                }
            }

            LOGGER.info("Processed Policy Evaluation for: " + "resourceUri='{}', subjectIdentifier='{}', action='{}',"
                    + " result='{}'", uri, subjectIdentifier, action, result.getEffect());

            // A policy evaluation result with an INDETERMINATE effect is almost always due to transient errors.
            // Caching such results will inevitably cause users to get back a stale result for a period of time
            // even when the transient error is fixed.
            if (result.getEffect() != Effect.INDETERMINATE) {
                try {
                    this.cache.set(key, result);
                } catch (Exception e) {
                    LOGGER.error(
                            String.format("Unable to set cache key '%s' to value '%s' due to exception", key, result),
                            e);
                }
            }
        }
        return result;
    }

    LinkedHashSet<PolicySet> filterPolicySetsByPriority(final String subjectIdentifier, final String uri,
            final List<PolicySet> allPolicySets, final LinkedHashSet<String> policySetsEvaluationOrder)
            throws IllegalArgumentException {

        if (policySetsEvaluationOrder.isEmpty()) {
            if (allPolicySets.size() > 1) {
                LOGGER.error(
                        "Found more than one policy set during policy evaluation and "
                                + "no evaluation order is provided. subjectIdentifier='{}', resourceURI='{}'",
                        subjectIdentifier, uri);
                throw new IllegalArgumentException("More than one policy set exists for this zone. "
                        + "Please provide an ordered list of policy set names to consider for this evaluation and "
                        + "resubmit the request.");
            } else {
                return new LinkedHashSet<>(allPolicySets);
            }
        }

        Map<String, PolicySet> allPolicySetsMap = allPolicySets.stream()
                .collect(Collectors.toMap(PolicySet::getName, Function.identity()));
        LinkedHashSet<PolicySet> filteredPolicySets = new LinkedHashSet<>();
        for (String policySetId : policySetsEvaluationOrder) {
            PolicySet policySet = allPolicySetsMap.get(policySetId);
            if (policySet == null) {
                LOGGER.error("No existing policy set matches policy set in the evaluation order of the request. "
                        + "Subject: {}, Resource: {}", subjectIdentifier, uri);
                throw new IllegalArgumentException(
                        "No existing policy set matches policy set in the evaluaion order of the request. "
                                + "Please review the policy evauation order and resubmit the request.");
            } else {
                filteredPolicySets.add(policySet);
            }
        }
        return filteredPolicySets;
    }

    private PolicyEvaluationResult evalPolicySet(final PolicySet policySet, final String subjectIdentifier,
            final String resourceURI, final String action, final Set<Attribute> supplementalResourceAttributes,
            final Set<Attribute> supplementalSubjectAttributes) {

        PolicyEvaluationResult result;
        try {
            MatchResult matchResult = matchPolicies(subjectIdentifier, resourceURI, action, policySet.getPolicies(),
                    supplementalResourceAttributes, supplementalSubjectAttributes);

            Effect effect = Effect.NOT_APPLICABLE;
            Set<String> resolvedResourceUris = matchResult.getResolvedResourceUris();
            resolvedResourceUris.add(resourceURI);

            Set<Attribute> resourceAttributes = Collections.emptySet();
            Set<Attribute> subjectAttributes = Collections.emptySet();
            ConditionShell groovyShell = null;
            List<MatchedPolicy> matchedPolicies = matchResult.getMatchedPolicies();
            for (MatchedPolicy matchedPolicy : matchedPolicies) {
                Policy policy = matchedPolicy.getPolicy();
                resourceAttributes = matchedPolicy.getResourceAttributes();
                subjectAttributes = matchedPolicy.getSubjectAttributes();
                Target target = policy.getTarget();
                String resourceURITemplate = null;
                if (target != null && target.getResource() != null) {
                    resourceURITemplate = target.getResource().getUriTemplate();
                }

                boolean conditionEvaluationResult = true;
                if (!policy.getConditions().isEmpty()) {
                    if (null == groovyShell) {
                        groovyShell = new GroovyConditionShell();
                    }
                    conditionEvaluationResult = evaluateConditions(subjectAttributes, resourceAttributes, resourceURI,
                            policy.getConditions(), resourceURITemplate);
                }
                LOGGER.debug("Checking condition of policy '{}': Condition evaluated to ? -> {}, policy effect {}",
                        policy.getName(), conditionEvaluationResult, policy.getEffect());

                LOGGER.debug("Condition Eval: {} Result: {}", policy.getConditions(), conditionEvaluationResult);
                if (conditionEvaluationResult) {
                    effect = policy.getEffect();
                    LOGGER.info("Condition Evaluation success: policy set name='{}', policy name='{}', effect='{}'",
                            policySet.getName(), policy.getName(), policy.getEffect());
                    break;
                }
            }
            result = new PolicyEvaluationResult(effect, subjectAttributes, new ArrayList<>(resourceAttributes),
                    resolvedResourceUris);
        } catch (Exception e) {
            result = handlePolicyEvaluationException(policySet, subjectIdentifier, resourceURI, e);
        }
        return result;
    }

    private PolicyEvaluationResult handlePolicyEvaluationException(final PolicySet policySet,
            final String subjectIdentifier, final String resourceURI, final Throwable e) {
        PolicyEvaluationResult result = new PolicyEvaluationResult(Effect.INDETERMINATE);
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Exception occured while evaluating the policy set. Policy Set ID:'")
                .append(policySet.getName()).append("' subject:'").append(subjectIdentifier)
                .append("', Resource URI: '").append(resourceURI).append("'");
        if (e instanceof AttributeLimitExceededException || e instanceof AttributeRetrievalException) {
            result.setMessage(e.getMessage());
        }
        LOGGER.error("{}", logMessage, e);
        return result;
    }

    boolean evaluateConditions(final Set<Attribute> subjectAttributes, final Set<Attribute> resourceAttributes,
            final String resourceURI, final List<Condition> conditions, final String resourceURITemplate) {
        List<ConditionScript> validatedConditionScripts;
        try {
            validatedConditionScripts = this.policySetValidator.validatePolicyConditions(conditions);
        } catch (PolicySetValidationException e) {
            LOGGER.error("Unable to validate conditions: {}", e.getMessage());
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
            } catch (ConditionAssertionFailedException e) {
                LOGGER.debug("Condition Assertion Failed", e);
                result = false;
            } catch (Exception e) {
                LOGGER.error("Unable to evualate condition: {}", conditions.get(i), e);
                throw new PolicyEvaluationException("Condition Evaluation failed", e);
            }
        }
        return result;
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

    private MatchResult matchPolicies(final String subjectIdentifier, final String resourceURI, final String action,
            final List<Policy> allPolicies, final Set<Attribute> supplementalResourceAttributes,
            final Set<Attribute> supplementalSubjectAttributes) {
        PolicyMatchCandidate criteria = new PolicyMatchCandidate(action, resourceURI, subjectIdentifier,
                supplementalResourceAttributes, supplementalSubjectAttributes);
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
