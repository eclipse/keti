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

package com.ge.predix.acs.zone.management.dao;

import java.util.HashSet;
import java.util.Set;

import com.ge.predix.acs.rest.Zone;

/**
 *
 * @author 212319607
 */
public class ZoneConverter {

    public ZoneEntity toZoneEntity(final Zone zone) {

        if (zone == null) {
            return null;
        }

        ZoneEntity entity = new ZoneEntity();
        entity.setName(zone.getName());
        entity.setSubdomain(zone.getSubdomain());
        entity.setDescription(zone.getDescription());
        return entity;
    }

    public Zone toZone(final ZoneEntity zoneEntity) {

        if (zoneEntity == null) {
            return null;
        }

        Zone zone = new Zone();
        zone.setName(zoneEntity.getName());
        zone.setSubdomain(zoneEntity.getSubdomain());
        zone.setDescription(zoneEntity.getDescription());
        return zone;
    }

    public Set<Zone> toZone(final Set<ZoneEntity> zoneEntities) {
        Set<Zone> result = new HashSet<>();
        for (ZoneEntity ze : zoneEntities) {
            result.add(toZone(ze));
        }
        return result;
    }

}
