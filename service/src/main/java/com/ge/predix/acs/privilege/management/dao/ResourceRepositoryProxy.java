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

package com.ge.predix.acs.privilege.management.dao;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.zone.management.dao.ZoneEntity;

@Component
public class ResourceRepositoryProxy implements ResourceRepository, ResourceHierarchicalRepository, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRepositoryProxy.class);
    private static final String MESSAGE = "method not supported";

    @Autowired(required = false)
    private GraphResourceRepository graphRepository;

    @Autowired
    @Qualifier("resourceRepository") // This is the bean id registered by Spring data JPA.
    private ResourceRepository nonGraphRepository;

    @Autowired
    private Environment environment;

    // This is set to the active repository being proxied to, based on the active profile. See afterPropertiesSet()
    private ResourceRepository activeRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (Arrays.asList(this.environment.getActiveProfiles()).contains("titan")) {
            this.activeRepository = this.graphRepository;
            LOGGER.info("Resource hierarchical repository enabled.");
        } else {
            this.activeRepository = this.nonGraphRepository;
            LOGGER.info("Resource non-hierarchical repository enabled.");
        }
    }

    @Override
    public List<ResourceEntity> findAll() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public List<ResourceEntity> findAll(final Sort arg0) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public List<ResourceEntity> findAll(final Iterable<Long> arg0) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends ResourceEntity> Page<S> findAll(final Example<S> example, final Pageable pageable) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends ResourceEntity> List<S> findAll(final Example<S> example, final Sort sort) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends ResourceEntity> List<S> findAll(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends ResourceEntity> List<S> save(final Iterable<S> arg0) {
        return this.activeRepository.save(arg0);
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends ResourceEntity> S saveAndFlush(final S arg0) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public void deleteInBatch(final Iterable<ResourceEntity> arg0) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public void deleteAllInBatch() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public ResourceEntity getOne(final Long arg0) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public Page<ResourceEntity> findAll(final Pageable arg0) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends ResourceEntity> S save(final S arg0) {
        return this.activeRepository.save(arg0);
    }

    @Override
    public ResourceEntity findOne(final Long arg0) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends ResourceEntity> S findOne(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public boolean exists(final Long arg0) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends ResourceEntity> long count(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends ResourceEntity> boolean exists(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public void delete(final Long arg0) {
        this.activeRepository.delete(arg0);
    }

    @Override
    public void delete(final ResourceEntity arg0) {
        this.activeRepository.delete(arg0);

    }

    @Override
    public void delete(final Iterable<? extends ResourceEntity> arg0) {
        this.activeRepository.delete(arg0);
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public ResourceEntity getResourceWithInheritedAttributes(final ZoneEntity zone, final String resourceIdentifier) {
        if (this.activeRepository == this.graphRepository) { // i.e. titan is enabled
            return this.graphRepository.getResourceWithInheritedAttributes(zone, resourceIdentifier);
        } else {
            return this.nonGraphRepository.getByZoneAndResourceIdentifier(zone, resourceIdentifier);
        }
    }

    @Override
    public List<ResourceEntity> findByZone(final ZoneEntity zone) {
        return this.activeRepository.findByZone(zone);
    }

    @Override
    public ResourceEntity getByZoneAndResourceIdentifier(final ZoneEntity zone, final String resourceIdentifier) {
        return this.activeRepository.getByZoneAndResourceIdentifier(zone, resourceIdentifier);
    }

    @Override
    public Set<String> getResourceEntityAndDescendantsIds(final ResourceEntity entity) {
        if (this.activeRepository == this.graphRepository) { // i.e. titan is enabled
            return this.graphRepository.getResourceEntityAndDescendantsIds(entity);
        } else {
            return Collections.singleton(entity.getResourceIdentifier());
        }
    }
}
