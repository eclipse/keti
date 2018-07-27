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

package org.eclipse.keti.acs.model

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @author acs-engineers@ge.com
 */
@ApiModel(description = "A boolean groovy expression which determines whether the policy effect applies to the" + " access control request.")
class Condition {

    /**
     * @return the name
     */
    var name: String? = null

    /**
     * @return the condition
     */
    @get:ApiModelProperty(required = true)
    var condition: String? = null

    constructor() {
        // Default constructor.
    }

    constructor(condition: String) {
        this.condition = condition
    }

    constructor(
        name: String,
        condition: String
    ) {
        this.condition = condition
        this.name = name
    }

    override fun toString(): String {
        return "Condition [name=" + this.name + ", condition=" + this.condition + "]"
    }
}
