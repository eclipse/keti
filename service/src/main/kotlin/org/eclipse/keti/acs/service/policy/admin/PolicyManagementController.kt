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

package org.eclipse.keti.acs.service.policy.admin

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.apache.commons.lang.StringUtils
import org.eclipse.keti.acs.commons.web.BaseRestApi
import org.eclipse.keti.acs.commons.web.POLICY_SETS_URL
import org.eclipse.keti.acs.commons.web.POLICY_SET_URL
import org.eclipse.keti.acs.commons.web.RestApiException
import org.eclipse.keti.acs.commons.web.V1
import org.eclipse.keti.acs.commons.web.created
import org.eclipse.keti.acs.commons.web.expand
import org.eclipse.keti.acs.commons.web.noContent
import org.eclipse.keti.acs.commons.web.notFound
import org.eclipse.keti.acs.commons.web.ok
import org.eclipse.keti.acs.model.PolicySet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.DELETE
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.PUT
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

/**
 * @author acs-engineers@ge.com
 */
@RestController
@RequestMapping(value = [V1])
class PolicyManagementController : BaseRestApi() {

    @Autowired
    private lateinit var service: PolicyManagementService

    @ApiOperation(
        value = "Returns all the policy sets for the given zone.",
        tags = ["Policy Set Management"]
    )
    @RequestMapping(
        method = [GET],
        value = [POLICY_SETS_URL],
        produces = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    @ResponseBody
    fun getAllPolicySets(): ResponseEntity<List<PolicySet>> {
        val allPolicySets = this.service.allPolicySets
        return ok(allPolicySets)
    }

    @ApiOperation(value = "Creates/Updates a policy set for the given zone.", tags = ["Policy Set Management"])
    @ApiResponses(
        value = [(ApiResponse(
            code = 201,
            message = "Policy set creation successful. Policy set URI is returned in 'Location' header."
        ))]
    )
    @RequestMapping(method = [PUT], value = [POLICY_SET_URL], consumes = [(MediaType.APPLICATION_JSON_VALUE)])
    fun createPolicySet(
        @RequestBody policySet: PolicySet,
        @PathVariable("policySetId") policySetId: String
    ): ResponseEntity<String> {

        validatePolicyIdOrFail(policySet, policySetId)

        try {
            this.service.upsertPolicySet(policySet)
            val policySetUri = expand(POLICY_SET_URL, "policySetId:" + policySet.name)
            return created(policySetUri.path)
        } catch (e: PolicyManagementException) {
            throw RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e.message!!, e)
        }
    }

    @ApiOperation(value = "Retrieves a policy set for the given zone.", tags = ["Policy Set Management"])
    @RequestMapping(method = [GET], value = [POLICY_SET_URL])
    @ResponseBody
    fun getPolicySet(@PathVariable("policySetId") name: String): ResponseEntity<PolicySet> {
        val result = this.service.getPolicySet(name)

        return if (null != result) {
            ok(result)
        } else notFound()
    }

    @ApiOperation(value = "Deletes a policy set for the given zone.", tags = ["Policy Set Management"])
    @RequestMapping(method = [DELETE], value = [POLICY_SET_URL])
    fun deletePolicySet(@PathVariable("policySetId") name: String): ResponseEntity<Void> {
        this.service.deletePolicySet(name)
        return noContent()
    }

    /**
     * @param policySet
     * @param policySetId
     */
    private fun validatePolicyIdOrFail(
        policySet: PolicySet?,
        policySetId: String
    ) {
        if (policySet == null) {
            throw RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Policy Set cannot be empty or null")
        }

        val name = policySet.name
        if (!StringUtils.isEmpty(name) && policySetId != name) {
            throw RestApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                String.format(
                    "Policy Set name in the payload = %s, does not match the one provided in URI = %s",
                    name, policySetId
                )
            )
        }
    }
}
