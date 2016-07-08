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

package com.ge.predix.acs.zone.management;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.privilege.management.dao.ResourceEntity;
import com.ge.predix.acs.privilege.management.dao.ResourceRepository;
import com.ge.predix.acs.privilege.management.dao.SubjectEntity;
import com.ge.predix.acs.privilege.management.dao.SubjectRepository;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.zone.management.dao.ZoneConverter;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.management.dao.ZoneRepository;

@Component
@SuppressWarnings("nls")
public class ZoneServiceImpl implements ZoneService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZoneServiceImpl.class);

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    @Qualifier("resourceRepository")
    private ResourceRepository resourceRepository;

    @Autowired
    @Qualifier("subjectRepository")
    private SubjectRepository subjectRepository;

    private final ZoneConverter zoneConverter = new ZoneConverter();

    private static final String SUBDOMAIN_REGEX = "(?:[A-Za-z0-9][A-Za-z0-9\\-]{0,61}[A-Za-z0-9]|[A-Za-z0-9])";
    private static final Pattern SUBDOMAIN_PATTERN;

    static {
        SUBDOMAIN_PATTERN = Pattern.compile(SUBDOMAIN_REGEX);
    }

    @Override
    public boolean upsertZone(final Zone zone) {
        LOGGER.debug("upsertZone Request for: " + zone.toString());
        validateAndSanitizeInputOrFail(zone);
        boolean isEntityUpdate = false;
        ZoneEntity zoneEntity = this.zoneRepository.getByName(zone.getName());
        ZoneEntity zoneWithSameSubdomain = this.zoneRepository.getBySubdomain(zone.getSubdomain());
        if (null != zoneWithSameSubdomain && null == zoneEntity) {
            // there is already a zone with proposed subdomain and it is not an
            // update to an existing Zone
            String message = String.format("Subdomain %s for zoneName = %s is already being used.", zone.getSubdomain(),
                    zone.getName());
            throw new ZoneManagementException(message);
        }
        try {

            if (null == zoneEntity) {
                zoneEntity = this.zoneConverter.toZoneEntity(zone);
            } else {
                isEntityUpdate = true;
                LOGGER.debug("Updating existing Zone " + zoneEntity.getName() + " " + zoneEntity.getSubdomain());
                zoneEntity.setName(zone.getName());
                zoneEntity.setDescription(zone.getDescription());
                zoneEntity.setSubdomain(zone.getSubdomain());
            }
            this.zoneRepository.save(zoneEntity);
            return isEntityUpdate;

        } catch (Exception e) {
            String message = String.format("Unable to persist Zone identified by zoneName = %s", zone.getName());
            throw new ZoneManagementException(message, e);
        }
    }

    @Override
    public Zone retrieveZone(final String zoneName) {

        ZoneEntity currentZone = this.zoneRepository.getByName(zoneName);
        if (currentZone == null) {
            String message = String.format("No Zone identified by zoneName = %s", zoneName);
            throw new ZoneManagementException(message);
        }
        return this.zoneConverter.toZone(currentZone);
    }

    private void validateAndSanitizeInputOrFail(final Zone zone) {
        this.validateSubdomainNames(zone.getSubdomain());
        if (StringUtils.isEmpty(zone.getName())) {
            throw new ZoneManagementException("Empty or Null Zone Name: The Zone Name is mandatory.");
        }
        if (StringUtils.isEmpty(zone.getSubdomain())) {
            throw new ZoneManagementException("Empty or Null Zone Subdomain: The Zone Subdomain is mandatory.");
        }
        if (StringUtils.isEmpty(zone.getDescription())) {
            zone.setDescription(zone.getName() + " descrption");
        }
    }

    private void validateSubdomainNames(final String subDomain) {
        if (!SUBDOMAIN_PATTERN.matcher(subDomain).matches()) {
            throw new ZoneManagementException("Invalid Zone Subdomain: " + subDomain);
        }
    }

    @Override
    public Boolean deleteZone(final String zoneName) {
        ZoneEntity currentZone = this.zoneRepository.getByName(zoneName);
        if (currentZone != null) {
            // Delete child entities in Graph repos first. This is not transactional.
            List<ResourceEntity> resourcesInZone = this.resourceRepository.findByZone(currentZone);
            this.resourceRepository.delete(resourcesInZone);
            List<SubjectEntity> subjectsInZone = this.subjectRepository.findByZone(currentZone);
            this.subjectRepository.delete(subjectsInZone);

            this.zoneRepository.delete(currentZone);
        }
        return currentZone != null;
    }

}
