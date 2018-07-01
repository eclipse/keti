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

import org.apache.commons.collections.CollectionUtils
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.policy.evaluation.cache.PolicyEvaluationCache
import org.eclipse.keti.acs.privilege.management.dao.ResourceEntity
import org.eclipse.keti.acs.privilege.management.dao.ResourceRepositoryProxy
import org.eclipse.keti.acs.privilege.management.dao.SubjectEntity
import org.eclipse.keti.acs.privilege.management.dao.SubjectRepositoryProxy
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.eclipse.keti.acs.zone.resolver.ZoneResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.ArrayList

private val LOGGER = LoggerFactory.getLogger(PrivilegeManagementServiceImpl::class.java)

/**
 * The implementation of privilege management.
 *
 * @author acs-engineers@ge.com
 */
@Component
class PrivilegeManagementServiceImpl : PrivilegeManagementService {

    @Autowired
    private lateinit var cache: PolicyEvaluationCache

    @Autowired
    private lateinit var subjectRepository: SubjectRepositoryProxy

    @Autowired
    private lateinit var resourceRepository: ResourceRepositoryProxy

    @Autowired
    private lateinit var zoneResolver: ZoneResolver

    private val privilegeConverter = PrivilegeConverter()

    override val resources: List<BaseResource>
        @Transactional(readOnly = true)
        get() {
            val zone = this.zoneResolver.zoneEntityOrFail

            val resources = ArrayList<BaseResource>()
            val resourceEntities = this.resourceRepository.findByZone(zone)

            if (!CollectionUtils.isEmpty(resourceEntities)) {
                for (resourceEntity in resourceEntities) {
                    resources.add(this.privilegeConverter.toResource(resourceEntity))
                }
            }
            return resources
        }

    override val subjects: List<BaseSubject>
        @Transactional(readOnly = true)
        get() {
            val zone = this.zoneResolver.zoneEntityOrFail
            val subjects = ArrayList<BaseSubject>()

            val subjectEntities = this.subjectRepository.findByZone(zone)
            if (!CollectionUtils.isEmpty(subjectEntities)) {
                for (subjectEntity in subjectEntities) {
                    subjects.add(this.privilegeConverter.toSubject(subjectEntity))
                }
            }
            return subjects
        }

    override fun appendResources(resources: List<BaseResource>?) {
        val zone = this.zoneResolver.zoneEntityOrFail

        if (CollectionUtils.isEmpty(resources)) {
            throw PrivilegeManagementException("Null Or Empty list of resources")
        }
        // fail fast if identifiers are missing or null
        validResourcesOrFail(resources!!)

        val entities = ArrayList<ResourceEntity>()
        appendResourcesInTransaction(resources, zone, entities)
    }

    @Transactional
    internal fun appendResourcesInTransaction(
        resources: List<BaseResource>, zone: ZoneEntity,
        entities: MutableList<ResourceEntity>
    ) {
        for (resource in resources) {
            val persistedResource = this.resourceRepository
                .getByZoneAndResourceIdentifier(zone, resource.resourceIdentifier)

            val entity = this.privilegeConverter.toResourceEntity(zone, resource)
            if (persistedResource != null) {
                LOGGER.debug(
                    "Found an existing resource with resourceIdentifier = {}, zone = {}. Upserting the same.",
                    resource.resourceIdentifier, zone
                )
                entity!!.id = persistedResource.id
            }
            entities.add(entity)
        }

        try {
            this.cache.resetForResources(zone.name, entities)
            this.resourceRepository.save(entities)
        } catch (e: Exception) {

            var message = String.format(
                "Unable to persist Resource(s) for zone = %s. Transaction was rolled back.",
                zone.toString()
            )
            if (constrainViolation(e)) {
                message = String.format("Duplicate Resource(s) identified by zone = %s.", zone.toString())
            }
            LOGGER.error(message, e)
            throw PrivilegeManagementException(message, e)
        }
    }

    @Transactional(readOnly = true)
    override fun getByResourceIdentifier(resourceIdentifier: String): BaseResource? {
        val zone = this.zoneResolver.zoneEntityOrFail
        val resourceEntity = this.resourceRepository
            .getByZoneAndResourceIdentifier(zone, resourceIdentifier)
        return createResource(resourceIdentifier, zone, resourceEntity)
    }

    @Transactional(readOnly = true)
    override fun getByResourceIdentifierWithInheritedAttributes(resourceIdentifier: String): BaseResource? {
        val zone = this.zoneResolver.zoneEntityOrFail
        val resourceEntity = this.resourceRepository
            .getResourceWithInheritedAttributes(zone, resourceIdentifier)
        return createResource(resourceIdentifier, zone, resourceEntity)
    }

    private fun createResource(
        resourceIdentifier: String, zone: ZoneEntity,
        resourceEntity: ResourceEntity?
    ): BaseResource? {
        val resource = this.privilegeConverter.toResource(resourceEntity)
        if (resource == null) {
            LOGGER.debug(
                "Unable to find the resource for resourceIdentifier = {} , zone = {}.", resourceIdentifier,
                zone
            )
        }
        return resource
    }

    override fun upsertResource(resource: BaseResource?): Boolean {
        val zone = this.zoneResolver.zoneEntityOrFail
        validateResourceOrFail(resource)

        val updatedResource = this.privilegeConverter.toResourceEntity(zone, resource)

        val persistedResource = upsertResourceInTransaction(resource!!, zone, updatedResource)

        // true if non previous persisted entity was there.
        return persistedResource == null
    }

    @Transactional
    internal fun upsertResourceInTransaction(
        resource: BaseResource, zone: ZoneEntity,
        updatedResource: ResourceEntity?
    ): ResourceEntity? {
        val persistedResource = this.resourceRepository
            .getByZoneAndResourceIdentifier(zone, resource.resourceIdentifier)

        if (persistedResource != null) {
            LOGGER.debug(
                "Found an existing resource with resourceIdentifier = {}, " + "zone = {}. Upserting the same.",
                resource.resourceIdentifier, zone
            )
            updatedResource!!.id = persistedResource.id
            this.cache.resetForResourcesByIds(
                zone.name,
                this.resourceRepository.getResourceEntityAndDescendantsIds(updatedResource)
            )
        } else {
            LOGGER.debug(
                "Found no existing resource. Creating a new one with the resourceIdentifier = {}," + " zone = {}.",
                resource.resourceIdentifier, zone
            )
            this.cache.resetForResourcesByIds(
                zone.name,
                setOf(updatedResource!!.resourceIdentifier)
            )
        }

        try {
            this.resourceRepository.save(updatedResource)
        } catch (e: Exception) {
            var message = String
                .format(
                    "Unable to persist Resource identified by resourceIdentifier = %s , zone = %s.",
                    resource.resourceIdentifier, zone.toString()
                )
            if (constrainViolation(e)) {
                message = String.format(
                    "Duplicate Resource identified by resourceIdentifier = %s, zone = %s.",
                    resource.resourceIdentifier, zone.toString()
                )
            }
            LOGGER.error(message, e)
            throw PrivilegeManagementException(message, e)
        }

        return persistedResource
    }

    /**
     * @param e
     * @return
     */
    private fun constrainViolation(e: Exception): Boolean {
        val exceptionType = e.javaClass
        return DataIntegrityViolationException::class.java.isAssignableFrom(exceptionType)
    }

    @Transactional
    override fun deleteResource(resourceIdentifier: String): Boolean {
        val zone = this.zoneResolver.zoneEntityOrFail
        var deleted = false
        val resourceEntity = this.resourceRepository
            .getByZoneAndResourceIdentifier(zone, resourceIdentifier)
        if (resourceEntity != null) {
            this.cache.resetForResourcesByIds(
                zone.name,
                this.resourceRepository.getResourceEntityAndDescendantsIds(resourceEntity)
            )
            this.resourceRepository.delete(resourceEntity.id)
            deleted = true
            LOGGER.info("Deleted resource with resourceId = {}, zone = {}.", resourceIdentifier, zone)
        }
        return deleted
    }

    override fun appendSubjects(subjects: List<BaseSubject>?) {
        val zone = this.zoneResolver.zoneEntityOrFail
        if (CollectionUtils.isEmpty(subjects)) {
            throw PrivilegeManagementException("Null Or Empty list of subjects.")
        }
        // fail fast if identifiers are missing or null
        validSubjectUrisOrFail(subjects!!)

        val subjectEntities = ArrayList<SubjectEntity>()

        appendSubjectsInTransaction(subjects, zone, subjectEntities)
    }

    @Transactional
    internal fun appendSubjectsInTransaction(
        subjects: List<BaseSubject>, zone: ZoneEntity,
        subjectEntities: MutableList<SubjectEntity>
    ) {
        for (subject in subjects) {
            val persistedSubject = this.subjectRepository
                .getByZoneAndSubjectIdentifier(zone, subject.subjectIdentifier)
            val entity = this.privilegeConverter.toSubjectEntity(zone, subject)
            if (persistedSubject != null) {
                entity!!.setId(persistedSubject.id)
            }
            subjectEntities.add(entity)
        }
        try {
            this.cache.resetForSubjects(zone.name, subjectEntities)
            this.subjectRepository.save(subjectEntities)
        } catch (e: Exception) {
            var message = String.format(
                "Unable to persist Subject(s) for zone = %s. Transaction was rolled back.",
                zone.toString()
            )
            if (constrainViolation(e)) {
                message = String.format("Duplicate Subject(s) identified by zone = %s", zone.toString())
            }
            LOGGER.error(message, e)
            throw PrivilegeManagementException(message, e)
        }
    }

    @Transactional(readOnly = true)
    override fun getBySubjectIdentifier(subjectIdentifier: String): BaseSubject? {
        val zone = this.zoneResolver.zoneEntityOrFail
        val subjectEntity = this.subjectRepository.getByZoneAndSubjectIdentifier(zone, subjectIdentifier)
        return createSubject(subjectIdentifier, zone, subjectEntity)
    }

    @Transactional(readOnly = true)
    override fun getBySubjectIdentifierWithInheritedAttributes(subjectIdentifier: String): BaseSubject? {
        val zone = this.zoneResolver.zoneEntityOrFail
        val subjectEntity = this.subjectRepository.getSubjectWithInheritedAttributes(zone, subjectIdentifier)
        return createSubject(subjectIdentifier, zone, subjectEntity)
    }

    private fun createSubject(
        subjectIdentifier: String, zone: ZoneEntity,
        subjectEntity: SubjectEntity?
    ): BaseSubject? {
        val subject = this.privilegeConverter.toSubject(subjectEntity)
        if (subject == null) {
            LOGGER.debug("Unable to find the subject for subjectIdentifier = {}, zone = {}.", subjectIdentifier, zone)
        }
        return subject
    }

    @Transactional(readOnly = true)
    override fun getBySubjectIdentifierAndScopes(subjectIdentifier: String, scopes: Set<Attribute>): BaseSubject? {
        val zone = this.zoneResolver.zoneEntityOrFail
        val subjectEntity = this.subjectRepository
            .getSubjectWithInheritedAttributesForScopes(zone, subjectIdentifier, scopes)
        return createSubject(subjectIdentifier, zone, subjectEntity)
    }

    override fun upsertSubject(subject: BaseSubject?): Boolean {
        val zone = this.zoneResolver.zoneEntityOrFail
        validateSubjectOrFail(subject)

        val updatedSubject = this.privilegeConverter.toSubjectEntity(zone, subject)

        val persistedSubject = upsertSubjectInTransaction(subject!!, zone, updatedSubject)

        // Return false if the persistedSubject is null, which means we updated an existing subject.
        return null == persistedSubject
    }

    @Transactional
    internal fun upsertSubjectInTransaction(
        subject: BaseSubject, zone: ZoneEntity,
        updatedSubject: SubjectEntity?
    ): SubjectEntity? {
        val persistedSubject = this.subjectRepository
            .getByZoneAndSubjectIdentifier(zone, subject.subjectIdentifier)

        if (persistedSubject != null) {
            updatedSubject!!.setId(persistedSubject.id)
            this.cache.resetForSubjectsByIds(
                zone.name,
                this.subjectRepository.getSubjectEntityAndDescendantsIds(updatedSubject)
            )
        } else {
            this.cache.resetForSubjectsByIds(
                zone.name,
                setOf(updatedSubject!!.subjectIdentifier)
            )
        }

        try {
            this.subjectRepository.save(updatedSubject)
        } catch (e: Exception) {
            var message = String
                .format(
                    "Unable to persist Subject identified by subjectIidentifier = %s , zone = %s.",
                    subject.subjectIdentifier, zone.toString()
                )
            if (constrainViolation(e)) {
                message = String.format(
                    "Duplicate Subject identified by subjectIidentifier = %s, zone = %s.",
                    subject.subjectIdentifier, zone.toString()
                )
            }
            LOGGER.error(message, e)
            throw PrivilegeManagementException(message, e)
        }

        return persistedSubject
    }

    @Transactional
    override fun deleteSubject(subjectIdentifier: String): Boolean {
        val zone = this.zoneResolver.zoneEntityOrFail

        var deleted = false

        val subjectEntity = this.subjectRepository.getByZoneAndSubjectIdentifier(zone, subjectIdentifier)
        if (subjectEntity != null) {
            this.cache.resetForSubjectsByIds(
                zone.name,
                this.subjectRepository.getSubjectEntityAndDescendantsIds(subjectEntity)
            )
            this.subjectRepository.delete(subjectEntity.id)
            deleted = true
            LOGGER.info("Deleted subject with subjectIdentifier={}, zone = {}.", subjectIdentifier, zone)
        }
        return deleted
    }

    private fun validSubjectUrisOrFail(subjects: List<BaseSubject>) {
        for (s in subjects) {
            validateSubjectOrFail(s)
        }
    }

    /**
     * @param s
     */
    private fun validateSubjectOrFail(s: BaseSubject?) {
        if (s == null) {
            throw PrivilegeManagementException("Subject is null.")
        }

        if (!s.isIdentifierValid) {
            throw PrivilegeManagementException(
                String.format(
                    "Subject missing subjectIdentifier = %s this is mandatory for POST API",
                    s.subjectIdentifier
                )
            )
        }
    }

    private fun validResourcesOrFail(resources: List<BaseResource>) {

        for (r in resources) {
            validateResourceOrFail(r)
        }
    }

    private fun validateResourceOrFail(r: BaseResource?) {
        if (r == null) {
            throw PrivilegeManagementException("Resource is null.")
        }

        if (!r.isIdentifierValid) {
            throw PrivilegeManagementException(
                String.format(
                    "Resource missing resourceIdentifier = %s ,this is mandatory for POST API",
                    r.resourceIdentifier
                )
            )
        }
    }
}
