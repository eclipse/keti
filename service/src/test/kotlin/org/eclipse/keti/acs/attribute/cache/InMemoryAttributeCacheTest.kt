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

import org.eclipse.keti.acs.attribute.readers.CachedAttributes
import org.eclipse.keti.acs.model.Attribute
import org.springframework.test.util.ReflectionTestUtils
import org.testng.Assert
import org.testng.annotations.Test
import java.util.HashMap

private const val TEST_ZONE = "test-zone"
private const val TEST_KEY = "test-key"

class InMemoryAttributeCacheTest {

    @Test
    fun testSetCachedAttributes() {
        val inMemoryAttributeCache = InMemoryAttributeCache(5, TEST_ZONE) { zoneId: String, identifier: String ->
            resourceKey(zoneId, identifier)
        }
        val attributes = setOf(Attribute("https://test.com", "attribute1", "value1"))
        val cachedAttributes = CachedAttributes(attributes)
        inMemoryAttributeCache[TEST_KEY] = cachedAttributes

        val resourceCache = ReflectionTestUtils
            .getField(inMemoryAttributeCache, "attributeCache") as Map<String, CachedAttributes>
        val cacheKey = resourceKey(TEST_ZONE, TEST_KEY)
        Assert.assertEquals(resourceCache[cacheKey], cachedAttributes)
    }

    @Test
    fun testGetCachedAttributes() {
        val inMemoryAttributeCache = InMemoryAttributeCache(5, TEST_ZONE) { zoneId: String, identifier: String ->
            resourceKey(zoneId, identifier)
        }
        val attributes = setOf(Attribute("https://test.com", "attribute1", "value1"))
        val cachedAttributes = CachedAttributes(attributes)

        val cachedAttributesMap = HashMap<String, CachedAttributes>()
        val cacheKey = resourceKey(TEST_ZONE, TEST_KEY)
        cachedAttributesMap[cacheKey] = cachedAttributes
        ReflectionTestUtils.setField(inMemoryAttributeCache, "attributeCache", cachedAttributesMap)

        Assert.assertEquals(inMemoryAttributeCache[TEST_KEY], cachedAttributes)
    }

    @Test
    fun testFlushAll() {
        val inMemoryAttributeCache = InMemoryAttributeCache(5, TEST_ZONE) { zoneId: String, identifier: String ->
            subjectKey(zoneId, identifier)
        }
        val attributes = setOf(Attribute("https://test.com", "attribute1", "value1"))
        val cachedAttributes = CachedAttributes(attributes)

        val cachedAttributesMap = HashMap<String, CachedAttributes>()
        val cacheKey = subjectKey(TEST_ZONE, TEST_KEY)
        cachedAttributesMap[cacheKey] = cachedAttributes
        ReflectionTestUtils.setField(inMemoryAttributeCache, "attributeCache", cachedAttributesMap)
        Assert.assertEquals(inMemoryAttributeCache[TEST_KEY], cachedAttributes)
        inMemoryAttributeCache.flushAll()
        Assert.assertTrue(
            (ReflectionTestUtils.getField(inMemoryAttributeCache, "attributeCache") as Map<String, CachedAttributes>)
                .isEmpty()
        )
    }
}
