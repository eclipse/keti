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
import io.swagger.annotations.ApiParam

/**
 * @author acs-engineers@ge.com
 */
@ApiModel("The resource the access control operation would read, create, delete, or modify. Typically used" + " within a policy definition")
class ResourceType {

    var name: String? = null
    var uriTemplate: String? = null
    var attributes: List<Attribute>? = null

    @get:ApiParam(
        "If this is not specified, ACS uses resourceURI in the evaluation request as the resource URI to lookup " + "resource attributes. This URI template can be used to extract a contiguous subset of resourceURI. " + "For example, /region/us/report/asset/12 in evaluation request can be mapped to a resource /asset/12 by " + "defining attributeUriTemplate as /region/us/report{attribute_uri}. ACS extracts the value of URI " + "Template variable 'attribute_uri' as the resourceURI. "
    )
    var attributeUriTemplate: String? = null
}
