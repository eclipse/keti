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

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;

@Component
public class SubjectRepositoryProxy implements SubjectRepository, SubjectHierarchicalRepository, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectRepositoryProxy.class);
    private static final String MESSAGE = "method not supported";

    @Autowired(required = false)
    private GraphSubjectRepository graphRepository;

    @Autowired
    @Qualifier("subjectRepository") // This is the bean id registered by Spring data JPA.
    private SubjectRepository nonGraphRepository;

    @Autowired
    private Environment environment;

    // This is set to the active repository being proxied to, based on the active profile. See afterPropertiesSet()
    private SubjectRepository activeRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (Arrays.asList(this.environment.getActiveProfiles()).contains("titan")) {
            this.activeRepository = this.graphRepository;
            LOGGER.info("Subject hierarchical repository enabled.");
        } else {
            this.activeRepository = this.nonGraphRepository;
            LOGGER.info("Subject non-hierarchical repository enabled.");
        }
    }

    @Override
    public List<SubjectEntity> findAll() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public List<SubjectEntity> findAll(final Sort sort) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public List<SubjectEntity> findAll(final Iterable<Long> ids) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends SubjectEntity> List<S> save(final Iterable<S> entities) {
        return this.activeRepository.save(entities);
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends SubjectEntity> S saveAndFlush(final S entity) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public void deleteInBatch(final Iterable<SubjectEntity> entities) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public void deleteAllInBatch() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public SubjectEntity getOne(final Long id) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends SubjectEntity> List<S> findAll(final Example<S> example, final Sort sort) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends SubjectEntity> List<S> findAll(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public Page<SubjectEntity> findAll(final Pageable pageable) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends SubjectEntity> Page<S> findAll(final Example<S> example, final Pageable pageable) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends SubjectEntity> S save(final S entity) {
        return this.activeRepository.save(entity);
    }

    @Override
    public SubjectEntity findOne(final Long id) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends SubjectEntity> S findOne(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }


    @Override
    public boolean exists(final Long id) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends SubjectEntity> long count(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public void delete(final Long id) {
        this.activeRepository.delete(id);
    }

    @Override
    public void delete(final SubjectEntity entity) {
        this.activeRepository.delete(entity);
    }

    @Override
    public void delete(final Iterable<? extends SubjectEntity> entities) {
        this.activeRepository.delete(entities);
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public SubjectEntity getSubjectWithInheritedAttributesForScopes(final ZoneEntity zone,
            final String subjectIdentifier, final Set<Attribute> scopes) {
        if (this.activeRepository == this.graphRepository) { // i.e. titan is enabled
            return this.graphRepository.getSubjectWithInheritedAttributesForScopes(zone, subjectIdentifier, scopes);
        } else {
            return this.nonGraphRepository.getByZoneAndSubjectIdentifier(zone, subjectIdentifier);
        }
    }

    @Override
    public SubjectEntity getSubjectWithInheritedAttributes(final ZoneEntity zone, final String subjectIdentifier) {
        if (this.activeRepository == this.graphRepository) { // i.e. titan is enabled
            return this.graphRepository.getSubjectWithInheritedAttributes(zone, subjectIdentifier);
        } else {
            return this.nonGraphRepository.getByZoneAndSubjectIdentifier(zone, subjectIdentifier);
        }
    }

    @Override
    public List<SubjectEntity> findByZone(final ZoneEntity zone) {
        return this.activeRepository.findByZone(zone);
    }

    @Override
    public SubjectEntity getByZoneAndSubjectIdentifier(final ZoneEntity zone, final String subjectIdentifier) {
        return this.activeRepository.getByZoneAndSubjectIdentifier(zone, subjectIdentifier);
    }

    @Override
    public Set<String> getSubjectEntityAndDescendantsIds(final SubjectEntity entity) {
        if (this.activeRepository == this.graphRepository) { // i.e. titan is enabled
            return this.graphRepository.getSubjectEntityAndDescendantsIds(entity);
        } else {
            return Collections.singleton(entity.getSubjectIdentifier());
        }
    }

    @Override
    public <S extends SubjectEntity> boolean exists(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }
}
