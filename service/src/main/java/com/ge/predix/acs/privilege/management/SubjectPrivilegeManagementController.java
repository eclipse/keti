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

import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.SUBJECTS_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.SUBJECT_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.V1;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.created;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.noContent;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.notFound;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.ok;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.net.URI;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ge.predix.acs.commons.web.BaseRestApi;
import com.ge.predix.acs.commons.web.RestApiException;
import com.ge.predix.acs.commons.web.UriTemplateUtils;
import com.ge.predix.acs.rest.BaseSubject;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
public class SubjectPrivilegeManagementController extends BaseRestApi {
    @Autowired
    private PrivilegeManagementService service;

    @ApiOperation(
            value = "Creates a list of subjects. Existing subjects will be updated with the provided values.",
            tags = { "Attribute Management" })
    @ApiResponses(value = { @ApiResponse(code = 204, message = "Subject objects added successfully."), })
    @RequestMapping(method = POST, value = { V1 + SUBJECTS_URL }, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> appendsubjects(@RequestBody final List<BaseSubject> subjects) {
        try {
            this.service.appendSubjects(subjects);
            return noContent();
        } catch (Exception e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e);
        }
    }

    @ApiOperation(value = "Retrieves the list of subjects for the given zone.", tags = { "Attribute Management" })
    @RequestMapping(method = GET, value = { V1 + SUBJECTS_URL }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BaseSubject>> getSubjects() {
        List<BaseSubject> subjects = this.service.getSubjects();
        return ok(subjects);
    }

    @ApiOperation(
            value = "Retrieves the subject for the given zone. The subjectIdentifier must be URL encoded in "
                    + "application/x-www-form-urlencoded format with UTF-8.",
            tags = { "Attribute Management" })
    @RequestMapping(method = GET, value = { V1 + SUBJECT_URL }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getSubject(@PathVariable("subjectIdentifier") final String subjectIdentifier,
            @RequestParam(
                    name = "includeInheritedAttributes",
                    defaultValue = "false") final boolean includeInheritedAttributes) {
        BaseSubject subject;
        if (includeInheritedAttributes) {
            subject = this.service.getBySubjectIdentifierWithInheritedAttributes(subjectIdentifier);
        } else {
            subject = this.service.getBySubjectIdentifier(subjectIdentifier);
        }

        if (subject == null) {
            return notFound();
        }
        return ok(subject);
    }

    @ApiOperation(value = "Creates/Updates a given Subject.", tags = { "Attribute Management" })
    @RequestMapping(method = PUT, value = V1 + SUBJECT_URL, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseSubject> putSubject(@RequestBody final BaseSubject subject,
            @PathVariable("subjectIdentifier") final String subjectIdentifier) {

        try {
            if (StringUtils.isEmpty(subject.getSubjectIdentifier())) {
                subject.setSubjectIdentifier(subjectIdentifier);
            }

            validSubjectIdentifierOrFail(subject, subjectIdentifier);

            boolean createdSubject = this.service.upsertSubject(subject);

            URI subjectUri = UriTemplateUtils.expand(SUBJECT_URL, "subjectIdentifier:" + subjectIdentifier);

            if (createdSubject) {
                return created(subjectUri.getRawPath(), false);
            }

            return created(subjectUri.getRawPath(), true);
        } catch (Exception e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e);
        }
    }

    @ApiOperation(
            value = "Deletes the subject for a given zone. The subjectIdentifier must be URL encoded in "
                    + "application/x-www-form-urlencoded format with UTF-8.",
            tags = { "Attribute Management" })
    @RequestMapping(method = DELETE, value = { V1 + SUBJECT_URL })
    public ResponseEntity<Void> deleteSubject(@PathVariable("subjectIdentifier") final String subjectIdentifier) {
        this.service.deleteSubject(subjectIdentifier);
        return noContent();
    }

    private void validSubjectIdentifierOrFail(final BaseSubject subject, final String subjectIdentifier) {
        if (!subjectIdentifier.equals(subject.getSubjectIdentifier())) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("Subject identifier = %s, does not match the one provided in URI = %s",
                            subject.getSubjectIdentifier(), subjectIdentifier));
        }
    }
}
