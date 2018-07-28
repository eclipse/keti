/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.service.policy.admin

import org.apache.commons.lang.StringUtils
import org.eclipse.keti.acs.commons.exception.UntrustedIssuerException
import org.eclipse.keti.acs.model.PolicySet
import org.eclipse.keti.acs.policy.evaluation.cache.PolicyEvaluationCache
import org.eclipse.keti.acs.service.policy.admin.dao.PolicySetEntity
import org.eclipse.keti.acs.service.policy.admin.dao.PolicySetRepository
import org.eclipse.keti.acs.service.policy.validation.PolicySetValidationException
import org.eclipse.keti.acs.service.policy.validation.PolicySetValidator
import org.eclipse.keti.acs.utils.JsonUtils
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.eclipse.keti.acs.zone.resolver.ZoneResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import javax.transaction.Transactional

private val LOGGER = LoggerFactory.getLogger(PolicyManagementServiceImpl::class.java)

/**
 * @author acs-engineers@ge.com
 */
@Component
class PolicyManagementServiceImpl : PolicyManagementService {

    @Autowired
    private lateinit var cache: PolicyEvaluationCache
    @Autowired
    private lateinit var policySetRepository: PolicySetRepository
    @Autowired
    private lateinit var zoneResolver: ZoneResolver
    @Autowired
    private lateinit var policySetValidator: PolicySetValidator
    private val jsonUtils = JsonUtils()

    override val allPolicySets: List<PolicySet>
        get() {
            val zone = this.zoneResolver.zoneEntityOrFail
            val result = ArrayList<PolicySet>()
            val policySetEnityList = this.policySetRepository.findByZone(zone)
            for (policySetEntity in policySetEnityList) {
                result.add(this.jsonUtils.deserialize(policySetEntity.policySetJson!!, PolicySet::class.java)!!)
            }
            return result
        }

    override fun upsertPolicySet(policySet: PolicySet) {

        val policySetName = policySet.name

        try {
            val zone = this.zoneResolver.zoneEntityOrFail

            validatePolicySet(zone, policySet)

            val policySetPayload = this.jsonUtils.serialize(policySet)
            upsertPolicySetInTransaction(policySetName!!, zone, policySetPayload)
        } catch (e: Exception) {
            handleException(e, policySetName)
        }
    }

    @Transactional
    internal fun upsertPolicySetInTransaction(
        policySetName: String,
        zone: ZoneEntity,
        policySetPayload: String?
    ) {
        val existingPolicySetEntity = this.policySetRepository.getByZoneAndPolicySetId(zone, policySetName)
        val policySetEntity = PolicySetEntity(zone, policySetName, policySetPayload)

        // If policy Set already exists, set PK of entity for update
        if (null != existingPolicySetEntity) {
            LOGGER.debug(
                "Found an existing policy set policySetName = {}, zone = {}, upserting now .", policySetName,
                zone
            )
            policySetEntity.id = existingPolicySetEntity.id
        } else {
            LOGGER.debug(
                "No existing policy set found for policySetName = {},  zone = {}, inserting now .",
                policySetName, zone
            )
        }

        this.cache.resetForPolicySet(zone.name!!, policySetName)
        this.policySetRepository.save(policySetEntity)
    }

    private fun handleException(
        e: Exception,
        policySetName: String?
    ) {

        val message = String
            .format("Creation of Policy set %s failed with the following error %s", policySetName, e.message)
        LOGGER.error(message, e)

        if (e is UntrustedIssuerException || e is PolicyManagementException) {
            throw e as RuntimeException
        }
        throw PolicyManagementException(message, e)
    }

    override fun getPolicySet(policySetName: String): PolicySet? {
        val zone = this.zoneResolver.zoneEntityOrFail
        val policySetEntity = this.policySetRepository.getByZoneAndPolicySetId(zone, policySetName)
        if (policySetEntity != null) {
            return this.jsonUtils.deserialize(policySetEntity.policySetJson!!, PolicySet::class.java)
        }
        LOGGER.debug("No policy set found for policySetName = {},  zone = {}.", policySetName, zone)
        return null
    }

    override fun deletePolicySet(policySetId: String?) {
        val zone = this.zoneResolver.zoneEntityOrFail
        val policySetEntity = this.policySetRepository.getByZoneAndPolicySetId(zone, policySetId)
        if (policySetEntity != null) {
            LOGGER.info(
                "Found an existing policy set policySetName={}, zone={}, deleting now.", policySetId,
                zone.name
            )

            val policySet = this.jsonUtils.deserialize(policySetEntity.policySetJson!!, PolicySet::class.java)
            if (policySet != null) {
                this.policySetValidator.removeCachedConditions(policySet)
            }

            // Since we only support one policy set and we don't want to load that policy set when checking for a
            // cached invalidation, we use a hard-coded value for the policy set key.
            this.cache.resetForPolicySet(zone.name!!, policySetId!!)
            this.policySetRepository.delete(policySetEntity)
        } else {
            LOGGER.debug(
                "Cound not find an existing policy set " + "policySetName={}, zone={}, Could not delete it.",
                policySetId, zone.name
            )
        }
    }

    private fun validatePolicySet(
        zone: ZoneEntity,
        policySet: PolicySet
    ) {
        val policySetName = policySet.name
        if (StringUtils.isEmpty(policySetName)) {
            throw PolicyManagementException("Failed to add policy set because the policy set name is missing")
        }

        // Validate whether the policy set name is URI acceptable.
        try {
            val policySetNameURI = URI(policySetName)
            LOGGER.debug("Creating policy set '{}'.", policySetNameURI)
        } catch (e1: URISyntaxException) {
            LOGGER.debug(
                "Failed to add policy set for policySetName = {},  " + "zone = {}, inserting now.",
                policySet.name, zone
            )
            throw PolicyManagementException(
                String.format(
                    "Failed to add policy set because the policy set name '%s' is not URI friendly.",
                    policySet.name
                ), e1
            )
        }

        try {
            this.policySetValidator.validatePolicySet(policySet)
        } catch (e: PolicySetValidationException) {
            LOGGER.debug("Policy Validation Failed policySetName = {}, zone = {}.", policySet.name, zone)
            throw PolicyManagementException(e.message!!, e)
        }
    }
}
