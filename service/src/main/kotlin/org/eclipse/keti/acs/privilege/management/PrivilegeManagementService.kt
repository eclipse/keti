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

import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject

/**
 * CRUD interface operations for privilege management.
 *
 * @author acs-engineers@ge.com
 */
interface PrivilegeManagementService {

    val resources: List<BaseResource>

    val subjects: List<BaseSubject>

    fun appendResources(resources: List<BaseResource>?)

    fun getByResourceIdentifier(resourceIdentifier: String): BaseResource?

    fun getByResourceIdentifierWithInheritedAttributes(resourceIdentifier: String): BaseResource?

    fun upsertResource(resource: BaseResource?): Boolean

    fun deleteResource(resourceIdentifier: String): Boolean

    fun appendSubjects(subjects: List<BaseSubject>?)

    fun getBySubjectIdentifier(subjectIdentifier: String): BaseSubject?

    fun getBySubjectIdentifierWithInheritedAttributes(subjectIdentifier: String): BaseSubject?

    fun getBySubjectIdentifierAndScopes(subjectIdentifier: String, scopes: Set<Attribute>?): BaseSubject?

    fun upsertSubject(subject: BaseSubject?): Boolean

    fun deleteSubject(subjectIdentifier: String): Boolean
}
