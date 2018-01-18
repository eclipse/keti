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

package org.eclipse.keti.acs.request.context;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import org.eclipse.keti.acs.request.context.AcsRequestContext.ACSRequestContextAttribute;
import org.eclipse.keti.acs.zone.management.dao.ZoneRepository;
import com.ge.predix.uaa.token.lib.ZoneOAuth2Authentication;

/**
 * A ThreadLocal store for the ACS Request Context.
 *
 * @author acs-engineers@ge.com
 */
@Component
public final class AcsRequestContextHolder implements ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcsRequestContextHolder.class);
    private static final InheritableThreadLocal<AcsRequestContext> ACS_REQUEST_CONTEXT_STORE = new
            InheritableThreadLocal<>();

    // Hide Constructor
    private AcsRequestContextHolder() {
    }

    // Can only be called by the filter...
    // Filter calls it to create the ACSRequestContext for a request
    static void initialize() {
        ACS_REQUEST_CONTEXT_STORE.set(new AcsRequestContextBuilder().zoneEntityOrFail().build());
    }

    // Can only be called by the filter...
    static void clear() {
        ACS_REQUEST_CONTEXT_STORE.remove();
    }

    // Only public interface to be used by the clients to access the AcsRequestContext specific to the
    // Request...
    public static AcsRequestContext getAcsRequestContext() {
        return ACS_REQUEST_CONTEXT_STORE.get();
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        LOGGER.trace("AcsRequestContextHolder Initialized");
        AcsRequestContextBuilder.initAcsRequestContextBuilderRepos(applicationContext);
    }

    /**
     * The Fluent implementation to create the {@link AcsRequestContext} for the current AcsRequest...
     *
     * @author acs-engineers@ge.com
     */
    private static final class AcsRequestContextBuilder {
        private static ZoneRepository zoneRepository;
        private static final Logger LOGGER = LoggerFactory.getLogger(AcsRequestContextBuilder.class);
        private final Map<ACSRequestContextAttribute, Object> requestContextMap;

        AcsRequestContextBuilder() {
            this.requestContextMap = new EnumMap<>(ACSRequestContextAttribute.class);
        }

        static void initAcsRequestContextBuilderRepos(final ApplicationContext applicationContext) {
            zoneRepository = applicationContext.getBean(ZoneRepository.class);
            LOGGER.info("AcsRequestContextBuilder JPA Repositories Initialized");
        }

        AcsRequestContextBuilder zoneEntityOrFail() {
            ZoneOAuth2Authentication zoneAuth = (ZoneOAuth2Authentication) SecurityContextHolder.getContext()
                    .getAuthentication();
            this.requestContextMap
                    .put(ACSRequestContextAttribute.ZONE_ENTITY, zoneRepository.getBySubdomain(zoneAuth.getZoneId()));
            return this;
        }

        AcsRequestContext build() {
            return new AcsRequestContext(Collections.unmodifiableMap(this.requestContextMap));
        }
    }
}
