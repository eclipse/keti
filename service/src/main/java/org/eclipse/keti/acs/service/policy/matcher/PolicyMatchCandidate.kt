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

package org.eclipse.keti.acs.service.policy.matcher

import org.eclipse.keti.acs.model.Attribute

/**
 *
 * @author acs-engineers@ge.com
 */
class PolicyMatchCandidate {

    /**
     * @return the action
     */
    var action: String? = null
    /**
     * @return the resourceURI
     */
    var resourceURI: String? = null
    /**
     *
     * @return subjectIdentifier
     */
    /**
     * Sets the subject identifier.
     */
    var subjectIdentifier: String? = null
    var supplementalResourceAttributes: Set<Attribute>? = null
    var supplementalSubjectAttributes: Set<Attribute>? = null

    /**
     * Creates a new criteria object.
     *
     * @param action
     * action
     * @param resourceURI
     * resource
     * @param supplementalResourceAttributes
     * attributes for the resource
     * @param subjectIdentifier
     * identifier for the subject
     * @param supplementalSubjectAttributes
     * attributes for the subject
     */
    constructor(
        action: String,
        resourceURI: String,
        subjectIdentifier: String,
        supplementalResourceAttributes: Set<Attribute>,
        supplementalSubjectAttributes: Set<Attribute>
    ) {
        this.action = action
        this.resourceURI = resourceURI
        this.subjectIdentifier = subjectIdentifier
        this.supplementalResourceAttributes = supplementalResourceAttributes
        this.supplementalSubjectAttributes = supplementalSubjectAttributes
    }

    /**
     * Creates a new criteria object.
     *
     */
    constructor() {
        // Default constructor.
    }
}
