/*******************************************************************************
 * Copyright 2018 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package org.eclipse.keti.acs.service.policy.validation

import com.github.fge.jsonschema.main.JsonSchemaFactory
import org.eclipse.keti.acs.commons.policy.condition.ConditionScript
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionCache
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionShell
import org.eclipse.keti.acs.model.Condition
import org.eclipse.keti.acs.model.Policy
import org.eclipse.keti.acs.model.PolicySet
import org.eclipse.keti.acs.utils.JsonUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.ArrayList
import java.util.HashSet
import javax.annotation.PostConstruct

private val LOGGER = LoggerFactory.getLogger(PolicySetValidatorImpl::class.java)

private val JSONUTILS = JsonUtils()
private val JSONSCHEMA = JSONUTILS.readJsonNodeFromFile("acs-policy-set-schema.json")

/**
 * @author acs-engineers@ge.com
 */
@Component
class PolicySetValidatorImpl : PolicySetValidator {

    @Autowired
    private lateinit var conditionCache: GroovyConditionCache

    @Autowired
    private lateinit var conditionShell: GroovyConditionShell

    @Value("\${validAcsPolicyHttpActions:GET, POST, PUT, DELETE, PATCH, SUBSCRIBE, MESSAGE}")
    private var validAcsPolicyHttpActions: String = "GET, POST, PUT, DELETE, PATCH, SUBSCRIBE, MESSAGE"

    private lateinit var validAcsPolicyHttpActionsSet: Set<String>

    override fun removeCachedConditions(policySet: PolicySet) {
        for (policy in policySet.policies) {
            for (condition in policy.conditions) {
                this.conditionCache.remove(condition.condition!!)
            }
        }
    }

    override fun validatePolicySet(policySet: PolicySet) {
        validateSchema(policySet)
        for (p in policySet.policies) {
            validatePolicyConditions(p.conditions)
            validatePolicyActions(p)
        }
    }

    private fun validatePolicyActions(p: Policy) {

        if (p.target != null && p.target!!.action != null) {
            val policyActions = p.target!!.action!!
            // Empty actions will be treated as null actions which behave like
            // match any
            // during policy evaluation
            if (policyActions.trim { it <= ' ' }.isEmpty()) {
                p.target!!.action = null
                return
            }
            for (action in policyActions.split("\\s*,\\s*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (!this.validAcsPolicyHttpActionsSet.contains(action)) {
                    throw PolicySetValidationException(
                        String.format(
                            "Policy Action validation failed: " + "the action: [%s] is not contained in the allowed set of actions: [%s]",
                            action,
                            this.validAcsPolicyHttpActions
                        )
                    )
                }
            }
        }
    }

    private fun validateSchema(policySet: PolicySet) {
        try {
            val policySetJsonNode = JSONUTILS.readJsonNodeFromObject(policySet)
            val factory = JsonSchemaFactory.byDefault()
            val schema = factory.getJsonSchema(JSONSCHEMA!!)
            val report = schema.validate(policySetJsonNode)
            val iterator = report.iterator()
            val valid = report.isSuccess
            val sb = StringBuilder()
            if (!valid) {
                while (iterator.hasNext()) {
                    val processingMessage = iterator.next()
                    sb.append(" ")
                    sb.append(processingMessage)
                    LOGGER.debug("{}", processingMessage)
                }
                throw PolicySetValidationException("JSON Schema validation " + sb.toString())
            }
        } catch (e: PolicySetValidationException) {
            throw e
        } catch (e: Exception) {
            throw PolicySetValidationException("Error while validating JSON schema", e)
        }
    }

    override fun validatePolicyConditions(conditions: List<Condition>?): List<ConditionScript> {
        val conditionScripts = ArrayList<ConditionScript>()
        try {
            if (conditions == null || conditions.isEmpty()) {
                return conditionScripts
            }
            for (condition in conditions) {
                val conditionScript = condition.condition
                var compiledScript: ConditionScript? = this.conditionCache[conditionScript!!]
                if (compiledScript != null) {
                    conditionScripts.add(compiledScript)
                    continue
                }

                try {
                    LOGGER.debug("Adding condition: {}", conditionScript)
                    compiledScript = this.conditionShell.parse(conditionScript)
                    conditionScripts.add(compiledScript)
                    this.conditionCache.put(conditionScript, compiledScript)
                } catch (e: Exception) {
                    throw PolicySetValidationException(
                        "Condition : [" + conditionScript + "] validation failed with error : " + e.message, e
                    )
                }
            }

            return conditionScripts
        } catch (e: PolicySetValidationException) {
            throw e
        } catch (e: Exception) {
            throw PolicySetValidationException("Unexpected exception while validating policy conditions", e)
        }
    }

    /**
     * Initialization method to populate the allowedPolicyHttpActions.
     */
    @PostConstruct
    fun init() {
        val actions =
            this.validAcsPolicyHttpActions.split("\\s*,\\s*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        LOGGER.debug("ACS Server configured with validAcsPolicyHttpActions : {}", this.validAcsPolicyHttpActions)
        this.validAcsPolicyHttpActionsSet = HashSet(listOf(*actions))
    }

    fun setValidAcsPolicyHttpActions(validAcsPolicyHttpActions: String) {
        this.validAcsPolicyHttpActions = validAcsPolicyHttpActions
    }
}
