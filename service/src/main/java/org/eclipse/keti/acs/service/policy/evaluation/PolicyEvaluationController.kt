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

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.eclipse.keti.acs.commons.web.AcsApiUriTemplates.POLICY_EVALUATION_URL
import org.eclipse.keti.acs.commons.web.AcsApiUriTemplates.V1
import org.eclipse.keti.acs.commons.web.BaseRestApi
import org.eclipse.keti.acs.commons.web.ResponseEntityBuilder.ok
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

/**
 * Policy evaluator REST controller.
 *
 * @author acs-engineers@ge.com
 */
@RestController
class PolicyEvaluationController : BaseRestApi() {

    @Autowired
    private lateinit var service: PolicyEvaluationService

    @ApiOperation(
        value = "Evaluates all applicable policies and returns decision result",
        tags = ["Policy Evaluation"],
        response = PolicyEvaluationResult::class
    )
    @ApiResponses(
        value = [(ApiResponse(
            code = 200,
            message = "Policy evaluation was successful",
            response = PolicyEvaluationResult::class
        ))]
    )
    @RequestMapping(
        method = [POST],
        value = [(V1 + POLICY_EVALUATION_URL)],
        consumes = [(MediaType.APPLICATION_JSON_VALUE)],
        produces = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    @ResponseBody
    fun evalPolicyV1(@RequestBody request: PolicyEvaluationRequestV1): ResponseEntity<PolicyEvaluationResult> {
        return ok(this.service.evalPolicy(request))
    }
}
