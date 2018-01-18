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

import org.eclipse.keti.acs.attribute.readers.CachedAttributes;

public interface AttributeCache {
    String RESOURCE = "resource";
    String SUBJECT = "subject";

    default void setAttributes(final String identifier, final CachedAttributes value) {
        this.set(identifier, value);
    }

    default CachedAttributes getAttributes(final String identifier) {
        return this.get(identifier);
    }

    void set(String key, CachedAttributes value);

    // get should return null if value is not found in the cache
    CachedAttributes get(String key);

    void flushAll();
}
