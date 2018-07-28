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

package org.eclipse.keti.acs.privilege.management.dao

import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.env.Environment
import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

private val LOGGER = LoggerFactory.getLogger(SubjectRepositoryProxy::class.java)
private const val MESSAGE = "method not supported"

@Component
class SubjectRepositoryProxy : SubjectRepository, SubjectHierarchicalRepository, InitializingBean {

    @Autowired(required = false)
    private val graphRepository: GraphSubjectRepository? = null

    @Autowired
    @Qualifier("subjectRepository") // This is the bean id registered by Spring data JPA.
    private lateinit var nonGraphRepository: SubjectRepository

    @Autowired
    private lateinit var environment: Environment

    // This is set to the active repository being proxied to, based on the active profile. See afterPropertiesSet()
    private var activeRepository: SubjectRepository? = null

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        if (listOf(*this.environment.activeProfiles).contains("graph")) {
            this.activeRepository = this.graphRepository
            LOGGER.info("Subject hierarchical repository enabled.")
        } else {
            this.activeRepository = this.nonGraphRepository
            LOGGER.info("Subject non-hierarchical repository enabled.")
        }
    }

    override fun findAll(): List<SubjectEntity> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun findAll(sort: Sort): List<SubjectEntity> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun findAll(ids: Iterable<Long>): List<SubjectEntity> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : SubjectEntity> save(entities: Iterable<S>): List<S> {
        return this.activeRepository!!.save(entities)
    }

    override fun flush() {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : SubjectEntity> saveAndFlush(entity: S): S {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun deleteInBatch(entities: Iterable<SubjectEntity>) {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun deleteAllInBatch() {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun getOne(id: Long?): SubjectEntity {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : SubjectEntity> findAll(
        example: Example<S>,
        sort: Sort
    ): List<S> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : SubjectEntity> findAll(example: Example<S>): List<S> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun findAll(pageable: Pageable): Page<SubjectEntity> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : SubjectEntity> findAll(
        example: Example<S>,
        pageable: Pageable
    ): Page<S> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : SubjectEntity> save(entity: S): S {
        return this.activeRepository!!.save(entity)
    }

    override fun findOne(id: Long?): SubjectEntity {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : SubjectEntity> findOne(example: Example<S>): S {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun exists(id: Long?): Boolean {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun count(): Long {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : SubjectEntity> count(example: Example<S>): Long {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun delete(id: Long?) {
        this.activeRepository!!.delete(id)
    }

    override fun delete(entity: SubjectEntity) {
        this.activeRepository!!.delete(entity)
    }

    override fun delete(entities: Iterable<SubjectEntity>) {
        this.activeRepository!!.delete(entities)
    }

    override fun deleteAll() {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun getSubjectWithInheritedAttributesForScopes(
        zone: ZoneEntity,
        subjectIdentifier: String,
        scopes: Set<Attribute>?
    ): SubjectEntity? {
        return if (this.activeRepository === this.graphRepository) { // i.e. graph is enabled
            this.graphRepository!!.getSubjectWithInheritedAttributesForScopes(zone, subjectIdentifier, scopes)
        } else {
            this.nonGraphRepository.getByZoneAndSubjectIdentifier(zone, subjectIdentifier)
        }
    }

    override fun getSubjectWithInheritedAttributes(
        zone: ZoneEntity,
        subjectIdentifier: String
    ): SubjectEntity? {
        return if (this.activeRepository === this.graphRepository) { // i.e. graph is enabled
            this.graphRepository!!.getSubjectWithInheritedAttributes(zone, subjectIdentifier)
        } else {
            this.nonGraphRepository.getByZoneAndSubjectIdentifier(zone, subjectIdentifier)
        }
    }

    override fun findByZone(zoneEntity: ZoneEntity): List<SubjectEntity> {
        return this.activeRepository!!.findByZone(zoneEntity)
    }

    override fun getByZoneAndSubjectIdentifier(
        zone: ZoneEntity,
        subjectIdentifier: String
    ): SubjectEntity? {
        return this.activeRepository!!.getByZoneAndSubjectIdentifier(zone, subjectIdentifier)
    }

    override fun getSubjectEntityAndDescendantsIds(entity: SubjectEntity?): Set<String> {
        return if (this.activeRepository === this.graphRepository) { // i.e. graph is enabled
            this.graphRepository!!.getSubjectEntityAndDescendantsIds(entity)
        } else {
            setOf(entity!!.subjectIdentifier!!)
        }
    }

    override fun <S : SubjectEntity> exists(example: Example<S>): Boolean {
        throw UnsupportedOperationException(MESSAGE)
    }
}
