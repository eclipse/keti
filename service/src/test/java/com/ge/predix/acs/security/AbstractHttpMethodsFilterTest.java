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

package com.ge.predix.acs.security;

import java.net.URI;
import java.util.Collections;

import org.eclipse.jetty.http.MimeTypes;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.commons.web.ResponseEntityBuilder;

public final class AbstractHttpMethodsFilterTest {

    private static final String V1_DUMMY = "/v1/dummy";

    private static final class DummyHttpMethodsFilter extends AbstractHttpMethodsFilter {

        DummyHttpMethodsFilter() {
            super(Collections.singletonMap("\\A" + V1_DUMMY + "/??\\Z", Collections.singleton(HttpMethod.GET)));
        }
    }

    @RestController
    private static final class DummyController {

        @RequestMapping(method = RequestMethod.GET, value = V1_DUMMY)
        public ResponseEntity<String> getDummy() {
            return ResponseEntityBuilder.ok();
        }
    }

    @InjectMocks
    private DummyController dummyController;

    private MockMvc mockMvc;

    @BeforeClass
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc =
                MockMvcBuilders.standaloneSetup(this.dummyController).addFilters(new DummyHttpMethodsFilter()).build();
    }

    @Test
    public void testWithNoAcceptHeaderInRequest() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.request(HttpMethod.GET, URI.create(V1_DUMMY)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test(dataProvider = "mediaTypesAndExpectedStatuses")
    public void testUnacceptableMediaTypes(final String mediaType, final ResultMatcher resultMatcher) throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.request(HttpMethod.GET, URI.create(V1_DUMMY))
                .header(HttpHeaders.ACCEPT, mediaType)).andExpect(resultMatcher);
    }

    @DataProvider
    public Object[][] mediaTypesAndExpectedStatuses() {
        return new Object[][] { new Object[] { MediaType.ALL_VALUE, MockMvcResultMatchers.status().isOk() },
                { MediaType.APPLICATION_JSON_VALUE, MockMvcResultMatchers.status().isOk() },
                { MimeTypes.Type.APPLICATION_JSON_UTF_8.toString(), MockMvcResultMatchers.status().isOk() },
                { MediaType.APPLICATION_JSON_VALUE + ", application/*+json", MockMvcResultMatchers.status().isOk() },
                { MediaType.TEXT_PLAIN_VALUE, MockMvcResultMatchers.status().isOk() },
                { MimeTypes.Type.TEXT_PLAIN_UTF_8.toString(), MockMvcResultMatchers.status().isOk() },
                { "text/*+plain, " + MediaType.TEXT_PLAIN_VALUE, MockMvcResultMatchers.status().isOk() },
                { "fake/type, " + MediaType.TEXT_PLAIN_VALUE, MockMvcResultMatchers.status().isOk() },
                { "fake/type", MockMvcResultMatchers.status().isNotAcceptable() } };
    }
}