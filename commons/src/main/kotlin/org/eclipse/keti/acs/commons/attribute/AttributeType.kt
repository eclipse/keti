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

package org.eclipse.keti.acs.commons.attribute

/**
 * @author acs-engineers@ge.com
 */
class AttributeType(
    val issuer: String?,
    val name: String?
) {

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + if (this.issuer == null) 0 else this.issuer.hashCode()
        result = prime * result + if (this.name == null) 0 else this.name.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val that = other as AttributeType?
        if (this.issuer == null) {
            if (that?.issuer != null) {
                return false
            }
        } else if (this.issuer != that?.issuer) {
            return false
        }
        if (this.name == null) {
            if (that?.name != null) {
                return false
            }
        } else if (this.name != that?.name) {
            return false
        }
        return true
    }

    override fun toString(): String {
        return "AttributeType [issuer=" + this.issuer + ", name=" + this.name + "]"
    }
}
