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

package org.eclipse.keti.acs.attribute.cache

import org.apache.commons.collections4.map.PassiveExpiringMap
import org.eclipse.keti.acs.attribute.readers.CachedAttributes

class InMemoryAttributeCache internal constructor(
    maxCachedIntervalMinutes: Long,
    private val zoneName: String,
    private val getKey: (String, String) -> String
) : AbstractAttributeCache {

    private val attributeCache: MutableMap<String, CachedAttributes>

    init {
        this.attributeCache = PassiveExpiringMap(maxCachedIntervalMinutes)
    }

    override fun set(
        key: String,
        value: CachedAttributes
    ) {
        this.attributeCache[this.getKey(this.zoneName, key)] = value
    }

    override fun get(key: String): CachedAttributes? {
        return this.attributeCache[this.getKey(this.zoneName, key)]
    }

    override fun flushAll() {
        this.attributeCache.clear()
    }
}
