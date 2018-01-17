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

package org.eclipse.keti.service.policy.matcher;

import java.util.List;

import org.eclipse.keti.model.Policy;
import org.eclipse.keti.service.policy.evaluation.MatchedPolicy;

/**
 * Matches an access control request to a policy.
 *
 * @author acs-engineers@ge.com
 */
public interface PolicyMatcher {
    /**
     * @param candidate
     *            the criteria for a match.
     * @param policies
     *            the list of potential policy matches
     * @return the policies that match the access control request.
     */
    List<MatchedPolicy> match(PolicyMatchCandidate candidate, List<Policy> policies);

    /**
     * @param candidate
     *            the criteria for a match.
     * @param policies
     *            the list of potential policy matches
     * @return the policies that match the access control request and the set of resolved URIs from applying all
     *         attribute URI templates.
     */
    MatchResult matchForResult(PolicyMatchCandidate candidate, List<Policy> policies);
}
