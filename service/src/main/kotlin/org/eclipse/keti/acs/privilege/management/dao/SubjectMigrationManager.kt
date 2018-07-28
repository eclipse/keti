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

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

private val LOGGER = LoggerFactory.getLogger(SubjectMigrationManager::class.java)

@Component
@Profile("graph")
class SubjectMigrationManager {

    fun doSubjectMigration(
        subjectRepository: SubjectRepository,
        subjectHierarchicalRepository: GraphSubjectRepository,
        pageSize: Int
    ) {
        var numOfSubjectsSaved = 0
        var pageRequest: Pageable? = PageRequest(0, pageSize, Sort("id"))
        val numOfSubjectEntitiesToMigrate = subjectRepository.count()
        var pageOfSubjects: Page<SubjectEntity>

        do {
            pageOfSubjects = subjectRepository.findAll(pageRequest)
            val subjectListToSave = pageOfSubjects.content
            numOfSubjectsSaved += pageOfSubjects.numberOfElements
            subjectListToSave.forEach { item ->
                item.id = 0L
                LOGGER.trace(
                    "doSubjectMigration Subject-Id: {} Zone-name: {} Zone-id: {}", item.subjectIdentifier,
                    item.zone!!.name, item.zone!!.id
                )
            }

            subjectHierarchicalRepository.save(subjectListToSave)
            LOGGER.info("Total subjects migrated so far: {}/{}", numOfSubjectsSaved, numOfSubjectEntitiesToMigrate)
            pageRequest = pageOfSubjects.nextPageable()
        } while (pageOfSubjects.hasNext())

        LOGGER.info("Number of subject entities migrated: {}", numOfSubjectsSaved)
        LOGGER.info("Subject migration to graph completed.")
    }

    fun rollbackMigratedData(subjectHierarchicalRepository: GraphSubjectRepository) {
        LOGGER.info("Initiating rollback for subjectHierarchicalRepository")
        subjectHierarchicalRepository.deleteAll()
    }
}
