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

package com.ge.predix.acs.model;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @author acs-engineers@ge.com
 */
@ApiModel(description = "A collection of access control policies evaluated in order. The first applicable policy"
        + " determines the access control effect.")
@SuppressWarnings("javadoc")
public class PolicySet {
    private List<Policy> policies = new ArrayList<>();
    private String name;

    public PolicySet() {
        // required for jackson serialization
    }

    public PolicySet(final String name) {
        this.name = name;
    }

    /**
     * @return All policies in this policy set.
     */
    @ApiModelProperty(value = "A non empty list of Policies that define the Policy set",
            required = true)
    public List<Policy> getPolicies() {
        return this.policies;
    }

    /**
     * @param policies the policies to set
     */
    public void setPolicies(final List<Policy> policies) {
        this.policies = policies;
    }

    @ApiModelProperty(value = "User defined name for the Policy set",
            required = false)
    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        return "PolicySet [policies=" + this.policies + ", name=" + this.name + "]";
    }

}
