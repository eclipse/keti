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

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;

import org.eclipse.keti.acs.rest.Zone;
import org.eclipse.keti.acs.zone.management.ZoneService;
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity;

/**
 * @author acs-engineers@ge.com
 */
public class TestUtils {

    // to set the a field of object for testing
    public void setField(final Object target, final String name, final Object value) {

        // check if the object is a proxy object
        if (AopUtils.isAopProxy(target) && target instanceof Advised) {
            try {
                ReflectionTestUtils.setField(((Advised) target).getTargetSource().getTarget(), name, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ReflectionTestUtils.setField(target, name, value);
        }
    }

    public Zone createZone(final String name, final String subdomain) {
        Zone zone = new Zone();
        zone.setName(name);
        zone.setSubdomain(subdomain);
        return zone;
    }

    /**
     * @returns a zone entity for a name 'Default acs Zone' and subdomain 'default_subdomain'
     */
    public ZoneEntity createDefaultZoneEntity() {
        ZoneEntity zoneEntity = new ZoneEntity();
        zoneEntity.setDescription("default zoneEnity for test");
        zoneEntity.setName("Default Acs Zone");
        zoneEntity.setSubdomain("default-subdomain");
        return zoneEntity;
    }

    public Zone createTestZone(final String testName) {
        return new Zone(testName + ".zone", testName + "-subdomain", "");
    }

    public Zone setupTestZone(final String testName, final ZoneService zoneService) {
        Zone testZone = createTestZone(testName);
        zoneService.upsertZone(testZone);
        MockSecurityContext.mockSecurityContext(testZone);
        MockAcsRequestContext.mockAcsRequestContext();
        return testZone;
    }

    public MockMvcContext createWACWithCustomGETRequestBuilder(final WebApplicationContext wac, final String subdomain,
            final String resourceURI) throws URISyntaxException {
        MockMvcContext result = new MockMvcContext();
        result.setBuilder(MockMvcRequestBuilders.get(new URI("http://" + subdomain + ".localhost/" + resourceURI))
                .accept(MediaType.APPLICATION_JSON));
        result.setMockMvc(MockMvcBuilders.webAppContextSetup(wac).defaultRequest(result.getBuilder()).build());
        return result;
    }

    public MockMvcContext createWACWithCustomDELETERequestBuilder(final WebApplicationContext wac,
            final String subdomain, final String resourceURI) throws URISyntaxException {
        MockMvcContext result = new MockMvcContext();
        result.setBuilder(MockMvcRequestBuilders.delete(new URI("http://" + subdomain + ".localhost/" + resourceURI)));
        result.setMockMvc(MockMvcBuilders.webAppContextSetup(wac).defaultRequest(result.getBuilder()).build());
        return result;
    }

    public MockMvcContext createWACWithCustomPUTRequestBuilder(final WebApplicationContext wac, final String subdomain,
            final String resourceURI) throws URISyntaxException {
        MockMvcContext result = new MockMvcContext();
        result.setBuilder(MockMvcRequestBuilders.put(new URI("http://" + subdomain + ".localhost/" + resourceURI)));
        result.setMockMvc(MockMvcBuilders.webAppContextSetup(wac).defaultRequest(result.getBuilder()).build());
        return result;
    }

    public MockMvcContext createWACWithCustomPOSTRequestBuilder(final WebApplicationContext wac, final String subdomain,
            final String resourceURI) throws URISyntaxException {
        MockMvcContext result = new MockMvcContext();
        result.setBuilder(MockMvcRequestBuilders.post(new URI("http://" + subdomain + ".localhost/" + resourceURI)));
        result.setMockMvc(MockMvcBuilders.webAppContextSetup(wac).defaultRequest(result.getBuilder()).build());
        return result;
    }

    public static class TestAssertion {
        public static void assertDoesNotThrow(final Class<?> unexpectedException, final Runnable executableCode) {

            /*
            TODO We should Start moving tests to use this assertDoesNotThrow method wherever we use the try { ...
            } catch (Exception e) { Assert.fail(); }

            */
            try {
                executableCode.run();
            } catch (Exception e) {
                if (e.getClass() == unexpectedException) {
                    Assert.fail();
                }
                throw e;
            }
        }
    }
}
