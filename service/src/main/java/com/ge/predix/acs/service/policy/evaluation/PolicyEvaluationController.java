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
package com.ge.predix.acs.service.policy.evaluation;

import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.POLICY_EVALUATION_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.V1;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.ok;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.ge.predix.acs.commons.web.BaseRestApi;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Policy evaluator REST controller.
 *
 * @author 212304931
 */
@RestController
public class PolicyEvaluationController extends BaseRestApi {

    @Autowired
    private PolicyEvaluationService service;

    @ApiOperation(value = "Evaluates all applicable policies and returns decision result",
            tags = { "Policy Evaluation" }, response = PolicyEvaluationResult.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Policy evaluation was successful",
            response = PolicyEvaluationResult.class), })
    @RequestMapping(method = POST, value = V1 + POLICY_EVALUATION_URL, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<PolicyEvaluationResult> evalPolicyV1(@RequestBody final PolicyEvaluationRequestV1 request) {
        return ok(this.service.evalPolicy(request));
    }
}
