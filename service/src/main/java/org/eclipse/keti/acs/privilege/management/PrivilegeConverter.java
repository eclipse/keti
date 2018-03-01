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

package org.eclipse.keti.acs.privilege.management;

import java.util.Set;

import org.eclipse.keti.acs.model.Attribute;
import org.eclipse.keti.acs.privilege.management.dao.ResourceEntity;
import org.eclipse.keti.acs.privilege.management.dao.SubjectEntity;
import org.eclipse.keti.acs.rest.BaseResource;
import org.eclipse.keti.acs.rest.BaseSubject;
import org.eclipse.keti.acs.utils.JsonUtils;
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity;

/**
 *
 * @author acs-engineers@ge.com
 */
public class PrivilegeConverter {
    private final JsonUtils jsonUtils = new JsonUtils();

    public ResourceEntity toResourceEntity(final ZoneEntity zone, final BaseResource resource) {
        if (resource == null) {
            return null;
        }

        ResourceEntity resourceEntity = new ResourceEntity(zone, resource.getResourceIdentifier());
        resourceEntity.setAttributes(resource.getAttributes());
        String attributesAsJson = this.jsonUtils.serialize(resource.getAttributes());
        resourceEntity.setAttributesAsJson(attributesAsJson);
        resourceEntity.setParents(resource.getParents());
        return resourceEntity;
    }

    @SuppressWarnings("unchecked")
    public BaseResource toResource(final ResourceEntity resourceEntity) {
        if (resourceEntity == null) {
            return null;
        }

        BaseResource resource = new BaseResource(resourceEntity.getResourceIdentifier());

        Set<Attribute> deserialize = this.jsonUtils.deserialize(resourceEntity.getAttributesAsJson(), Set.class,
                Attribute.class);
        resource.setAttributes(deserialize);
        resource.setParents(resourceEntity.getParents());
        return resource;
    }

    public SubjectEntity toSubjectEntity(final ZoneEntity zone, final BaseSubject subject) {
        if (subject == null) {
            return null;
        }

        SubjectEntity subjectEntity = new SubjectEntity(zone, subject.getSubjectIdentifier());
        subjectEntity.setAttributes(subject.getAttributes());
        String attributesAsJson = this.jsonUtils.serialize(subject.getAttributes());
        subjectEntity.setAttributesAsJson(attributesAsJson);
        subjectEntity.setParents(subject.getParents());
        return subjectEntity;
    }

    @SuppressWarnings("unchecked")
    public BaseSubject toSubject(final SubjectEntity subjectEntity) {
        if (subjectEntity == null) {
            return null;
        }

        BaseSubject subject = new BaseSubject(subjectEntity.getSubjectIdentifier());

        Set<Attribute> deserialize = this.jsonUtils.deserialize(subjectEntity.getAttributesAsJson(), Set.class,
                Attribute.class);
        subject.setAttributes(deserialize);
        subject.setParents(subjectEntity.getParents());
        return subject;
    }
}
