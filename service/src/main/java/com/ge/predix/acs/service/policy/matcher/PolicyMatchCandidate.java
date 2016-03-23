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

import java.util.List;

import com.ge.predix.acs.model.Attribute;

/**
 *
 * @author 212314537
 */
public class PolicyMatchCandidate {
    private String action;
    private String resourceURI;
    private String subjectIdentifier;
    private List<Attribute> subjectAttributes;

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
     * @param subjectAttributes
     *            attributes for the subject
     */
    public PolicyMatchCandidate(final String action, final String resourceURI, final String subjectIdentifier,
            final List<Attribute> subjectAttributes) {
        super();
        this.action = action;
        this.resourceURI = resourceURI;
        this.subjectIdentifier = subjectIdentifier;
        this.subjectAttributes = subjectAttributes;
    }

    /**
     * Creates a new criteria object.
     *
     */
    public PolicyMatchCandidate() {
        super();
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

    /**
     * @return the subjectAttributes
     */
    public List<Attribute> getSubjectAttributes() {
        return this.subjectAttributes;
    }

    /**
     * @param subjectAttributes
     *            the subjectAttributes to set
     */
    public void setSubjectAttributes(final List<Attribute> subjectAttributes) {
        this.subjectAttributes = subjectAttributes;
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
