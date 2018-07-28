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

package org.eclipse.keti.acs.service.policy.evaluation

import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Policy

/**
 * Holding class for a matched policy and its resource attributes, based on any attributeUriTemplate in policy target.
 */
class MatchedPolicy {

    var policy: Policy? = null
    var resourceAttributes: Set<Attribute>? = null
    var subjectAttributes: Set<Attribute>? = null

    constructor(
        policy: Policy,
        resourceAttributes: Set<Attribute>
    ) {
        this.policy = policy
        this.resourceAttributes = resourceAttributes
    }

    constructor(
        policy: Policy,
        resourceAttributes: Set<Attribute>,
        subjectAttributes: Set<Attribute>
    ) {
        this.policy = policy
        this.resourceAttributes = resourceAttributes
        this.subjectAttributes = subjectAttributes
    }

    override fun toString(): String {
        return ("MatchedPolicy [policy=" + this.policy + ", resourceAttributes=" + this.resourceAttributes + ", subjectAttributes=" + this.subjectAttributes + "]")
    }
}
