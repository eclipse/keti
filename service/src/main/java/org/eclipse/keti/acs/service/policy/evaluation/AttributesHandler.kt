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

package org.eclipse.keti.acs.service.policy.evaluation

import org.eclipse.keti.acs.model.Attribute
import java.util.HashMap

/**
 * @author acs-engineers@ge.com
 */
class AttributesHandler
/**
 * Default constructor.
 *
 * @param attributes
 * attribute list
 */
    (attributes: List<Attribute>) {

    private val attributeMap = HashMap<String, String>()

    init {
        for (attributeValue in attributes) {
            this.attributeMap[getKey(attributeValue.issuer!!, attributeValue.name!!)] = attributeValue.value!!
        }
    }

    /**
     * @param issuer
     * @param name
     * @return
     */
    private fun getKey(
        issuer: String,
        name: String
    ): String {
        return issuer + name
    }

    /**
     * Get an attribute value.
     *
     * @param issuer
     * attribute issuer
     * @param name
     * attribute name
     * @return attribute value
     */
    fun attributes(
        issuer: String,
        name: String
    ): String? {
        return this.attributeMap[getKey(issuer, name)]
    }

    /**
     * @param issuer
     * claim issuer
     * @param name
     * claim name
     * @param value
     * claim value
     * @return true if has claim
     */
    fun hasAttribute(
        issuer: String,
        name: String,
        value: String
    ): Boolean {
        return this.attributeMap.containsKey(getKey(issuer, name))
    }
}
