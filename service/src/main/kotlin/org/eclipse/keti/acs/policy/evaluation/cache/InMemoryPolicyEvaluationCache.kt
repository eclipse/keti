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

package org.eclipse.keti.acs.policy.evaluation.cache

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.util.ConcurrentReferenceHashMap
import java.util.ArrayList

@Component
@Profile("simple-cache")
class InMemoryPolicyEvaluationCache : AbstractPolicyEvaluationCache() {

    private val evalCache = ConcurrentReferenceHashMap<String, String>()

    override fun delete(key: String) {
        this.evalCache.remove(key)
    }

    override fun delete(keys: Collection<String>) {
        for (key in keys) {
            delete(key)
        }
    }

    override fun flushAll() {
        this.evalCache.clear()
    }

    override fun keys(key: String): Set<String> {
        return this.evalCache.keys
    }

    override fun multiGet(keys: List<String>): List<String?> {
        val results = ArrayList<String?>()
        for (key in keys) {
            results.add(this.evalCache[key])
        }
        return results
    }

    override fun multiSet(map: Map<String, String>) {
        for ((key, value) in map) {
            set(key, value)
        }
    }

    override fun set(
        key: String,
        value: String
    ) {
        if (isPolicySetChangedKey(key) ||
            isResourceChangedKey(key) ||
            isSubjectChangedKey(key) ||
            isPolicyEvalResultKey(key)
        ) {
            this.evalCache[key] = value
        } else {
            throw IllegalArgumentException("Unsupported key format.")
        }
    }

    override fun setIfNotExists(
        key: String,
        value: String
    ) {
        if (!this.evalCache.containsKey(key)) {
            this.evalCache[key] = value
        }
    }
}
