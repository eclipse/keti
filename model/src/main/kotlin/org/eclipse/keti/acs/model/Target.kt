/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.model

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @author acs-engineers@ge.com
 */
@ApiModel(description = "Determines whether a policy applies to an access control request.")
class Target {

    var name: String? = null

    /**
     * @return the subject
     */
    var subject: SubjectType? = null

    /**
     * @return the action
     */
    @get:ApiModelProperty(
        value = "String containing one or more comma separated HTTP verbs, " + "ex: \"GET\" or \"GET, POST\". If not specified it will match \"GET, POST, PUT, DELETE, PATCH\" ",
        required = false
    )
    var action: String? = null

    var resource: ResourceType? = null

    constructor() : super()

    constructor(
        name: String,
        subject: SubjectType,
        action: String,
        resource: ResourceType
    ) : super() {
        this.name = name
        this.subject = subject
        this.action = action
        this.resource = resource
    }
}
