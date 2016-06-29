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
package com.ge.predix.acs.service.policy.evaluation;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Policy;

/**
 * Holding class for a matched policy and its resource attributes, based on any attributeUriTemplate in policy target.
 */
public class MatchedPolicy {

    private Policy policy;
    private Set<Attribute> resourceAttributes;
    private Set<Attribute> subjectAttributes;

    public MatchedPolicy(final Policy policy, final Set<Attribute> resourceAttributes) {
        this.policy = policy;
        this.resourceAttributes = resourceAttributes;
    }

    public MatchedPolicy(final Policy policy, final Set<Attribute> resourceAttributes,
            final Set<Attribute> subjectAttributes) {
        this.policy = policy;
        this.resourceAttributes = resourceAttributes;
        this.subjectAttributes = subjectAttributes;
    }

    public Policy getPolicy() {
        return this.policy;
    }

    public void setPolicy(final Policy policy) {
        this.policy = policy;
    }

    public Set<Attribute> getResourceAttributes() {
        return this.resourceAttributes;
    }

    public void setResourceAttributes(final Set<Attribute> resourceAttributes) {
        this.resourceAttributes = resourceAttributes;
    }

    public Set<Attribute> getSubjectAttributes() {
        return this.subjectAttributes;
    }

    public void setSubjectAttributes(final Set<Attribute> subjectAttributes) {
        this.subjectAttributes = subjectAttributes;
    }

    @Override
    public String toString() {
        return "MatchedPolicy [policy=" + this.policy + ", resourceAttributes=" + this.resourceAttributes
                + ", subjectAttributes=" + this.subjectAttributes + "]";
    }
}
