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

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.eclipse.keti.acs.commons.web.BaseRestApi
import org.eclipse.keti.acs.commons.web.PARENTS_ATTR_NOT_SUPPORTED_MSG
import org.eclipse.keti.acs.commons.web.RestApiException
import org.eclipse.keti.acs.commons.web.SUBJECTS_URL
import org.eclipse.keti.acs.commons.web.SUBJECT_URL
import org.eclipse.keti.acs.commons.web.V1
import org.eclipse.keti.acs.commons.web.created
import org.eclipse.keti.acs.commons.web.expand
import org.eclipse.keti.acs.commons.web.noContent
import org.eclipse.keti.acs.commons.web.notFound
import org.eclipse.keti.acs.commons.web.ok
import org.eclipse.keti.acs.rest.BaseSubject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.DELETE
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RequestMethod.PUT
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Arrays

@RestController
class SubjectPrivilegeManagementController : BaseRestApi() {

    @Autowired
    private lateinit var service: PrivilegeManagementService

    private var graphProfileActive: Boolean? = null

    @ApiOperation(
        value = "Retrieves the list of subjects for the given zone.",
        tags = ["Attribute Management"]
    )
    @RequestMapping(
        method = [GET],
        value = [(V1 + SUBJECTS_URL)],
        produces = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    fun getSubjects(): ResponseEntity<List<BaseSubject>> {
        val subjects = this.service.subjects
        return ok(subjects)
    }

    private fun getGraphProfileActive(): Boolean? {
        if (this.graphProfileActive == null) {
            this.graphProfileActive = Arrays.asList(*this.environment.activeProfiles).contains("graph")
        }

        return this.graphProfileActive
    }

    private fun failIfParentsSpecified(subjects: List<BaseSubject>) {
        if (this.getGraphProfileActive()!!) {
            return
        }

        for (subject in subjects) {
            if (!CollectionUtils.isEmpty(subject.parents)) {
                throw RestApiException(HttpStatus.NOT_IMPLEMENTED, PARENTS_ATTR_NOT_SUPPORTED_MSG)
            }
        }
    }

    @ApiOperation(
        value = "Creates a list of subjects. Existing subjects will be updated with the provided values.",
        tags = ["Attribute Management"]
    )
    @ApiResponses(value = [(ApiResponse(code = 204, message = "Subject objects added successfully."))])
    @RequestMapping(
        method = [POST],
        value = [(V1 + SUBJECTS_URL)],
        consumes = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    fun appendsubjects(@RequestBody subjects: List<BaseSubject>): ResponseEntity<Void> {
        try {
            this.failIfParentsSpecified(subjects)

            this.service.appendSubjects(subjects)
            return noContent()
        } catch (e: RestApiException) {
            // NOTE: This block is necessary to avoid accidentally
            // converting the HTTP status code to an unintended one
            throw e
        } catch (e: Exception) {
            throw RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e)
        }
    }

    @ApiOperation(
        value = "Retrieves the subject for the given zone. The subjectIdentifier must be URL encoded in " + "application/x-www-form-urlencoded format with UTF-8.",
        tags = ["Attribute Management"]
    )
    @RequestMapping(method = [GET], value = [(V1 + SUBJECT_URL)])
    fun getSubject(
        @PathVariable("subjectIdentifier") subjectIdentifier: String,
        @RequestParam(name = "includeInheritedAttributes", defaultValue = "false")
        includeInheritedAttributes: Boolean
    ): ResponseEntity<BaseSubject> {
        val subject: BaseSubject? = if (includeInheritedAttributes) {
            this.service.getBySubjectIdentifierWithInheritedAttributes(subjectIdentifier)
        } else {
            this.service.getBySubjectIdentifier(subjectIdentifier)
        }

        return if (subject == null) {
            notFound()
        } else ok(subject)
    }

    @ApiOperation(value = "Creates/Updates a given Subject.", tags = ["Attribute Management"])
    @RequestMapping(
        method = [PUT],
        value = [(V1 + SUBJECT_URL)],
        consumes = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    fun putSubject(
        @RequestBody subject: BaseSubject,
        @PathVariable("subjectIdentifier") subjectIdentifier: String
    ): ResponseEntity<BaseSubject> {
        try {
            this.failIfParentsSpecified(listOf(subject))

            if (StringUtils.isEmpty(subject.subjectIdentifier)) {
                subject.subjectIdentifier = subjectIdentifier
            }

            validSubjectIdentifierOrFail(subject, subjectIdentifier)

            val createdSubject = this.service.upsertSubject(subject)

            val subjectUri = expand(SUBJECT_URL, "subjectIdentifier:$subjectIdentifier")

            return if (createdSubject) {
                created(subjectUri.rawPath, false)
            } else created(subjectUri.rawPath, true)
        } catch (e: RestApiException) {
            // NOTE: This block is necessary to avoid accidentally
            // converting the HTTP status code to an unintended one
            throw e
        } catch (e: Exception) {
            throw RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e)
        }
    }

    @ApiOperation(
        value = "Deletes the subject for a given zone. The subjectIdentifier must be URL encoded in " + "application/x-www-form-urlencoded format with UTF-8.",
        tags = ["Attribute Management"]
    )
    @RequestMapping(method = [DELETE], value = [(V1 + SUBJECT_URL)])
    fun deleteSubject(@PathVariable("subjectIdentifier") subjectIdentifier: String): ResponseEntity<Void> {
        this.service.deleteSubject(subjectIdentifier)
        return noContent()
    }

    private fun validSubjectIdentifierOrFail(
        subject: BaseSubject,
        subjectIdentifier: String
    ) {
        if (subjectIdentifier != subject.subjectIdentifier) {
            throw RestApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                String.format(
                    "Subject identifier = %s, does not match the one provided in URI = %s",
                    subject.subjectIdentifier, subjectIdentifier
                )
            )
        }
    }
}
