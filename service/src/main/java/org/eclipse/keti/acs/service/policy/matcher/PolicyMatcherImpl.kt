/*******************************************************************************
 * Copyright 2017 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.service.policy.matcher

import org.eclipse.keti.acs.attribute.readers.AttributeReaderFactory
import org.eclipse.keti.acs.commons.web.UriTemplateUtils
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Policy
import org.eclipse.keti.acs.service.policy.evaluation.MatchedPolicy
import org.eclipse.keti.acs.service.policy.evaluation.ResourceAttributeResolver
import org.eclipse.keti.acs.service.policy.evaluation.SubjectAttributeResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet

private val LOGGER = LoggerFactory.getLogger(PolicyMatcherImpl::class.java)

/**
 * @author acs-engineers@ge.com
 */
@Component
class PolicyMatcherImpl : PolicyMatcher {

    @Autowired
    private lateinit var attributeReaderFactory: AttributeReaderFactory

    override fun match(
        candidate: PolicyMatchCandidate,
        policies: List<Policy>
    ): List<MatchedPolicy> {
        return matchForResult(candidate, policies).matchedPolicies
    }

    override fun matchForResult(
        candidate: PolicyMatchCandidate,
        policies: List<Policy>
    ): MatchResult {
        val resourceAttributeResolver = ResourceAttributeResolver(
            this.attributeReaderFactory.resourceAttributeReader!!,
            candidate.resourceURI!!,
            candidate.supplementalResourceAttributes
        )
        val subjectAttributeResolver = SubjectAttributeResolver(
            this.attributeReaderFactory.subjectAttributeReader!!,
            candidate.subjectIdentifier!!,
            candidate.supplementalSubjectAttributes
        )

        val matchedPolicies = ArrayList<MatchedPolicy>()
        val resolvedResourceUris = HashSet<String>()
        for (policy in policies) {
            val resAttrResolverResult = resourceAttributeResolver.getResult(policy)
            val resourceAttributes = resAttrResolverResult.resourceAttributes
            val subjectAttributes = subjectAttributeResolver.getResult(resourceAttributes)
            if (resAttrResolverResult.isAttributeUriTemplateFound) {
                resolvedResourceUris.add(resAttrResolverResult.resovledResourceUri)
            }
            if (isPolicyMatch(candidate, policy, resourceAttributes, subjectAttributes)) {
                matchedPolicies.add(MatchedPolicy(policy, resourceAttributes, subjectAttributes))
            }
        }
        return MatchResult(matchedPolicies, resolvedResourceUris)
    }

    /**
     * @param candidate policy match candidate
     * @param policy    to match
     * @return true if the policy meets the criteria
     */
    private fun isPolicyMatch(
        candidate: PolicyMatchCandidate,
        policy: Policy,
        resourceAttributes: Set<Attribute>,
        subjectAttributes: Set<Attribute>
    ): Boolean {
        // A policy with no target matches everything.
        if (null == policy.target) {
            return true
        }
        val actionMatch = isActionMatch(candidate.action, policy)

        val subjectMatch = isSubjectMatch(subjectAttributes, policy)

        val resourceMatch = isResourceMatch(candidate.resourceURI, resourceAttributes, policy)

        LOGGER.debug(
            "Checking policy [{}]: Action match ? -> {}, Subject match ? -> {}, Resource match ? -> {}, ",
            policy.name,
            actionMatch,
            subjectMatch,
            resourceMatch
        )

        return actionMatch && subjectMatch && resourceMatch
    }

    private fun isResourceMatch(
        resourceURI: String?,
        resourceAttributes: Set<Attribute>,
        policy: Policy
    ): Boolean {
        if (null == policy.target?.resource) {
            return true
        }

        val uriTemplateMatch = UriTemplateUtils.isCanonicalMatch(policy.target!!.resource!!.uriTemplate, resourceURI)

        if (!uriTemplateMatch) {
            return false
        }

        if (null == policy.target!!.resource!!.attributes) {
            return true
        }

        for (attr in policy.target!!.resource!!.attributes!!) {
            if (!containsAttributeType(attr.issuer!!, attr.name!!, resourceAttributes)) {
                return false
            }
        }
        return true
    }

    private fun isSubjectMatch(
        subjectAttributes: Set<Attribute>,
        policy: Policy
    ): Boolean {
        if (null == policy.target?.subject || null == policy.target!!.subject!!.attributes) {
            return true
        }

        for (attr in policy.target!!.subject!!.attributes) {
            if (!containsAttributeType(attr.issuer!!, attr.name!!, subjectAttributes)) {
                return false
            }
        }
        return true
    }

    private fun isActionMatch(
        requestAction: String?,
        policy: Policy
    ): Boolean {
        // Target's action is not null and they match or the Target's action is
        // null and is considered a match for anything
        var policyActionList = emptyList<String>()
        val policyActionDefined = policy.target != null && policy.target!!.action != null
        if (policyActionDefined) {
            val policyActions = policy.target!!.action!!
            policyActionList =
                Arrays.asList(*policyActions.split("\\s*,\\s*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        }
        return policyActionDefined && requestAction != null && policyActionList.contains(requestAction) || policy.target?.action == null
    }

    private fun containsAttributeType(
        issuer: String,
        name: String,
        attributes: Collection<Attribute>
    ): Boolean {
        for (attr in attributes) {
            if (issuer == attr.issuer && name == attr.name) {
                return true
            }
        }
        return false
    }
}
