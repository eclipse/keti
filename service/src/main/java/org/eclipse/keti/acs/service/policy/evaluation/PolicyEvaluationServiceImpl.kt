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

package org.eclipse.keti.acs.service.policy.evaluation

import org.eclipse.keti.acs.attribute.readers.AttributeRetrievalException
import org.eclipse.keti.acs.commons.policy.condition.ConditionAssertionFailedException
import org.eclipse.keti.acs.commons.policy.condition.ConditionScript
import org.eclipse.keti.acs.commons.policy.condition.ResourceHandler
import org.eclipse.keti.acs.commons.policy.condition.SubjectHandler
import org.eclipse.keti.acs.commons.policy.condition.groovy.AttributeMatcher
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Condition
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.model.Policy
import org.eclipse.keti.acs.model.PolicySet
import org.eclipse.keti.acs.policy.evaluation.cache.PolicyEvaluationCache
import org.eclipse.keti.acs.policy.evaluation.cache.PolicyEvaluationRequestCacheKey
import org.eclipse.keti.acs.policy.evaluation.cache.PolicyEvaluationRequestCacheKey.Builder
import org.eclipse.keti.acs.privilege.management.dao.AttributeLimitExceededException
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.eclipse.keti.acs.service.policy.admin.PolicyManagementService
import org.eclipse.keti.acs.service.policy.matcher.MatchResult
import org.eclipse.keti.acs.service.policy.matcher.PolicyMatchCandidate
import org.eclipse.keti.acs.service.policy.matcher.PolicyMatcher
import org.eclipse.keti.acs.service.policy.validation.PolicySetValidationException
import org.eclipse.keti.acs.service.policy.validation.PolicySetValidator
import org.eclipse.keti.acs.zone.resolver.ZoneResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedHashSet

private val LOGGER = LoggerFactory.getLogger(PolicyEvaluationServiceImpl::class.java)

@Component
class PolicyEvaluationServiceImpl : PolicyEvaluationService {

    @Autowired
    private lateinit var cache: PolicyEvaluationCache
    @Autowired
    private lateinit var policyService: PolicyManagementService
    @Autowired
    private lateinit var policyMatcher: PolicyMatcher
    @Autowired
    private lateinit var policySetValidator: PolicySetValidator
    @Autowired
    private lateinit var zoneResolver: ZoneResolver

    override fun evalPolicy(request: PolicyEvaluationRequestV1): PolicyEvaluationResult {
        val zone = this.zoneResolver.zoneEntityOrFail
        val uri = request.resourceIdentifier
        val subjectIdentifier = request.subjectIdentifier
        val action = request.action
        val policySetsEvaluationOrder = request.policySetsEvaluationOrder

        if (uri == null || subjectIdentifier == null || action == null) {
            LOGGER.error(
                "Policy evaluation request is missing required input parameters: " + "resourceURI='{}' subjectIdentifier='{}' action='{}'",
                uri,
                subjectIdentifier,
                action
            )

            throw IllegalArgumentException("Policy evaluation request is missing required input parameters. " + "Please review and resubmit the request.")
        }

        val allPolicySets = this.policyService.allPolicySets

        if (allPolicySets.isEmpty()) {
            return PolicyEvaluationResult(Effect.NOT_APPLICABLE)
        }

        val filteredPolicySets = filterPolicySetsByPriority(
            subjectIdentifier, uri, allPolicySets,
            policySetsEvaluationOrder
        )

        // At this point empty evaluation order means we have only one policy set.
        // Fixing policy evaluation order so we could build a cache key.
        val key: PolicyEvaluationRequestCacheKey = if (policySetsEvaluationOrder.isEmpty()) {
            Builder().zoneId(zone.name)
                .policySetIds(
                    LinkedHashSet(setOf(filteredPolicySets.iterator().next().name))
                )
                .request(request).build()
        } else {
            Builder().zoneId(zone.name).request(request).build()
        }

        var result: PolicyEvaluationResult? = null
        try {
            result = this.cache.get(key)
        } catch (e: Exception) {
            LOGGER.error(String.format("Unable to get cache key '%s'", key), e)
        }

        if (null == result) {
            result = PolicyEvaluationResult(Effect.NOT_APPLICABLE)

            val supplementalResourceAttributes: HashSet<Attribute> = if (null == request.resourceAttributes) {
                HashSet()
            } else {
                HashSet(request.resourceAttributes)
            }
            val supplementalSubjectAttributes: HashSet<Attribute> = if (null == request.subjectAttributes) {
                HashSet()
            } else {
                HashSet(request.subjectAttributes)
            }

            for (policySet in filteredPolicySets) {
                result = evalPolicySet(
                    policySet, subjectIdentifier, uri, action, supplementalResourceAttributes,
                    supplementalSubjectAttributes
                )
                if (result.effect != Effect.NOT_APPLICABLE) {
                    break
                }
            }

            LOGGER.info(
                "Processed Policy Evaluation for: " + "resourceUri='{}', subjectIdentifier='{}', action='{}',"
                + " result='{}'", uri, subjectIdentifier, action, result!!.effect
            )

            // A policy evaluation result with an INDETERMINATE effect is almost always due to transient errors.
            // Caching such results will inevitably cause users to get back a stale result for a period of time
            // even when the transient error is fixed.
            if (result.effect != Effect.INDETERMINATE) {
                try {
                    this.cache.set(key, result)
                } catch (e: Exception) {
                    LOGGER.error(
                        String.format("Unable to set cache key '%s' to value '%s' due to exception", key, result),
                        e
                    )
                }
            }
        }
        return result
    }

    @Throws(IllegalArgumentException::class)
    fun filterPolicySetsByPriority(
        subjectIdentifier: String, uri: String,
        allPolicySets: List<PolicySet>, policySetsEvaluationOrder: LinkedHashSet<String>
    ): LinkedHashSet<PolicySet> {

        if (policySetsEvaluationOrder.isEmpty()) {
            if (allPolicySets.size > 1) {
                LOGGER.error(
                    "Found more than one policy set during policy evaluation and " + "no evaluation order is provided. subjectIdentifier='{}', resourceURI='{}'",
                    subjectIdentifier, uri
                )
                throw IllegalArgumentException(
                    "More than one policy set exists for this zone. "
                    + "Please provide an ordered list of policy set names to consider for this evaluation and "
                    + "resubmit the request."
                )
            } else {
                return LinkedHashSet(allPolicySets)
            }
        }

        val allPolicySetsMap = allPolicySets.map { it.name to it }.toMap()
        val filteredPolicySets = LinkedHashSet<PolicySet>()
        for (policySetId in policySetsEvaluationOrder) {
            val policySet = allPolicySetsMap[policySetId]
            if (policySet == null) {
                LOGGER.error(
                    "No existing policy set matches policy set in the evaluation order of the request. " + "Subject: {}, Resource: {}",
                    subjectIdentifier,
                    uri
                )
                throw IllegalArgumentException(
                    "No existing policy set matches policy set in the evaluaion order of the request. " + "Please review the policy evauation order and resubmit the request."
                )
            } else {
                filteredPolicySets.add(policySet)
            }
        }
        return filteredPolicySets
    }

    private fun evalPolicySet(
        policySet: PolicySet, subjectIdentifier: String,
        resourceURI: String, action: String, supplementalResourceAttributes: Set<Attribute>,
        supplementalSubjectAttributes: Set<Attribute>
    ): PolicyEvaluationResult {

        var result: PolicyEvaluationResult
        try {
            val matchResult = matchPolicies(
                subjectIdentifier, resourceURI, action, policySet.policies,
                supplementalResourceAttributes, supplementalSubjectAttributes
            )

            var effect = Effect.NOT_APPLICABLE
            val resolvedResourceUris = matchResult.resolvedResourceUris
            resolvedResourceUris.add(resourceURI)

            var resourceAttributes = emptySet<Attribute>()
            var subjectAttributes = emptySet<Attribute>()
            val matchedPolicies = matchResult.matchedPolicies
            for (matchedPolicy in matchedPolicies) {
                val policy = matchedPolicy.policy
                resourceAttributes = matchedPolicy.resourceAttributes
                subjectAttributes = matchedPolicy.subjectAttributes
                val target = policy.target
                var resourceURITemplate: String? = null
                if (target != null && target.resource != null) {
                    resourceURITemplate = target.resource.uriTemplate
                }

                var conditionEvaluationResult = true
                if (!policy.conditions.isEmpty()) {
                    conditionEvaluationResult = evaluateConditions(
                        subjectAttributes, resourceAttributes, resourceURI,
                        policy.conditions, resourceURITemplate
                    )
                }
                LOGGER.debug(
                    "Checking condition of policy '{}': Condition evaluated to ? -> {}, policy effect {}",
                    policy.name, conditionEvaluationResult, policy.effect
                )

                LOGGER.debug("Condition Eval: {} Result: {}", policy.conditions, conditionEvaluationResult)
                if (conditionEvaluationResult) {
                    effect = policy.effect
                    LOGGER.info(
                        "Condition Evaluation success: policy set name='{}', policy name='{}', effect='{}'",
                        policySet.name, policy.name, policy.effect
                    )
                    break
                }
            }
            result = PolicyEvaluationResult(
                effect, subjectAttributes, ArrayList(resourceAttributes),
                resolvedResourceUris
            )
        } catch (e: Exception) {
            result = handlePolicyEvaluationException(policySet, subjectIdentifier, resourceURI, e)
        }

        return result
    }

    private fun handlePolicyEvaluationException(
        policySet: PolicySet,
        subjectIdentifier: String, resourceURI: String, e: Throwable
    ): PolicyEvaluationResult {
        val result = PolicyEvaluationResult(Effect.INDETERMINATE)
        val logMessage = StringBuilder()
        logMessage.append("Exception occured while evaluating the policy set. Policy Set ID:'")
            .append(policySet.name).append("' subject:'").append(subjectIdentifier)
            .append("', Resource URI: '").append(resourceURI).append("'")
        if (e is AttributeLimitExceededException || e is AttributeRetrievalException) {
            result.message = e.message
        }
        LOGGER.error("{}", logMessage, e)
        return result
    }

    fun evaluateConditions(
        subjectAttributes: Set<Attribute>, resourceAttributes: Set<Attribute>,
        resourceURI: String, conditions: List<Condition>, resourceURITemplate: String?
    ): Boolean {
        val validatedConditionScripts: List<ConditionScript>
        try {
            validatedConditionScripts = this.policySetValidator.validatePolicyConditions(conditions)
        } catch (e: PolicySetValidationException) {
            LOGGER.error("Unable to validate conditions: {}", e.message)
            throw PolicyEvaluationException("Condition Validation failed", e)
        }

        debugAttributes(subjectAttributes, resourceAttributes)

        val attributeBindingsMap = this.getAttributeBindingsMap(
            subjectAttributes, resourceAttributes,
            resourceURI, resourceURITemplate
        )

        var result = true
        for (i in validatedConditionScripts.indices) {
            val conditionScript = validatedConditionScripts[i]
            result = try {
                result && conditionScript.execute(attributeBindingsMap)
            } catch (e: ConditionAssertionFailedException) {
                LOGGER.debug("Condition Assertion Failed", e)
                false
            } catch (e: Exception) {
                LOGGER.error("Unable to evualate condition: {}", conditions[i], e)
                throw PolicyEvaluationException("Condition Evaluation failed", e)
            }
        }
        return result
    }

    private fun getAttributeBindingsMap(
        subjectAttributes: Set<Attribute>,
        resourceAttributes: Set<Attribute>, resourceURI: String, resourceURITemplate: String?
    ): Map<String, Any> {
        val subjectHandler = SubjectHandler(subjectAttributes)
        val resourceHandler = ResourceHandler(resourceAttributes, resourceURI, resourceURITemplate)

        val attributeHandler = HashMap<String, Any>()
        attributeHandler["resource"] = resourceHandler
        attributeHandler["subject"] = subjectHandler
        attributeHandler["match"] = AttributeMatcher()
        return attributeHandler
    }

    private fun matchPolicies(
        subjectIdentifier: String, resourceURI: String, action: String,
        allPolicies: List<Policy>, supplementalResourceAttributes: Set<Attribute>,
        supplementalSubjectAttributes: Set<Attribute>
    ): MatchResult {
        val criteria = PolicyMatchCandidate(
            action, resourceURI, subjectIdentifier,
            supplementalResourceAttributes, supplementalSubjectAttributes
        )
        return this.policyMatcher.matchForResult(criteria, allPolicies)
    }

    private fun debugAttributes(subjectAttributes: Set<Attribute>, resourceAttributes: Set<Attribute>) {
        if (LOGGER.isDebugEnabled) {
            val sb = StringBuilder()
            sb.append("Subject Attributes :\n")
            val subjectAttributesItr = subjectAttributes.iterator()
            while (subjectAttributesItr.hasNext()) {
                sb.append(subjectAttributesItr.next().toString() + "\n")
            }
            sb.append("Resource Attributes :\n")
            val resourceAttributesIte = resourceAttributes.iterator()
            while (resourceAttributesIte.hasNext()) {
                sb.append(resourceAttributesIte.next().toString() + "\n")
            }
            LOGGER.debug(sb.toString())
        }
    }
}
