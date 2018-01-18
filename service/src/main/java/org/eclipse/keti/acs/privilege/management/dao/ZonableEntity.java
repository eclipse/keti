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

package org.eclipse.keti.acs.privilege.management.dao;

import org.eclipse.keti.acs.model.Attribute;
import org.eclipse.keti.acs.rest.Parent;
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity;

import java.util.Set;

public interface ZonableEntity {
    Long getId();

    String getEntityId();

    String getEntityType();

    void setId(Long id);

    ZoneEntity getZone();

    void setZone(ZoneEntity zone);

    Set<Attribute> getAttributes();

    void setAttributes(Set<Attribute> attributes);

    String getAttributesAsJson();

    void setAttributesAsJson(String attributesAsJson);

    Set<Parent> getParents();

    void setParents(Set<Parent> parentIdentifiers);
}
