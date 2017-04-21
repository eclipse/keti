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

import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.POLICY_SETS_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.POLICY_SET_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.V1;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.created;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.noContent;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.notFound;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.ok;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.net.URI;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.ge.predix.acs.commons.web.BaseRestApi;
import com.ge.predix.acs.commons.web.RestApiException;
import com.ge.predix.acs.commons.web.UriTemplateUtils;
import com.ge.predix.acs.model.PolicySet;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author 212314537
 */
@RestController
@RequestMapping(value = { V1 })
public class PolicyManagementController extends BaseRestApi {

    @Autowired
    private PolicyManagementService service;

    @ApiOperation(
            value = "Creates/Updates a policy set for the given zone.",
            tags = { "Policy Set Management" })
    @ApiResponses(
            value = { @ApiResponse(
                    code = 201,
                    message = "Policy set creation successful. Policy set URI is returned in 'Location' header."), })
    @RequestMapping(method = PUT, value = POLICY_SET_URL, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createPolicySet(@RequestBody final PolicySet policySet,
            @PathVariable("policySetId") final String policySetId) {

        validatePolicyIdOrFail(policySet, policySetId);

        try {
            this.service.upsertPolicySet(policySet);
            URI policySetUri = UriTemplateUtils.expand(POLICY_SET_URL, "policySetId:" + policySet.getName());
            return created(policySetUri.getPath());
        } catch (PolicyManagementException e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
        }
    }

    @ApiOperation(value = "Retrieves a policy set for the given zone.", tags = { "Policy Set Management" })
    @RequestMapping(method = GET, value = POLICY_SET_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<PolicySet> getPolicySet(@PathVariable("policySetId") final String name) {
        PolicySet result = this.service.getPolicySet(name);

        if (null != result) {
            return ok(result);
        }

        return notFound();
    }

    @ApiOperation(value = "Deletes a policy set for the given zone.", tags = { "Policy Set Management" })
    @RequestMapping(method = DELETE, value = POLICY_SET_URL)
    public ResponseEntity<Void> deletePolicySet(@PathVariable("policySetId") final String name) {
        this.service.deletePolicySet(name);
        return noContent();
    }

    @ApiOperation(value = "Returns all the policy sets for the given zone.", tags = { "Policy Set Management" })
    @RequestMapping(method = GET, value = POLICY_SETS_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<PolicySet>> getAllPolicySets() {
        List<PolicySet> allPolicySets = this.service.getAllPolicySets();
        return ok(allPolicySets);
    }

    /**
     * @param name
     * @param policySetId
     */
    private void validatePolicyIdOrFail(final PolicySet policySet, final String policySetId) {
        if (policySet == null) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Policy Set cannot be empty or null");
        }

        String name = policySet.getName();
        if (!StringUtils.isEmpty(name) && !policySetId.equals(name)) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("Policy Set name in the payload = %s, does not match the one provided in URI = %s",
                            name, policySetId));
        }
    }
}
