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

package com.ge.predix.acs.service.policy.admin;

import com.ge.predix.acs.model.PolicySet;

import java.util.List;

/**
 *
 * @author 212314537
 */
public interface PolicyManagementService {

    /**
     * @throws IllegalArgumentException
     *             If policy format is not valid or PolicySet.name is not set.
     */
    void upsertPolicySet(PolicySet policySet) throws IllegalArgumentException;

    /**
     * @return PolicySet for given name, null if no such set exists.
     */
    PolicySet getPolicySet(String policySetName);

    /**
     * Deletes a policy set.
     *
     * @param policySetID
     *            policy set ID
     */
    void deletePolicySet(String policySetID);

    /**
     * Get all policy sets.
     *
     * @return the list of policySets
     */
    List<PolicySet> getAllPolicySets();
}
