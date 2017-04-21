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

package com.ge.predix.acs.privilege.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationCache;
import com.ge.predix.acs.privilege.management.dao.ResourceEntity;
import com.ge.predix.acs.privilege.management.dao.ResourceRepositoryProxy;
import com.ge.predix.acs.privilege.management.dao.SubjectEntity;
import com.ge.predix.acs.privilege.management.dao.SubjectRepositoryProxy;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.resolver.ZoneResolver;

/**
 * The implementation of privilege management.
 *
 * @author 212319607
 */
@Component
@SuppressWarnings("nls")
public class PrivilegeManagementServiceImpl implements PrivilegeManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrivilegeManagementServiceImpl.class);

    @Autowired
    private PolicyEvaluationCache cache;

    @Autowired
    private SubjectRepositoryProxy subjectRepository;

    @Autowired
    private ResourceRepositoryProxy resourceRepository;

    @Autowired
    private ZoneResolver zoneResolver;

    private final PrivilegeConverter privilegeConverter = new PrivilegeConverter();

    @Override
    public void appendResources(final List<BaseResource> resources) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();

        if (CollectionUtils.isEmpty(resources)) {
            throw new PrivilegeManagementException("Null Or Empty list of resources");
        }
        // fail fast if identifiers are missing or null
        validResourcesOrFail(resources);

        List<ResourceEntity> entities = new ArrayList<>();
        appendResourcesInTransaction(resources, zone, entities);
    }

    @Transactional
    private void appendResourcesInTransaction(final List<BaseResource> resources, final ZoneEntity zone,
            final List<ResourceEntity> entities) {
        for (BaseResource resource : resources) {
            ResourceEntity persistedResource = this.resourceRepository
                    .getByZoneAndResourceIdentifier(zone, resource.getResourceIdentifier());

            ResourceEntity entity = this.privilegeConverter.toResourceEntity(zone, resource);
            if (persistedResource != null) {
                LOGGER.debug("Found an existing resource with resourceIdentifier = {}, zone = {}. Upserting the same.",
                        resource.getResourceIdentifier(), zone);
                entity.setId(persistedResource.getId());
            }
            entities.add(entity);
        }

        try {
            this.cache.resetForResources(zone.getName(), entities);
            this.resourceRepository.save(entities);
        } catch (Exception e) {

            String message = String.format("Unable to persist Resource(s) for zone = %s. Transaction was rolled back.",
                    zone.toString());
            if (constrainViolation(e)) {
                message = String.format("Duplicate Resource(s) identified by zone = %s.", zone.toString());
            }
            LOGGER.error(message, e);
            throw new PrivilegeManagementException(message, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BaseResource> getResources() {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();

        List<BaseResource> resources = new ArrayList<>();
        List<ResourceEntity> resourceEntities = this.resourceRepository.findByZone(zone);

        if (!CollectionUtils.isEmpty(resourceEntities)) {
            for (ResourceEntity resourceEntity : resourceEntities) {
                resources.add(this.privilegeConverter.toResource(resourceEntity));
            }
        }
        return resources;
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResource getByResourceIdentifier(final String resourceIdentifier) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        ResourceEntity resourceEntity = this.resourceRepository
                .getByZoneAndResourceIdentifier(zone, resourceIdentifier);
        return createResource(resourceIdentifier, zone, resourceEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResource getByResourceIdentifierWithInheritedAttributes(final String resourceIdentifier) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        ResourceEntity resourceEntity = this.resourceRepository
                .getResourceWithInheritedAttributes(zone, resourceIdentifier);
        return createResource(resourceIdentifier, zone, resourceEntity);
    }

    private BaseResource createResource(final String resourceIdentifier, final ZoneEntity zone,
            final ResourceEntity resourceEntity) {
        BaseResource resource = this.privilegeConverter.toResource(resourceEntity);
        if (resource == null) {
            LOGGER.debug("Unable to find the resource for resourceIdentifier = {} , zone = {}.", resourceIdentifier,
                    zone);
        }
        return resource;
    }

    @Override
    public boolean upsertResource(final BaseResource resource) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        validateResourceOrFail(resource);

        ResourceEntity updatedResource = this.privilegeConverter.toResourceEntity(zone, resource);

        ResourceEntity persistedResource = upsertResourceInTransaction(resource, zone, updatedResource);

        // true if non previous persisted entity was there.
        return persistedResource == null;
    }

    @Transactional
    private ResourceEntity upsertResourceInTransaction(final BaseResource resource, final ZoneEntity zone,
            final ResourceEntity updatedResource) {
        ResourceEntity persistedResource = this.resourceRepository
                .getByZoneAndResourceIdentifier(zone, resource.getResourceIdentifier());

        if (persistedResource != null) {
            LOGGER.debug("Found an existing resource with resourceIdentifier = {}, " + "zone = {}. Upserting the same.",
                    resource.getResourceIdentifier(), zone);
            updatedResource.setId(persistedResource.getId());
        } else {
            LOGGER.debug(
                    "Found no existing resource. Creating a new one with the resourceIdentifier = {}," + " zone = {}.",
                    resource.getResourceIdentifier(), zone);
        }

        try {
            this.cache.resetForResourcesByIds(zone.getName(),
                    this.resourceRepository.getResourceEntityAndDescendantsIds(updatedResource));
            this.resourceRepository.save(updatedResource);
        } catch (Exception e) {
            String message = String
                    .format("Unable to persist Resource identified by resourceIdentifier = %s , zone = %s.",
                            resource.getResourceIdentifier(), zone.toString());
            if (constrainViolation(e)) {
                message = String.format("Duplicate Resource identified by resourceIdentifier = %s, zone = %s.",
                        resource.getResourceIdentifier(), zone.toString());
            }
            LOGGER.error(message, e);
            throw new PrivilegeManagementException(message, e);
        }
        return persistedResource;
    }

    /**
     * @param e
     * @return
     */
    private boolean constrainViolation(final Exception e) {
        Class<? extends Exception> exceptionType = e.getClass();
        return DataIntegrityViolationException.class.isAssignableFrom(exceptionType);
    }

    @Override
    @Transactional
    public boolean deleteResource(final String resourceIdentifier) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        boolean deleted = false;
        ResourceEntity resourceEntity = this.resourceRepository
                .getByZoneAndResourceIdentifier(zone, resourceIdentifier);
        if (resourceEntity != null) {
            this.cache.resetForResourcesByIds(zone.getName(),
                    this.resourceRepository.getResourceEntityAndDescendantsIds(resourceEntity));
            this.resourceRepository.delete(resourceEntity.getId());
            deleted = true;
            LOGGER.info("Deleted resource with resourceId = {}, zone = {}.", resourceIdentifier, zone);
        }
        return deleted;
    }

    @Override
    public void appendSubjects(final List<BaseSubject> subjects) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        if (CollectionUtils.isEmpty(subjects)) {
            throw new PrivilegeManagementException("Null Or Empty list of subjects.");
        }
        // fail fast if identifiers are missing or null
        validSubjectUrisOrFail(subjects);

        List<SubjectEntity> subjectEntities = new ArrayList<>();

        appendSubjectsInTransaction(subjects, zone, subjectEntities);
    }

    @Transactional
    private void appendSubjectsInTransaction(final List<BaseSubject> subjects, final ZoneEntity zone,
            final List<SubjectEntity> subjectEntities) {
        for (BaseSubject subject : subjects) {
            SubjectEntity persistedSubject = this.subjectRepository
                    .getByZoneAndSubjectIdentifier(zone, subject.getSubjectIdentifier());
            SubjectEntity entity = this.privilegeConverter.toSubjectEntity(zone, subject);
            if (persistedSubject != null) {
                entity.setId(persistedSubject.getId());
            }
            subjectEntities.add(entity);
        }
        try {
            this.cache.resetForSubjects(zone.getName(), subjectEntities);
            this.subjectRepository.save(subjectEntities);
        } catch (Exception e) {
            String message = String.format("Unable to persist Subject(s) for zone = %s. Transaction was rolled back.",
                    zone.toString());
            if (constrainViolation(e)) {
                message = String.format("Duplicate Subject(s) identified by zone = %s", zone.toString());
            }
            LOGGER.error(message, e);
            throw new PrivilegeManagementException(message, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BaseSubject> getSubjects() {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        List<BaseSubject> subjects = new ArrayList<>();

        List<SubjectEntity> subjectEntities = this.subjectRepository.findByZone(zone);
        if (!CollectionUtils.isEmpty(subjectEntities)) {
            for (SubjectEntity subjectEntity : subjectEntities) {
                subjects.add(this.privilegeConverter.toSubject(subjectEntity));
            }
        }
        return subjects;
    }

    @Override
    @Transactional(readOnly = true)
    public BaseSubject getBySubjectIdentifier(final String subjectIdentifier) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        SubjectEntity subjectEntity = this.subjectRepository.getByZoneAndSubjectIdentifier(zone, subjectIdentifier);
        return createSubject(subjectIdentifier, zone, subjectEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseSubject getBySubjectIdentifierWithInheritedAttributes(final String subjectIdentifier) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        SubjectEntity subjectEntity = this.subjectRepository.getSubjectWithInheritedAttributes(zone, subjectIdentifier);
        return createSubject(subjectIdentifier, zone, subjectEntity);
    }

    private BaseSubject createSubject(final String subjectIdentifier, final ZoneEntity zone,
            final SubjectEntity subjectEntity) {
        BaseSubject subject = this.privilegeConverter.toSubject(subjectEntity);
        if (subject == null) {
            LOGGER.debug("Unable to find the subject for subjectIdentifier = {}, zone = {}.", subjectIdentifier, zone);
        }
        return subject;
    }

    @Override
    @Transactional(readOnly = true)
    public BaseSubject getBySubjectIdentifierAndScopes(final String subjectIdentifier, final Set<Attribute> scopes) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        SubjectEntity subjectEntity = this.subjectRepository
                .getSubjectWithInheritedAttributesForScopes(zone, subjectIdentifier, scopes);
        return createSubject(subjectIdentifier, zone, subjectEntity);
    }

    @Override
    public boolean upsertSubject(final BaseSubject subject) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();
        validateSubjectOrFail(subject);

        SubjectEntity updatedSubject = this.privilegeConverter.toSubjectEntity(zone, subject);

        SubjectEntity persistedSubject = upsertSubjectInTransaction(subject, zone, updatedSubject);

        // Return false if the persistedSubject is null, which means we updated an existing subject.
        if (null != persistedSubject) {
            return false;
        }
        return true;
    }

    @Transactional
    private SubjectEntity upsertSubjectInTransaction(final BaseSubject subject, final ZoneEntity zone,
            final SubjectEntity updatedSubject) {
        SubjectEntity persistedSubject = this.subjectRepository
                .getByZoneAndSubjectIdentifier(zone, subject.getSubjectIdentifier());

        if (persistedSubject != null) {
            updatedSubject.setId(persistedSubject.getId());
        }

        try {
            this.cache.resetForSubjectsByIds(zone.getName(),
                    this.subjectRepository.getSubjectEntityAndDescendantsIds(updatedSubject));
            this.subjectRepository.save(updatedSubject);
        } catch (Exception e) {
            String message = String
                    .format("Unable to persist Subject identified by subjectIidentifier = %s , zone = %s.",
                            subject.getSubjectIdentifier(), zone.toString());
            if (constrainViolation(e)) {
                message = String.format("Duplicate Subject identified by subjectIidentifier = %s, zone = %s.",
                        subject.getSubjectIdentifier(), zone.toString());
            }
            LOGGER.error(message, e);
            throw new PrivilegeManagementException(message, e);
        }
        return persistedSubject;
    }

    @Override
    @Transactional
    public boolean deleteSubject(final String subjectIdentifier) {
        ZoneEntity zone = this.zoneResolver.getZoneEntityOrFail();

        boolean deleted = false;

        SubjectEntity subjectEntity = this.subjectRepository.getByZoneAndSubjectIdentifier(zone, subjectIdentifier);
        if (subjectEntity != null) {
            this.cache.resetForSubjectsByIds(zone.getName(),
                    this.subjectRepository.getSubjectEntityAndDescendantsIds(subjectEntity));
            this.subjectRepository.delete(subjectEntity.getId());
            deleted = true;
            LOGGER.info("Deleted subject with subjectIdentifier={}, zone = {}.", subjectIdentifier, zone);
        }
        return deleted;
    }

    private void validSubjectUrisOrFail(final List<BaseSubject> subjects) {
        for (BaseSubject s : subjects) {
            validateSubjectOrFail(s);
        }
    }

    /**
     * @param s
     */
    private void validateSubjectOrFail(final BaseSubject s) {
        if (s == null) {
            throw new PrivilegeManagementException("Subject is null.");
        }

        if (!s.isIdentifierValid()) {
            throw new PrivilegeManagementException(
                    String.format("Subject missing subjectIdentifier = %s this is mandatory for POST API",
                            s.getSubjectIdentifier()));
        }
    }

    private void validResourcesOrFail(final List<BaseResource> resources) {

        for (BaseResource r : resources) {
            validateResourceOrFail(r);
        }
    }

    private void validateResourceOrFail(final BaseResource r) {
        if (r == null) {
            throw new PrivilegeManagementException("Resource is null.");
        }

        if (!r.isIdentifierValid()) {
            throw new PrivilegeManagementException(
                    String.format("Resource missing resourceIdentifier = %s ,this is mandatory for POST API",
                            r.getResourceIdentifier()));
        }
    }

    public void setCache(final PolicyEvaluationCache cache) {
        this.cache = cache;
    }
}
