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

package com.ge.predix.acs.service.policy.admin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.commons.exception.UntrustedIssuerException;
import com.ge.predix.acs.model.PolicySet;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationCache;
import com.ge.predix.acs.service.policy.admin.dao.PolicySetEntity;
import com.ge.predix.acs.service.policy.admin.dao.PolicySetRepository;
import com.ge.predix.acs.service.policy.validation.PolicySetValidationException;
import com.ge.predix.acs.service.policy.validation.PolicySetValidator;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.resolver.ZoneResolver;

/**
 * @author acs-engineers@ge.com
 */
@Component
@SuppressWarnings("nls")
public class PolicyManagementServiceImpl implements PolicyManagementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyManagementServiceImpl.class);

    @Autowired
    private PolicyEvaluationCache cache;
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
            LOGGER.debug("Found an existing policy set policySetName = {}, zone = {}, upserting now .", policySetName,
                    zone);
            policySetEntity.setId(existingPolicySetEntity.getId());
        } else {
            LOGGER.debug("No existing policy set found for policySetName = {},  zone = {}, inserting now .",
                    policySetName, zone);
        }

        this.cache.resetForPolicySet(zone.getName(), policySetName);
        this.policySetRepository.save(policySetEntity);
    }

    private void handleException(final Exception e, final String policySetName) {

        String message = String
                .format("Creation of Policy set %s failed with the following error %s", policySetName, e.getMessage());
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
        LOGGER.debug("No policy set found for policySetName = {},  zone = {}.", policySetName, zone);
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
    public void deletePolicySet(final String policySetId) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        PolicySetEntity policySetEntity = this.policySetRepository.getByZoneAndPolicySetId(zone, policySetId);
        if (policySetEntity != null) {
            LOGGER.info("Found an existing policy set policySetName={}, zone={}, deleting now.", policySetId,
                    zone.getName());

            PolicySet policySet = this.jsonUtils.deserialize(policySetEntity.getPolicySetJson(), PolicySet.class);
            if (policySet != null) {
                this.policySetValidator.removeCachedConditions(policySet);
            }

            // Since we only support one policy set and we don't want to load that policy set when checking for a
            // cached invalidation, we use a hard-coded value for the policy set key.
            this.cache.resetForPolicySet(zone.getName(), policySetId);
            this.policySetRepository.delete(policySetEntity);
        } else {
            LOGGER.debug("Cound not find an existing policy set " + "policySetName={}, zone={}, Could not delete it.",
                    policySetId, zone.getName());
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
            LOGGER.debug("Creating policy set '{}'.", policySetNameURI);
        } catch (URISyntaxException e1) {
            LOGGER.debug("Failed to add policy set for policySetName = {},  " + "zone = {}, inserting now.",
                    policySet.getName(), zone);
            throw new PolicyManagementException(
                    String.format("Failed to add policy set because the policy set name '%s' is not URI friendly.",
                            policySet.getName()), e1);
        }

        try {
            this.policySetValidator.validatePolicySet(policySet);
        } catch (PolicySetValidationException e) {
            LOGGER.debug("Policy Validation Failed policySetName = {}, zone = {}.", policySet.getName(), zone);
            throw new PolicyManagementException(e.getMessage(), e);
        }

    }
}
