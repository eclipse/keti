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

package org.eclipse.keti.acs.service.policy.evaluation

import org.eclipse.keti.acs.attribute.readers.SubjectAttributeReader
import org.eclipse.keti.acs.model.Attribute
import java.util.HashMap
import java.util.HashSet

class SubjectAttributeResolver(
    private val subjectAttributeReader: SubjectAttributeReader,
    private val subjectIdentifier: String,
    supplementalSubjectAttributes: Set<Attribute>?
) {

    private val subjectAttributeMap = HashMap<String, Set<Attribute>>()
    private val supplementalSubjectAttributes: Set<Attribute>

    init {
        if (null == supplementalSubjectAttributes) {
            this.supplementalSubjectAttributes = emptySet()
        } else {
            this.supplementalSubjectAttributes = supplementalSubjectAttributes
        }
    }

    fun getResult(scopes: Set<Attribute>?): Set<Attribute> {
        var subjectAttributes: MutableSet<Attribute>? = this.subjectAttributeMap[this.subjectIdentifier]?.toMutableSet()
        if (null == subjectAttributes) {
            subjectAttributes = HashSet(
                this.subjectAttributeReader.getAttributesByScope(this.subjectIdentifier, scopes)!!
            )
            subjectAttributes.addAll(this.supplementalSubjectAttributes)
            this.subjectAttributeMap[this.subjectIdentifier] = subjectAttributes
        }
        return subjectAttributes
    }
}
