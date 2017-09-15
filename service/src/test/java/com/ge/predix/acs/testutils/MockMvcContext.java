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
 *******************************************************************************/

package com.ge.predix.acs.testutils;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class MockMvcContext {
    private MockMvc mockMvc;
    private MockHttpServletRequestBuilder builder;

    public MockHttpServletRequestBuilder getBuilder() {
        return this.builder;
    }

    public void setBuilder(final MockHttpServletRequestBuilder builder) {
        this.builder = builder;
    }

    public MockMvc getMockMvc() {
        return this.mockMvc;
    }

    public void setMockMvc(final MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }
}
