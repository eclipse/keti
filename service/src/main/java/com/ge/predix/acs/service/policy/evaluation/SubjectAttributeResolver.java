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
 *******************************************************************************/

package com.ge.predix.acs.service.policy.evaluation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ge.predix.acs.attribute.readers.SubjectAttributeReader;
import com.ge.predix.acs.model.Attribute;

public class SubjectAttributeResolver {

    private final Map<String, Set<Attribute>> subjectAttributeMap = new HashMap<>();
    private final SubjectAttributeReader subjectAttributeReader;
    private final String subjectIdentifier;
    private final Set<Attribute> supplementalSubjectAttributes;

    public SubjectAttributeResolver(final SubjectAttributeReader subjectAttributeReader, final String subjectIdentifier,
            final Set<Attribute> supplementalSubjectAttributes) {
        this.subjectAttributeReader = subjectAttributeReader;
        this.subjectIdentifier = subjectIdentifier;
        if (null == supplementalSubjectAttributes) {
            this.supplementalSubjectAttributes = Collections.emptySet();
        } else {
            this.supplementalSubjectAttributes = supplementalSubjectAttributes;
        }
    }

    public Set<Attribute> getResult(final Set<Attribute> scopes) {
        Set<Attribute> subjectAttributes = this.subjectAttributeMap.get(this.subjectIdentifier);
        if (null == subjectAttributes) {
            subjectAttributes = new HashSet<>(
                    this.subjectAttributeReader.getAttributesByScope(this.subjectIdentifier, scopes));
            subjectAttributes.addAll(this.supplementalSubjectAttributes);
            this.subjectAttributeMap.put(this.subjectIdentifier, subjectAttributes);
        }
        return subjectAttributes;
    }

}
