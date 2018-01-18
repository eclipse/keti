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

package org.eclipse.keti.acs.policy.evaluation.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;

@Component
@Profile({ "simple-cache" })
public class InMemoryPolicyEvaluationCache extends AbstractPolicyEvaluationCache {

    private final Map<String, String> evalCache = new ConcurrentReferenceHashMap<String, String>();

    @Override
    void delete(final String key) {
        this.evalCache.remove(key);
    }

    @Override
    void delete(final Collection<String> keys) {
        for (String key : keys) {
            delete(key);
        }
    }

    @Override
    void flushAll() {
        this.evalCache.clear();
    }

    @Override
    Set<String> keys(final String key) {
        return this.evalCache.keySet();
    }

    @Override
    List<String> multiGet(final List<String> keys) {
        List<String> results = new ArrayList<>();
        for (String key : keys) {
            results.add(this.evalCache.get(key));
        }
        return results;
    }

    @Override
    void multiSet(final Map<String, String> map) {
        for (Entry<String, String> entry : map.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }

    @Override
    void set(final String key, final String value) {
        if (isPolicySetChangedKey(key) || isResourceChangedKey(key) || isSubjectChangedKey(key)
                || isPolicyEvalResultKey(key)) {
            this.evalCache.put(key, value);
        } else {
            throw new IllegalArgumentException("Unsupported key format.");
        }
    }

    @Override
    void setIfNotExists(final String key, final String value) {
        if (!this.evalCache.containsKey(key)) {
            this.evalCache.put(key, value);
        }
    }
}
