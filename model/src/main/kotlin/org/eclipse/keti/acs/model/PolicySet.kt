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
import java.util.ArrayList

/**
 * @author acs-engineers@ge.com
 */
@ApiModel(description = "A collection of access control policies evaluated in order. The first applicable policy" + " determines the access control effect.")
class PolicySet {

    /**
     * @return All policies in this policy set.
     */
    @get:ApiModelProperty(value = "A non empty list of Policies that define the Policy set", required = true)
    var policies: List<Policy> = ArrayList()

    @get:ApiModelProperty(value = "User defined name for the Policy set", required = false)
    var name: String? = null

    constructor() {
        // required for jackson serialization
    }

    constructor(name: String) {
        this.name = name
    }

    override fun toString(): String {
        return "PolicySet [policies=" + this.policies + ", name=" + this.name + "]"
    }
}
