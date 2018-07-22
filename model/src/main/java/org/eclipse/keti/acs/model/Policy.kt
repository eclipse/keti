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
 *
 * @author acs-engineers@ge.com
 */
@ApiModel(description = "An access control policy.")
class Policy {

    var name: String? = null

    @get:ApiModelProperty(required = true)
    var target: Target? = null

    /**
     * @return the conditions
     */
    @get:ApiModelProperty(required = true)
    var conditions = emptyList<Condition>()

    @get:ApiModelProperty(required = true)
    var effect: Effect? = null

    override fun toString(): String {
        return ("Policy [name=" + this.name + ", target=" + this.target + ", conditions=" + this.conditions + ", effect=" + this.effect + "]")
    }

}
