/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.service.policy.admin

import org.eclipse.keti.acs.model.PolicySet

/**
 *
 * @author acs-engineers@ge.com
 */
interface PolicyManagementService {

    /**
     * Get all policy sets.
     *
     * @return the list of policySets
     */
    val allPolicySets: List<PolicySet>

    /**
     * @throws IllegalArgumentException
     * If policy format is not valid or PolicySet.name is not set.
     */
    @Throws(IllegalArgumentException::class)
    fun upsertPolicySet(policySet: PolicySet)

    /**
     * @return PolicySet for given name, null if no such set exists.
     */
    fun getPolicySet(policySetName: String): PolicySet?

    /**
     * Deletes a policy set.
     *
     * @param policySetId
     * policy set ID
     */
    fun deletePolicySet(policySetId: String?)
}
