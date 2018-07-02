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

package org.eclipse.keti.acs.request.context

import com.ge.predix.uaa.token.lib.ZoneOAuth2Authentication
import org.eclipse.keti.acs.request.context.AcsRequestContext.ACSRequestContextAttribute
import org.eclipse.keti.acs.request.context.AcsRequestContextHolder.AcsRequestContextBuilder
import org.eclipse.keti.acs.zone.management.dao.ZoneRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.EnumMap

private val LOGGER = LoggerFactory.getLogger(AcsRequestContextHolder::class.java)
private val ACS_REQUEST_CONTEXT_STORE = InheritableThreadLocal<AcsRequestContext>()

// Can only be called by the filter...
// Filter calls it to create the ACSRequestContext for a request
internal fun initialize() {
    ACS_REQUEST_CONTEXT_STORE.set(AcsRequestContextBuilder().zoneEntityOrFail().build())
}

// Can only be called by the filter...
fun clear() {
    ACS_REQUEST_CONTEXT_STORE.remove()
}

// Only public interface to be used by the clients to access the AcsRequestContext specific to the
// Request...
val acsRequestContext: AcsRequestContext?
    get() = ACS_REQUEST_CONTEXT_STORE.get()

/**
 * A ThreadLocal store for the ACS Request Context.
 *
 * @author acs-engineers@ge.com
 */
@Component
class AcsRequestContextHolder// Hide Constructor
private constructor() : ApplicationContextAware {

    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        LOGGER.trace("AcsRequestContextHolder Initialized")
        AcsRequestContextBuilder.initAcsRequestContextBuilderRepos(applicationContext)
    }

    /**
     * The Fluent implementation to create the [AcsRequestContext] for the current AcsRequest...
     *
     * @author acs-engineers@ge.com
     */
    internal class AcsRequestContextBuilder internal constructor() {

        private val requestContextMap: MutableMap<ACSRequestContextAttribute, Any?>

        init {
            this.requestContextMap = EnumMap(ACSRequestContextAttribute::class.java)
        }

        internal fun zoneEntityOrFail(): AcsRequestContextBuilder {
            val zoneAuth = SecurityContextHolder.getContext().authentication as ZoneOAuth2Authentication
            this.requestContextMap[ACSRequestContextAttribute.ZONE_ENTITY] =
                zoneRepository!!.getBySubdomain(zoneAuth.zoneId)
            return this
        }

        internal fun build(): AcsRequestContext {
            return AcsRequestContext(this.requestContextMap.toMap())
        }

        companion object {
            private var zoneRepository: ZoneRepository? = null
            private val LOGGER = LoggerFactory.getLogger(AcsRequestContextBuilder::class.java)

            internal fun initAcsRequestContextBuilderRepos(applicationContext: ApplicationContext) {
                zoneRepository = applicationContext.getBean(ZoneRepository::class.java)
                LOGGER.info("AcsRequestContextBuilder JPA Repositories Initialized")
            }
        }
    }
}
