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

package com.ge.predix.acs.service.policy.validation;

import java.util.List;

import com.ge.predix.acs.commons.policy.condition.ConditionScript;
import com.ge.predix.acs.model.Condition;
import com.ge.predix.acs.model.PolicySet;

/**
 *
 * @author acs-engineers@ge.com
 */
public interface PolicySetValidator {

    void removeCachedConditions(PolicySet policySet);

    /**
     * @param policySet
     *            PolicySet to be validated
     */
    void validatePolicySet(PolicySet policySet);

    /**
     * @param conditions
     *            Policy conditions to be validated
     * @return A list of the corresponding ConditionScript for validated conditions
     */
    List<ConditionScript> validatePolicyConditions(List<Condition> conditions);

}
