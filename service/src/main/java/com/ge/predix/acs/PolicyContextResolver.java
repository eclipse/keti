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

package com.ge.predix.acs;

/**
 * Allows us to encapsulate the way we retrieve the origin, application id from the security context, as well as some
 * convenient behavior like failing when certain properties are not available.
 *
 * @author acs-engineers@ge.com
 */
public interface PolicyContextResolver {

    /**
     * @return The issuer or idp id of the authenticated subject from the Security Context or throws a
     *         InvalidACSRequestScopeException if not available
     */
    String getIssuerIdOrFail();

    /**
     * @return The client id of the authenticated subject from the Security Context or throws a
     *         InvalidACSRequestScopeException if not available
     */
    String getClientIdOrFail();
}
