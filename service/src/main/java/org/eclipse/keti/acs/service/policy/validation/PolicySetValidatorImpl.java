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
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package org.eclipse.keti.acs.service.policy.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.keti.acs.commons.policy.condition.ConditionScript;
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionCache;
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionShell;
import org.eclipse.keti.acs.model.Condition;
import org.eclipse.keti.acs.model.Policy;
import org.eclipse.keti.acs.model.PolicySet;
import org.eclipse.keti.acs.utils.JsonUtils;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

/**
 * @author acs-engineers@ge.com
 */
@Component
@SuppressWarnings("nls")
public class PolicySetValidatorImpl implements PolicySetValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicySetValidatorImpl.class);

    private static final JsonUtils JSONUTILS = new JsonUtils();
    private static final JsonNode JSONSCHEMA;

    @Autowired
    private GroovyConditionCache conditionCache;

    @Autowired
    private GroovyConditionShell conditionShell;

    static {
        JSONSCHEMA = JSONUTILS.readJsonNodeFromFile("acs-policy-set-schema.json");
    }

    @Value("${validAcsPolicyHttpActions:GET, POST, PUT, DELETE, PATCH, SUBSCRIBE, MESSAGE}")
    private String validAcsPolicyHttpActions;

    private Set<String> validAcsPolicyHttpActionsSet;

    @Override
    public void removeCachedConditions(final PolicySet policySet) {
        for (Policy policy : policySet.getPolicies()) {
            for (Condition condition : policy.getConditions()) {
                this.conditionCache.remove(condition.getCondition());
            }
        }
    }

    @Override
    public void validatePolicySet(final PolicySet policySet) {
        validateSchema(policySet);
        for (Policy p : policySet.getPolicies()) {
            validatePolicyConditions(p.getConditions());
            validatePolicyActions(p);
        }
    }

    private void validatePolicyActions(final Policy p) {

        if (p.getTarget() != null && p.getTarget().getAction() != null) {
            String policyActions = p.getTarget().getAction();
            // Empty actions will be treated as null actions which behave like
            // match any
            // during policy evaluation
            if (policyActions.trim().length() == 0) {
                p.getTarget().setAction(null);
                return;
            }
            for (String action : policyActions.split("\\s*,\\s*")) {
                if (!this.validAcsPolicyHttpActionsSet.contains(action)) {
                    throw new PolicySetValidationException(String.format("Policy Action validation failed: "
                                    + "the action: [%s] is not contained in the allowed set of actions: [%s]", action,
                            this.validAcsPolicyHttpActions));
                }
            }
        }
    }

    private void validateSchema(final PolicySet policySet) {
        try {
            JsonNode policySetJsonNode = JSONUTILS.readJsonNodeFromObject(policySet);
            JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            JsonSchema schema = factory.getJsonSchema(JSONSCHEMA);
            ProcessingReport report = schema.validate(policySetJsonNode);
            Iterator<ProcessingMessage> iterator = report.iterator();
            boolean valid = report.isSuccess();
            StringBuilder sb = new StringBuilder();
            if (!valid) {
                while (iterator.hasNext()) {
                    ProcessingMessage processingMessage = iterator.next();
                    sb.append(" ");
                    sb.append(processingMessage);
                    LOGGER.debug("{}", processingMessage);
                }
                throw new PolicySetValidationException("JSON Schema validation " + sb.toString());
            }
        } catch (PolicySetValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new PolicySetValidationException("Error while validating JSON schema", e);
        }

    }

    @Override
    public List<ConditionScript> validatePolicyConditions(final List<Condition> conditions) {
        List<ConditionScript> conditionScripts = new ArrayList<>();
        try {
            if ((conditions == null) || conditions.isEmpty()) {
                return conditionScripts;
            }
            for (Condition condition : conditions) {
                String conditionScript = condition.getCondition();
                ConditionScript compiledScript = this.conditionCache.get(conditionScript);
                if (compiledScript != null) {
                    conditionScripts.add(compiledScript);
                    continue;
                }

                try {
                    LOGGER.debug("Adding condition: {}", conditionScript);
                    compiledScript = this.conditionShell.parse(conditionScript);
                    conditionScripts.add(compiledScript);
                    this.conditionCache.put(conditionScript, compiledScript);
                } catch (Exception e) {
                    throw new PolicySetValidationException(
                            "Condition : [" + conditionScript + "] validation failed with error : " + e.getMessage(),
                            e);
                }
            }

            return conditionScripts;

        } catch (PolicySetValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new PolicySetValidationException("Unexpected exception while validating policy conditions", e);
        }
    }

    /**
     * Initialization method to populate the allowedPolicyHttpActions.
     */
    @PostConstruct
    public void init() {
        String[] actions = this.validAcsPolicyHttpActions.split("\\s*,\\s*");
        LOGGER.debug("ACS Server configured with validAcsPolicyHttpActions : {}", this.validAcsPolicyHttpActions);
        this.validAcsPolicyHttpActionsSet = new HashSet<>(Arrays.asList(actions));
    }

    public void setValidAcsPolicyHttpActions(final String validAcsPolicyHttpActions) {
        this.validAcsPolicyHttpActions = validAcsPolicyHttpActions;
    }
}
