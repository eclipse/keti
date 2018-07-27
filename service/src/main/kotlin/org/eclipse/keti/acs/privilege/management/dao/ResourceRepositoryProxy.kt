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

package org.eclipse.keti.acs.privilege.management.dao

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
import java.util.Arrays

private val LOGGER = LoggerFactory.getLogger(ResourceRepositoryProxy::class.java)
private const val MESSAGE = "method not supported"

@Component
class ResourceRepositoryProxy : ResourceRepository, ResourceHierarchicalRepository, InitializingBean {

    @Autowired(required = false)
    private val graphRepository: GraphResourceRepository? = null

    @Autowired
    @Qualifier("resourceRepository") // This is the bean id registered by Spring data JPA.
    private lateinit var nonGraphRepository: ResourceRepository

    @Autowired
    private lateinit var environment: Environment

    // This is set to the active repository being proxied to, based on the active profile. See afterPropertiesSet()
    private var activeRepository: ResourceRepository? = null

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        if (Arrays.asList(*this.environment.activeProfiles).contains("graph")) {
            this.activeRepository = this.graphRepository
            LOGGER.info("Resource hierarchical repository enabled.")
        } else {
            this.activeRepository = this.nonGraphRepository
            LOGGER.info("Resource non-hierarchical repository enabled.")
        }
    }

    override fun findAll(): List<ResourceEntity> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun findAll(arg0: Sort): List<ResourceEntity> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun findAll(arg0: Iterable<Long>): List<ResourceEntity> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : ResourceEntity> findAll(
        example: Example<S>,
        pageable: Pageable
    ): Page<S> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : ResourceEntity> findAll(
        example: Example<S>,
        sort: Sort
    ): List<S> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : ResourceEntity> findAll(example: Example<S>): List<S> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : ResourceEntity> save(arg0: Iterable<S>): List<S> {
        return this.activeRepository!!.save(arg0)
    }

    override fun flush() {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : ResourceEntity> saveAndFlush(arg0: S): S {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun deleteInBatch(arg0: Iterable<ResourceEntity>) {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun deleteAllInBatch() {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun getOne(arg0: Long?): ResourceEntity {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun findAll(arg0: Pageable): Page<ResourceEntity> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : ResourceEntity> save(arg0: S): S {
        return this.activeRepository!!.save(arg0)
    }

    override fun findOne(arg0: Long?): ResourceEntity {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : ResourceEntity> findOne(example: Example<S>): S {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun exists(arg0: Long?): Boolean {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun count(): Long {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : ResourceEntity> count(example: Example<S>): Long {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : ResourceEntity> exists(example: Example<S>): Boolean {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun delete(arg0: Long?) {
        this.activeRepository!!.delete(arg0)
    }

    override fun delete(arg0: ResourceEntity) {
        this.activeRepository!!.delete(arg0)
    }

    override fun delete(arg0: Iterable<ResourceEntity>) {
        this.activeRepository!!.delete(arg0)
    }

    override fun deleteAll() {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun getResourceWithInheritedAttributes(
        zone: ZoneEntity,
        resourceIdentifier: String
    ): ResourceEntity? {
        return if (this.activeRepository === this.graphRepository) { // i.e. graph is enabled
            this.graphRepository!!.getResourceWithInheritedAttributes(zone, resourceIdentifier)
        } else {
            this.nonGraphRepository.getByZoneAndResourceIdentifier(zone, resourceIdentifier)
        }
    }

    override fun findByZone(zoneEntity: ZoneEntity): List<ResourceEntity> {
        return this.activeRepository!!.findByZone(zoneEntity)
    }

    override fun getByZoneAndResourceIdentifier(
        zone: ZoneEntity,
        resourceIdentifier: String
    ): ResourceEntity? {
        return this.activeRepository!!.getByZoneAndResourceIdentifier(zone, resourceIdentifier)
    }

    override fun getResourceEntityAndDescendantsIds(entity: ResourceEntity?): Set<String> {
        return if (this.activeRepository === this.graphRepository) { // i.e. graph is enabled
            this.graphRepository!!.getResourceEntityAndDescendantsIds(entity)
        } else {
            setOf(entity!!.resourceIdentifier!!)
        }
    }
}
