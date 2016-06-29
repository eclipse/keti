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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ge.predix.acs.commons.exception.UntrustedIssuerException;
import com.ge.predix.acs.model.PolicySet;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationCacheCircuitBreaker;
import com.ge.predix.acs.service.policy.admin.dao.PolicySetEntity;
import com.ge.predix.acs.service.policy.admin.dao.PolicySetRepository;
import com.ge.predix.acs.service.policy.validation.PolicySetValidationException;
import com.ge.predix.acs.service.policy.validation.PolicySetValidator;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.resolver.ZoneResolver;

/**
 * @author 212319607
 */
@Component
@SuppressWarnings("nls")
public class PolicyManagementServiceImpl implements PolicyManagementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyManagementServiceImpl.class);

    @Autowired
    private PolicyEvaluationCacheCircuitBreaker cache;
    @Autowired
    private PolicySetRepository policySetRepository;
    @Autowired
    private ZoneResolver zoneResolver;
    @Autowired
    private PolicySetValidator policySetValidator;
    private final JsonUtils jsonUtils = new JsonUtils();

    @Override
    public void upsertPolicySet(final PolicySet policySet) {

        String policySetName = policySet.getName();

        try {
            ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();

            validatePolicySet(zone, policySet);

            String policySetPayload = this.jsonUtils.serialize(policySet);
            upsertPolicySetInTransaction(policySetName, zone, policySetPayload);
        } catch (Exception e) {
            handleException(e, policySetName);
        }
    }

    @Transactional
    private void upsertPolicySetInTransaction(final String policySetName, final ZoneEntity zone,
            final String policySetPayload) {
        PolicySetEntity existingPolicySetEntity = this.policySetRepository.getByZoneAndPolicySetId(zone, policySetName);
        PolicySetEntity policySetEntity = new PolicySetEntity(zone, policySetName, policySetPayload);

        // If policy Set already exists, set PK of entity for update
        if (null != existingPolicySetEntity) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format(
                        "Found an existing policy set policySetName = %s, zone = %s," + " upserting now .",
                        policySetName, zone.toString()));
            }
            policySetEntity.setId(existingPolicySetEntity.getId());
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format(
                        "No existing policy set found for policySetName = %s,  zone = %s," + " inserting now .",
                        policySetName, zone.toString()));
            }
        }
        // Since we only support one policy set and we don't want to load that policy set when checking for a
        // cached invalidation, we use a hard-coded value for the policy set key.
        this.cache.resetForPolicySet(zone.getName(), "default");
        this.policySetRepository.save(policySetEntity);
    }

    private void handleException(final Exception e, final String policySetName) {

        String message = String.format("Creation of Policy set %s failed with the following error %s", policySetName,
                e.getMessage());

        LOGGER.error(message, e);

        if (e instanceof UntrustedIssuerException || e instanceof PolicyManagementException) {
            throw (RuntimeException) e;
        }
        throw new PolicyManagementException(message, e);
    }

    @Override
    public PolicySet getPolicySet(final String policySetName) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        PolicySetEntity policySetEntity = this.policySetRepository.getByZoneAndPolicySetId(zone, policySetName);
        if (policySetEntity != null) {
            return this.jsonUtils.deserialize(policySetEntity.getPolicySetJson(), PolicySet.class);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("No policy set found for policySetName = %s,  zone = %s.", policySetName,
                    zone.toString()));
        }

        return null;
    }

    @Override
    public List<PolicySet> getAllPolicySets() {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        ArrayList<PolicySet> result = new ArrayList<>();
        List<PolicySetEntity> policySetEnityList = this.policySetRepository.findByZone(zone);
        for (PolicySetEntity policySetEntity : policySetEnityList) {
            result.add(this.jsonUtils.deserialize(policySetEntity.getPolicySetJson(), PolicySet.class));
        }
        return result;
    }

    @Override
    public void deletePolicySet(final String policySetID) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        PolicySetEntity policySetEntity = this.policySetRepository.getByZoneAndPolicySetId(zone, policySetID);
        if (policySetEntity != null) {
            LOGGER.info(String.format("Found an existing policy set policySetName=%s, zone=%s, deleting now.",
                    policySetID, zone.getName()));
            // Since we only support one policy set and we don't want to load that policy set when checking for a
            // cached invalidation, we use a hard-coded value for the policy set key.
            this.cache.resetForPolicySet(zone.getName(), "default");
            this.policySetRepository.delete(policySetEntity);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format(
                        "Cound not find an existing policy set " + "policySetName=%s, zone=%s, Could not delete it.",
                        policySetID, zone.getName()));
            }
        }
    }

    private void validatePolicySet(final ZoneEntity zone, final PolicySet policySet) {
        String policySetName = policySet.getName();
        if (StringUtils.isEmpty(policySetName)) {
            throw new PolicyManagementException("Failed to add policy set because the policy set name is missing");
        }

        // Validate whether the policy set name is URI acceptable.
        try {
            URI policySetNameURI = new URI(policySetName);
            LOGGER.debug(String.format("Creating policy set '%s'.", policySetNameURI));
        } catch (URISyntaxException e1) {
            LOGGER.debug(
                    String.format("Failed to add policy set for policySetName = %s,  " + "zone = %s, inserting now.",
                            policySet.getName(), zone.toString()));
            throw new PolicyManagementException(
                    String.format("Failed to add policy set because the policy set name '%s' is not URI friendly.",
                            policySet.getName()),
                    e1);
        }

        if ((null == policySet) || StringUtils.isEmpty(policySet.getName())) {
            throw new IllegalArgumentException("PolicySet must have id and name");
        }
        try {
            this.policySetValidator.validatePolicySet(policySet);
        } catch (PolicySetValidationException e) {
            LOGGER.debug(String.format("Policy Validation Failed policySetName = %s,  " + "zone = %s .",
                    policySet.getName(), zone.toString()));
            throw new PolicyManagementException(e.getMessage(), e);
        }

        // Ensure there is only 1 policy-set in the repository
        List<PolicySetEntity> policySets = this.policySetRepository.findByZone(zone);
        if (policySets.isEmpty()) {
            return;
        }
        String currentPolicySetName = policySets.get(0).getPolicySetID();
        if (!currentPolicySetName.equals((policySet.getName()))) {
            String errMsg = String.format(
                    "Multiple policy sets file is not allowed. "
                            + "Existing PolicySetName: %s, New PolicySetName: %s, zone: %s .",
                    currentPolicySetName, policySet.getName(), zone.toString());
            throw new PolicyManagementException(errMsg);
        }
    }
}
