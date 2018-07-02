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

package org.eclipse.keti.acs.zone.resolver

import com.ge.predix.uaa.token.lib.ZoneOAuth2Authentication
import org.eclipse.keti.acs.commons.web.ZoneDoesNotExistException
import org.eclipse.keti.acs.request.context.AcsRequestContext.ACSRequestContextAttribute
import org.eclipse.keti.acs.request.context.acsRequestContext
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

// This could happen if a zone was removed from ACS but a registration still exists in ZAC
val zoneEntity: ZoneEntity
    get() {
        val result = acsRequestContext?.get(ACSRequestContextAttribute.ZONE_ENTITY) as? ZoneEntity
        if (null == result) {
            val zoneAuth = SecurityContextHolder.getContext()
                .authentication as ZoneOAuth2Authentication
            throw ZoneDoesNotExistException("The zone '" + zoneAuth.zoneId + "' does not exist.")
        }

        return result
    }

val zoneName: String
    get() = zoneEntity.name

@Component
class SpringSecurityZoneResolver : ZoneResolver {

    override val zoneEntityOrFail: ZoneEntity
        get() = zoneEntity
}
