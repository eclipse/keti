/*******************************************************************************
 * Copyright 2017 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.privilege.management

import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.privilege.management.dao.ResourceEntity
import org.eclipse.keti.acs.privilege.management.dao.SubjectEntity
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.utils.JsonUtils
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity

/**
 * @author acs-engineers@ge.com
 */
class PrivilegeConverter {

    private val jsonUtils = JsonUtils()

    fun toResourceEntity(
        zone: ZoneEntity,
        resource: BaseResource?
    ): ResourceEntity? {
        if (resource == null) {
            return null
        }

        val resourceEntity = ResourceEntity(zone, resource.resourceIdentifier!!)
        resourceEntity.attributes = resource.attributes
        val attributesAsJson = this.jsonUtils.serialize(resource.attributes)
        resourceEntity.attributesAsJson = attributesAsJson
        resourceEntity.parents = resource.parents
        return resourceEntity
    }

    fun toResource(resourceEntity: ResourceEntity?): BaseResource? {
        if (resourceEntity == null) {
            return null
        }

        val resource = BaseResource(resourceEntity.resourceIdentifier!!)

        val deserialize = this.jsonUtils.deserialize(
            resourceEntity.attributesAsJson!!, Set::class.java as Class<Set<Attribute>>, Attribute::class.java
        )
        resource.attributes = deserialize
        resource.parents = resourceEntity.parents
        return resource
    }

    fun toSubjectEntity(
        zone: ZoneEntity,
        subject: BaseSubject?
    ): SubjectEntity? {
        if (subject == null) {
            return null
        }

        val subjectEntity = SubjectEntity(zone, subject.subjectIdentifier!!)
        subjectEntity.attributes = subject.attributes
        val attributesAsJson = this.jsonUtils.serialize(subject.attributes)
        subjectEntity.attributesAsJson = attributesAsJson
        subjectEntity.parents = subject.parents
        return subjectEntity
    }

    fun toSubject(subjectEntity: SubjectEntity?): BaseSubject? {
        if (subjectEntity == null) {
            return null
        }

        val subject = BaseSubject(subjectEntity.subjectIdentifier!!)

        val deserialize = this.jsonUtils.deserialize(
            subjectEntity.attributesAsJson!!, Set::class.java as Class<Set<Attribute>>, Attribute::class.java
        )
        subject.attributes = deserialize
        subject.parents = subjectEntity.parents
        return subject
    }
}
