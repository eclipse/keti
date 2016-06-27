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

package com.ge.predix.acs.service.policy.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.commons.web.UriTemplateUtils;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Policy;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.service.policy.evaluation.MatchedPolicy;
import com.ge.predix.acs.service.policy.evaluation.ResourceAttributeResolver;
import com.ge.predix.acs.service.policy.evaluation.ResourceAttributeResolver.ResourceAttributeResolverResult;
import com.ge.predix.acs.service.policy.evaluation.SubjectAttributeResolver;

/**
 *
 * @author 212314537
 */
@Component
public class PolicyMatcherImpl implements PolicyMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyMatcherImpl.class);

    @Autowired
    private PrivilegeManagementService privilegeManagementService;

    @Override
    public List<MatchedPolicy> match(final PolicyMatchCandidate candidate, final List<Policy> policies) {
        return matchForResult(candidate, policies).getMatchedPolicies();
    }

    @Override
    public MatchResult matchForResult(final PolicyMatchCandidate candidate, final List<Policy> policies) {
        ResourceAttributeResolver resourceAttributeResolver = new ResourceAttributeResolver(
                this.privilegeManagementService, candidate.getResourceURI(),
                candidate.getSupplementalResourceAttributes());
        SubjectAttributeResolver subjectAttributeResolver = new SubjectAttributeResolver(
                this.privilegeManagementService, candidate.getSubjectIdentifier(),
                candidate.getSupplementalSubjectAttributes());

        List<MatchedPolicy> matchedPolicies = new ArrayList<>();
        Set<String> resolvedResourceUris = new HashSet<>();
        for (Policy policy : policies) {
            ResourceAttributeResolverResult resAttrResolverResult = resourceAttributeResolver.getResult(policy);
            Set<Attribute> resourceAttributes = resAttrResolverResult.getResourceAttributes();
            Set<Attribute> subjectAttributes = subjectAttributeResolver.getResult(resourceAttributes);
            if (resAttrResolverResult.isAttributeUriTemplateFound()) {
                resolvedResourceUris.add(resAttrResolverResult.getResovledResourceUri());
            }
            if (isPolicyMatch(candidate, policy, resourceAttributes, subjectAttributes)) {
                matchedPolicies.add(new MatchedPolicy(policy, resourceAttributes, subjectAttributes));
            }
        }
        return new MatchResult(matchedPolicies, resolvedResourceUris);
    }

    /**
     * @param candidate
     *            policy match candidate
     * @param policiy
     *            to match
     * @return true if the policy meets the criteria
     */
    @SuppressWarnings("nls")
    private boolean isPolicyMatch(final PolicyMatchCandidate candidate, final Policy policy,
            final Set<Attribute> resourceAttributes, final Set<Attribute> subjectAttributes) {
        // A policy with no target matches everything.
        if (null == policy.getTarget()) {
            return true;
        }
        boolean actionMatch = isActionMatch(candidate.getAction(), policy);

        boolean subjectMatch = isSubjectMatch(subjectAttributes, policy);

        boolean resourceMatch = isResourceMatch(candidate.getResourceURI(), resourceAttributes, policy);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format(
                    "Checking policy [%s]: Action match ? -> %s, Subject match ? -> %s, Resource match ? -> %s, ",
                    policy.getName(), actionMatch, subjectMatch, resourceMatch));
        }

        return actionMatch && subjectMatch && resourceMatch;
    }

    private boolean isResourceMatch(final String resourceURI, final Set<Attribute> resourceAttributes,
            final Policy policy) {
        if (null == policy.getTarget().getResource()) {
            return true;
        }

        boolean uriTemplateMatch = UriTemplateUtils.isCanonicalMatch(policy.getTarget().getResource().getUriTemplate(),
                resourceURI);

        if (!uriTemplateMatch) {
            return false;
        }

        if (null == policy.getTarget().getResource().getAttributes()) {
            return true;
        }

        for (Attribute attr : policy.getTarget().getResource().getAttributes()) {
            if (!containsAttributeType(attr.getIssuer(), attr.getName(), resourceAttributes)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSubjectMatch(final Set<Attribute> subjectAttributes, final Policy policy) {
        if ((null == policy.getTarget().getSubject()) || (null == policy.getTarget().getSubject().getAttributes())) {
            return true;
        }

        for (Attribute attr : policy.getTarget().getSubject().getAttributes()) {
            if (!containsAttributeType(attr.getIssuer(), attr.getName(), subjectAttributes)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("nls")
    private boolean isActionMatch(final String requestAction, final Policy policy) {
        // Target's action is not null and they match or the Target's action is
        // null and is considered a match for anything
        List<String> policyActionList = Collections.emptyList();
        boolean policyActionDefined = (policy.getTarget() != null) && (policy.getTarget().getAction() != null);
        if (policyActionDefined) {
            String policyActions = policy.getTarget().getAction();
            policyActionList = Arrays.asList(policyActions.split("\\s*,\\s*"));
        }
        return (policyActionDefined && (requestAction != null) && policyActionList.contains(requestAction))
                || (policy.getTarget().getAction() == null);
    }

    private boolean containsAttributeType(final String issuer, final String name,
            final Collection<Attribute> attributes) {
        for (Attribute attr : attributes) {
            if (issuer.equals(attr.getIssuer()) && name.equals(attr.getName())) {
                return true;
            }
        }
        return false;
    }
}
