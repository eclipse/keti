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

package com.ge.predix.acs.privilege.management.dao;

import java.util.Set;
import java.util.function.BiPredicate;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.utils.JsonUtils;

public final class AttributePredicate {
    private static final JsonUtils JSON_UTILS = new JsonUtils();

    private AttributePredicate() {
        // Prevents instantiation.
    }

    static BiPredicate<String, Set<Attribute>> elementOf() {
        return new BiPredicate<String, Set<Attribute>>() {
            @Override
            public boolean test(final String t, final Set<Attribute> u) {
                Attribute element = JSON_UTILS.deserialize(t, Attribute.class);
                return u.contains(element);
            }
        };
    }
}
