/*******************************************************************************
 * Copyright 2017 General Electric Company
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

package org.eclipse.keti.acs.attribute.cache;

import java.util.Map;
import java.util.function.BiFunction;

import org.apache.commons.collections4.map.PassiveExpiringMap;

import org.eclipse.keti.acs.attribute.readers.CachedAttributes;

public class InMemoryAttributeCache extends AbstractAttributeCache {

    private String zoneName;
    private BiFunction<String, String, String> getKey;
    private Map<String, CachedAttributes> attributeCache;

    InMemoryAttributeCache(final long maxCachedIntervalMinutes, final String zoneName,
            final BiFunction<String, String, String> getKey) {
        this.zoneName = zoneName;
        this.getKey = getKey;
        this.attributeCache = new PassiveExpiringMap<>(maxCachedIntervalMinutes);
    }

    @Override
    public void set(final String key, final CachedAttributes value) {
        this.attributeCache.put(this.getKey.apply(this.zoneName, key), value);
    }

    @Override
    public CachedAttributes get(final String key) {
        return this.attributeCache.get(this.getKey.apply(this.zoneName, key));

    }

    @Override
    public void flushAll() {
        this.attributeCache.clear();

    }

}
