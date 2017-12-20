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

package com.ge.predix.acs.commons.policy.condition.groovy;

import java.util.HashSet;
import java.util.Set;

import com.ge.predix.acs.commons.policy.condition.ResourceHandler;
import com.ge.predix.acs.commons.policy.condition.SubjectHandler;

/**
 * Support utility match methods.
 *
 * @author acs-engineers@ge.com
 */
@SuppressWarnings("javadoc")
public class AttributeMatcher {

    private ResourceHandler resourceHandler;
    private SubjectHandler subjectHandler;

    public ResourceHandler resource() {
        return this.resourceHandler;
    }

    public void setResourceHandler(final ResourceHandler resourceHandler) {
        this.resourceHandler = resourceHandler;
    }

    public SubjectHandler subject() {
        return this.subjectHandler;
    }

    public void setSubjectHandler(final SubjectHandler subjectHandler) {
        this.subjectHandler = subjectHandler;
    }

    /**
     * @return true if the intersection of sourceAttributes and targetAttributes is non-empty
     */
    public boolean any(final Set<String> sourceAttributes, final Set<String> targetAttributes) {
        if (null == sourceAttributes || null == targetAttributes) {
            return false;
        }

        // copy source set
        Set<String> intersection = new HashSet<>(sourceAttributes);
        // create intersection of source and target
        intersection.retainAll(targetAttributes);

        return !intersection.isEmpty();
    }

    /**
     * Returns true if attribute value is present in the source attribute values.
     *
     * @param sourceAttributes
     * @param attributeValue
     * @return
     */
    public boolean single(final Set<String> sourceAttributes, final String attributeValue) {
        boolean match = false;
        if (sourceAttributes != null && !sourceAttributes.isEmpty()) {
            match = sourceAttributes.contains(attributeValue);
        }
        return match;
    }

}
