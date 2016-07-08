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

import java.util.List;
import java.util.Set;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;

/**
 * CRUD interface operations for privilege management.
 *
 * @author 212319607
 */
public interface PrivilegeManagementService {

    void appendResources(List<BaseResource> resources);

    List<BaseResource> getResources();

    BaseResource getByResourceIdentifier(String resourceIdentifier);

    BaseResource getByResourceIdentifierWithInheritedAttributes(String resourceIdentifier);

    boolean upsertResource(BaseResource resource);

    boolean deleteResource(String resourceIdentifier);

    void appendSubjects(List<BaseSubject> subjects);

    List<BaseSubject> getSubjects();

    BaseSubject getBySubjectIdentifier(String subjectIdentifier);

    BaseSubject getBySubjectIdentifierWithInheritedAttributes(String subjectIdentifier);

    BaseSubject getBySubjectIdentifierAndScopes(final String subjectIdentifier, final Set<Attribute> scopes);

    boolean upsertSubject(BaseSubject subject);

    boolean deleteSubject(String subjectIdentifier);

}
