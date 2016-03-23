/*******************************************************************************
 * Copyright 2016 General Electric Company.
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
 *******************************************************************************/

package com.ge.predix.acs.policy.evaluation.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({ "simple-cache" })
public class InMemoryPolicyEvaluationCache extends AbstractPolicyEvaluationCache {

    private final Map<String, String> evalCache = Collections.synchronizedMap(new HashMap<String, String>());
    private final Map<String, Set<String>> resourceTranslations = Collections
            .synchronizedMap(new HashMap<String, Set<String>>());
    // For security reasons we can't evict time stamps used to invalidate cached evaluation results.
    private final Map<String, String> timestampCache = Collections.synchronizedMap(new HashMap<String, String>());

    @Override
    void delete(final String key) {
        this.evalCache.remove(key);
        this.timestampCache.remove(key);
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
        this.timestampCache.clear();
    }

    @Override
    Set<String> getResourceTranslations(final String fromKey) {
        Set<String> results = this.resourceTranslations.get(fromKey);
        if (null == results) {
            return Collections.emptySet();
        }
        return results;
    }

    @Override
    Set<String> keys(final String key) {
        return this.evalCache.keySet();
    }

    @Override
    List<String> multiGet(final List<String> keys) {
        List<String> results = new ArrayList<>();
        for (String key : keys) {
            String value = this.timestampCache.get(key);
            if (null == value) {
                value = this.evalCache.get(key);
            }
            results.add(value);
        }
        return results;
    }

    @Override
    List<Object> multiGetResourceTranslations(final List<String> fromKeys) {
        List<Object> result = new ArrayList<>();
        for (String fromKey : fromKeys) {
            Set<String> toKeys = this.resourceTranslations.get(fromKey);
            if (null == toKeys) {
                toKeys = Collections.emptySet();
            }
            result.add(toKeys);
        }
        return result;
    }

    @Override
    void multiSet(final Map<String, String> map) {
        for (Entry<String, String> entry : map.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }

    @Override
    void set(final String key, final String value) {
        if (isPolicySetChangedKey(key) || isResourceChangedKey(key) || isSubjectChangedKey(key)) {
            this.timestampCache.put(key, value);
        } else if (isPolicyEvalResultKey(key)) {
            this.evalCache.put(key, value);
        } else {
            throw new IllegalArgumentException("Unsupported key format.");
        }
    }

    @Override
    void setResourceTranslation(final String fromKey, final String toKey) {
        Set<String> toKeys = this.resourceTranslations.get(fromKey);
        if (null == toKeys) {
            toKeys = new HashSet<>();
        }
        toKeys.add(toKey);
        this.resourceTranslations.put(fromKey, toKeys);
    }

    @Override
    void setResourceTranslations(final Set<String> fromKeys, final String toKey) {
        for (String fromKey : fromKeys) {
            Set<String> toKeysLocal = this.resourceTranslations.get(fromKey);
            if (null == toKeysLocal) {
                toKeysLocal = new HashSet<>();
            }
            toKeysLocal.add(toKey);
            this.resourceTranslations.put(fromKey, toKeysLocal);
        }
    }
}
