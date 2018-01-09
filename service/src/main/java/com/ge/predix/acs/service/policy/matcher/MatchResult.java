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

package com.ge.predix.acs.service.policy.matcher;

import java.util.List;
import java.util.Set;

import com.ge.predix.acs.service.policy.evaluation.MatchedPolicy;

public class MatchResult {

    private final List<MatchedPolicy> matchedPolicies;
    private final Set<String> resolvedResourceUris;

    public MatchResult(final List<MatchedPolicy> matchedPolicies, final Set<String> resolvedResourceUris) {
        this.matchedPolicies = matchedPolicies;
        this.resolvedResourceUris = resolvedResourceUris;
    }

    public List<MatchedPolicy> getMatchedPolicies() {
        return this.matchedPolicies;
    }

    public Set<String> getResolvedResourceUris() {
        return this.resolvedResourceUris;
    }
}
