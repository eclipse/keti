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

package com.ge.predix.acs.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author 212314537
 */
@SuppressWarnings({ "javadoc", "nls" })
@ApiModel(
        description = "A boolean groovy expression which determines whether the policy effect applies to the"
                + " access control request.")
public class Condition {
    private String name;
    private String condition;

    public Condition() {
        // Default constructor.
    }

    public Condition(final String condition) {
        this.condition = condition;
    }

    public Condition(final String name, final String condition) {
        this.condition = condition;
        this.name = name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the condition
     */
    @ApiModelProperty(required = true)
    public String getCondition() {
        return this.condition;
    }

    /**
     * @param condition
     *            the condition to set
     */
    public void setCondition(final String condition) {
        this.condition = condition;
    }

    @Override
    public String toString() {
        return "Condition [name=" + this.name + ", condition=" + this.condition + "]";
    }

}
