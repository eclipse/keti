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

package org.eclipse.keti.acs.service.policy.admin;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public final class PolicyHttpMethodsFilterTest {

    @InjectMocks
    private PolicyManagementController policyManagementController;

    private static final Set<HttpMethod> ALL_HTTP_METHODS = new HashSet<>(Arrays.asList(HttpMethod.values()));

    private MockMvc mockMvc;

    @BeforeClass
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(this.policyManagementController)
                .addFilters(new PolicyHttpMethodsFilter()).build();
    }

    @Test(dataProvider = "urisAndTheirAllowedHttpMethods")
    public void testUriPatternsAndTheirAllowedHttpMethods(final String uri, final Set<HttpMethod> allowedHttpMethods)
            throws Exception {
        Set<HttpMethod> disallowedHttpMethods = new HashSet<>(ALL_HTTP_METHODS);
        disallowedHttpMethods.removeAll(allowedHttpMethods);
        for (HttpMethod disallowedHttpMethod : disallowedHttpMethods) {
            this.mockMvc.perform(MockMvcRequestBuilders.request(disallowedHttpMethod, URI.create(uri)))
                    .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed());
        }
    }

    @DataProvider
    public Object[][] urisAndTheirAllowedHttpMethods() {
        return new Object[][] {
                new Object[] { "/v1/policy-set/foo",
                        new HashSet<>(Arrays.asList(HttpMethod.PUT, HttpMethod.GET, HttpMethod.DELETE, HttpMethod.HEAD,
                                HttpMethod.OPTIONS)) },
                { "/v1/policy-set",
                        new HashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS)) } };
    }
}
