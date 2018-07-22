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

package org.eclipse.keti.acs.rest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Effect

/**
 *
 * @author acs-engineers@ge.com
 */
@ApiModel(description = "Policy evaluation result")
class PolicyEvaluationResult {

    @ApiModelProperty(value = "The effect of the policy evaluation", required = true)
    var effect: Effect

    @ApiModelProperty(value = "The collection of the subject's attributes", required = false)
    var subjectAttributes = emptySet<Attribute>()

    @ApiModelProperty(value = "The collection of the resource's attributes", required = false)
    var resourceAttributes = emptyList<Attribute>()

    @ApiModelProperty(
        value = "The resources that matched the policy evaluation request's resource identifier based on " + "the attribute uri templates defined in the policy set. For example, a policy request of" + "    /v1/site/1/plant/asset/1\nagainst a policy set with attribute uri templates:\n" + "    /v1{attribute_uri}/plant/asset/{asset_id}\n    /v1/site/{site_id}/plant{attribute_uri}\n" + "would include:\n" + "    /site/1\n    /asset/2\n" + "in this set, respectively.",
        required = false
    )
    var resolvedResourceUris = emptySet<String>()

    var timestamp: Long = 0

    var message: String? = null

    constructor() {
        this.effect = Effect.NOT_APPLICABLE
    }

    constructor(decision: Effect) {
        this.effect = decision
    }

    constructor(
        effect: Effect,
        subjectAttributes: Set<Attribute>,
        resourceAttributes: List<Attribute>
    ) {
        this.effect = effect
        this.subjectAttributes = subjectAttributes
        this.resourceAttributes = resourceAttributes
    }

    constructor(
        effect: Effect,
        subjectAttributes: Set<Attribute>,
        resourceAttributes: List<Attribute>,
        resolvedResourceUris: Set<String>
    ) {
        this.effect = effect
        this.subjectAttributes = subjectAttributes
        this.resourceAttributes = resourceAttributes
        this.resolvedResourceUris = resolvedResourceUris
    }

    override fun toString(): String {
        return ("PolicyEvaluationResult [effect=" + this.effect + ", subjectAttributes=" + this.subjectAttributes + ", resourceAttributes=" + this.resourceAttributes + "]")
    }
}
