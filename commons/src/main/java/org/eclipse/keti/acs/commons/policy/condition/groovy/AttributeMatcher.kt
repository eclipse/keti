/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.commons.policy.condition.groovy

import org.eclipse.keti.acs.commons.policy.condition.ResourceHandler
import org.eclipse.keti.acs.commons.policy.condition.SubjectHandler
import java.util.HashSet

/**
 * Support utility match methods.
 *
 * @author acs-engineers@ge.com
 */
class AttributeMatcher {

    private var resourceHandler: ResourceHandler? = null
    private var subjectHandler: SubjectHandler? = null

    fun resource(): ResourceHandler? {
        return this.resourceHandler
    }

    fun setResourceHandler(resourceHandler: ResourceHandler) {
        this.resourceHandler = resourceHandler
    }

    fun subject(): SubjectHandler? {
        return this.subjectHandler
    }

    fun setSubjectHandler(subjectHandler: SubjectHandler) {
        this.subjectHandler = subjectHandler
    }

    /**
     * @return true if the intersection of sourceAttributes and targetAttributes is non-empty
     */
    fun any(
        sourceAttributes: Set<String>?,
        targetAttributes: Set<String>?
    ): Boolean {
        if (null == sourceAttributes || null == targetAttributes) {
            return false
        }

        // copy source set
        val intersection = HashSet(sourceAttributes)
        // create intersection of source and target
        intersection.retainAll(targetAttributes)

        return !intersection.isEmpty()
    }

    /**
     * Returns true if attribute value is present in the source attribute values.
     *
     * @param sourceAttributes
     * @param attributeValue
     * @return
     */
    fun single(
        sourceAttributes: Set<String>?,
        attributeValue: String
    ): Boolean {
        var match = false
        if (sourceAttributes != null && !sourceAttributes.isEmpty()) {
            match = sourceAttributes.contains(attributeValue)
        }
        return match
    }

}
