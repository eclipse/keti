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

package com.ge.predix.acs.service.policy.matcher;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;

/**
 *
 * @author 212314537
 */
public class PolicyMatchCandidate {
    private String action;
    private String resourceURI;
    private String subjectIdentifier;
    private Set<Attribute> supplementalResourceAttributes;
    private Set<Attribute> supplementalSubjectAttributes;

    /**
     * Creates a new criteria object.
     *
     * @param action
     *            action
     * @param resourceURI
     *            resource
     * @param resourceAttributes
     *            attributes for the resource
     * @param subjectIdentifier
     *            identifier for the subject
     * @param supplementalSubjectAttributes
     *            attributes for the subject
     */
    public PolicyMatchCandidate(final String action, final String resourceURI, final String subjectIdentifier,
            final Set<Attribute> supplementalResourceAttributes, final Set<Attribute> supplementalSubjectAttributes) {
        this.action = action;
        this.resourceURI = resourceURI;
        this.subjectIdentifier = subjectIdentifier;
        this.supplementalResourceAttributes = supplementalResourceAttributes;
        this.supplementalSubjectAttributes = supplementalSubjectAttributes;
    }

    /**
     * Creates a new criteria object.
     *
     */
    public PolicyMatchCandidate() {
        // Default constructor.
    }

    /**
     * @return the action
     */
    public String getAction() {
        return this.action;
    }

    /**
     * @param action
     *            the action to set
     */
    public void setAction(final String action) {
        this.action = action;
    }

    /**
     * @return the resourceURI
     */
    public String getResourceURI() {
        return this.resourceURI;
    }

    /**
     * @param resourceURI
     *            the resourceURI to set
     */
    public void setResourceURI(final String resourceURI) {
        this.resourceURI = resourceURI;
    }

    public Set<Attribute> getSupplementalResourceAttributes() {
        return this.supplementalResourceAttributes;
    }

    public void setSupplementalResourceAttributes(final Set<Attribute> supplementalResourceAttributes) {
        this.supplementalResourceAttributes = supplementalResourceAttributes;
    }

    public Set<Attribute> getSupplementalSubjectAttributes() {
        return this.supplementalSubjectAttributes;
    }

    public void setSupplementalSubjectAttributes(final Set<Attribute> supplementalSubjectAttributes) {
        this.supplementalSubjectAttributes = supplementalSubjectAttributes;
    }

    /**
     *
     * @return subjectIdentifier
     */
    public String getSubjectIdentifier() {
        return this.subjectIdentifier;
    }

    /**
     * Sets the subject identifier.
     *
     * @param subjectIdentifier
     */
    public void setSubjectIdentifier(final String subjectIdentifier) {
        this.subjectIdentifier = subjectIdentifier;
    }
}
