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

package org.eclipse.keti.privilege.management.dao;

import java.util.Set;

import org.eclipse.keti.model.Attribute;
import org.eclipse.keti.zone.management.dao.ZoneEntity;

public interface SubjectHierarchicalRepository {
    SubjectEntity getSubjectWithInheritedAttributesForScopes(ZoneEntity zone, String subjectIdentifier,
                                                             Set<Attribute> scopes);

    SubjectEntity getSubjectWithInheritedAttributes(ZoneEntity zone, String subjectIdentifier);

    Set<String> getSubjectEntityAndDescendantsIds(SubjectEntity entity); 
}