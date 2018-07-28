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

package org.eclipse.keti.acs.zone.management

import org.apache.commons.lang.StringUtils
import org.eclipse.keti.acs.privilege.management.dao.ResourceRepository
import org.eclipse.keti.acs.privilege.management.dao.SubjectRepository
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.zone.management.dao.ZoneConverter
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.eclipse.keti.acs.zone.management.dao.ZoneRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.regex.Pattern

private val LOGGER = LoggerFactory.getLogger(ZoneServiceImpl::class.java)

private const val SUBDOMAIN_REGEX = "(?:[A-Za-z0-9][A-Za-z0-9\\-]{0,61}[A-Za-z0-9]|[A-Za-z0-9])"
private val SUBDOMAIN_PATTERN = Pattern.compile(SUBDOMAIN_REGEX)

@Component
class ZoneServiceImpl : ZoneService {

    @Autowired
    private lateinit var zoneRepository: ZoneRepository

    @Autowired
    @Qualifier("resourceRepository")
    private lateinit var resourceRepository: ResourceRepository

    @Autowired
    @Qualifier("subjectRepository")
    private lateinit var subjectRepository: SubjectRepository

    private val zoneConverter = ZoneConverter()

    override fun upsertZone(zone: Zone): Boolean {
        LOGGER.debug("upsertZone Request for: {}", zone)
        validateAndSanitizeInputOrFail(zone)
        var isEntityUpdate = false
        var zoneEntity = this.zoneRepository.getByName(zone.name!!)
        val zoneWithSameSubdomain = this.zoneRepository.getBySubdomain(zone.subdomain)
        if (null != zoneWithSameSubdomain && null == zoneEntity) {
            // there is already a zone with proposed subdomain and it is not an
            // update to an existing Zone
            val message = String.format(
                "Subdomain %s for zoneName = %s is already being used.", zone.subdomain,
                zone.name
            )
            throw ZoneManagementException(message)
        }
        try {

            if (null == zoneEntity) {
                zoneEntity = this.zoneConverter.toZoneEntity(zone)
            } else {
                isEntityUpdate = true
                LOGGER.debug("Updating existing Zone {} {}", zoneEntity.name, zoneEntity.subdomain)
                zoneEntity.name = zone.name
                zoneEntity.description = zone.description
                zoneEntity.subdomain = zone.subdomain
            }
            this.zoneRepository.save<ZoneEntity>(zoneEntity)
            return isEntityUpdate
        } catch (e: Exception) {
            val message = String.format("Unable to persist Zone identified by zoneName = %s", zone.name)
            throw ZoneManagementException(message, e)
        }
    }

    override fun retrieveZone(zoneName: String): Zone? {

        val currentZone = this.zoneRepository.getByName(zoneName)
        if (currentZone == null) {
            val message = String.format("No Zone identified by zoneName = %s", zoneName)
            throw ZoneManagementException(message)
        }
        return this.zoneConverter.toZone(currentZone)
    }

    private fun validateAndSanitizeInputOrFail(zone: Zone) {
        this.validateSubdomainNames(zone.subdomain!!)
        if (StringUtils.isEmpty(zone.name)) {
            throw ZoneManagementException("Empty or Null Zone Name: The Zone Name is mandatory.")
        }
        if (StringUtils.isEmpty(zone.subdomain)) {
            throw ZoneManagementException("Empty or Null Zone Subdomain: The Zone Subdomain is mandatory.")
        }
        if (StringUtils.isEmpty(zone.description)) {
            zone.description = zone.name + " descrption"
        }
    }

    private fun validateSubdomainNames(subDomain: String) {
        if (!SUBDOMAIN_PATTERN.matcher(subDomain).matches()) {
            throw ZoneManagementException("Invalid Zone Subdomain: $subDomain")
        }
    }

    override fun deleteZone(zoneName: String): Boolean? {
        val currentZone = this.zoneRepository.getByName(zoneName)
        if (currentZone != null) {
            // Delete child entities in Graph repos first. This is not transactional.
            val resourcesInZone = this.resourceRepository.findByZone(currentZone)
            this.resourceRepository.delete(resourcesInZone)
            val subjectsInZone = this.subjectRepository.findByZone(currentZone)
            this.subjectRepository.delete(subjectsInZone)

            this.zoneRepository.delete(currentZone)
        }
        return currentZone != null
    }
}
