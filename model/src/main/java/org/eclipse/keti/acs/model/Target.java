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

package org.eclipse.keti.acs.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author acs-engineers@ge.com
 */
@ApiModel(description = "Determines whether a policy applies to an access control request.")
@SuppressWarnings("javadoc")
public class Target {

    private String name;
    private SubjectType subject;
    private String action;
    private ResourceType resource;

    public Target() {
        super();
    }

    public Target(final String name, final SubjectType subject, final String action, final ResourceType resource) {
        super();
        this.name = name;
        this.subject = subject;
        this.action = action;
        this.resource = resource;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the subject
     */
    public SubjectType getSubject() {
        return this.subject;
    }

    /**
     * @param subject
     *            the subject to set
     */
    public void setSubject(final SubjectType subject) {
        this.subject = subject;
    }

    /**
     * @return the action
     */
    @ApiModelProperty(
            value = "String containing one or more comma separated HTTP verbs, "
                    + "ex: \"GET\" or \"GET, POST\". If not specified it will match \"GET, POST, PUT, DELETE, PATCH\" ",
            required = false)
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

    public ResourceType getResource() {
        return this.resource;
    }

    public void setResource(final ResourceType resource) {
        this.resource = resource;
    }

}
