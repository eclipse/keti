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

import java.util.Collections;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author acs-engineers@ge.com
 */
@ApiModel(description = "An access control policy.")
@SuppressWarnings("javadoc")
public class Policy {
    private String name;
    private Target target;
    private List<Condition> conditions = Collections.emptyList();
    private Effect effect;

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @ApiModelProperty(required = true)
    public Target getTarget() {
        return this.target;
    }

    public void setTarget(final Target target) {
        this.target = target;
    }

    /**
     * @return the conditions
     */
    @ApiModelProperty(required = true)
    public List<Condition> getConditions() {
        return this.conditions;
    }

    /**
     * @param conditions
     *            the conditions to set
     */
    public void setConditions(final List<Condition> conditions) {
        this.conditions = conditions;
    }

    @ApiModelProperty(required = true)
    public Effect getEffect() {
        return this.effect;
    }

    public void setEffect(final Effect effect) {
        this.effect = effect;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        return "Policy [name=" + this.name + ", target=" + this.target + ", conditions=" + this.conditions + ", effect="
                + this.effect + "]";
    }

}
