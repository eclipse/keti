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

package org.eclipse.keti.acs.testutils;

import static org.mockito.Mockito.when;

import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;

import org.eclipse.keti.acs.rest.Zone;
import com.ge.predix.uaa.token.lib.ZoneOAuth2Authentication;

/**
 *
 * @author acs-engineers@ge.com
 */
public final class MockSecurityContext {
    private MockSecurityContext() {
        // Prevents instantiation.
    }

    public static void mockSecurityContext(final Zone zone) {
        ZoneOAuth2Authentication acsZoneOAuth = Mockito.mock(ZoneOAuth2Authentication.class);
        if (zone != null) {
            when(acsZoneOAuth.getZoneId()).thenReturn(zone.getSubdomain());
        }
        SecurityContextHolder.getContext().setAuthentication(acsZoneOAuth);
    }
}
