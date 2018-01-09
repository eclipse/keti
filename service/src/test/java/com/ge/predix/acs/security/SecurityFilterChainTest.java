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

import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.HEALTH_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.HEARTBEAT_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.MANAGED_RESOURCES_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.POLICY_EVALUATION_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.POLICY_SETS_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.RESOURCE_CONNECTOR_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.SUBJECTS_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.SUBJECT_CONNECTOR_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.V1;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import com.fasterxml.jackson.core.JsonProcessingException;

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
public final class SecurityFilterChainTest extends AbstractTestNGSpringContextTests {

    private static final URI SUBJECT_URI = URI.create(V1 + SUBJECTS_URL + "/test");
    private static final URI SUBJECTS_URI = URI.create(V1 + SUBJECTS_URL);
    private static final URI RESOURCE_URI = URI.create(V1 + MANAGED_RESOURCES_URL + "/test");
    private static final URI RESOURCES_URI = URI.create(V1 + MANAGED_RESOURCES_URL);
    private static final URI POLICY_SET_URI = URI.create(V1 + POLICY_SETS_URL + "/test");
    private static final URI POLICY_SETS_URI = URI.create(V1 + POLICY_SETS_URL);
    private static final URI POLICY_EVAL_URI = URI.create(V1 + POLICY_EVALUATION_URL);
    private static final URI RESOURCE_CONNECTOR_URI = URI.create(V1 + RESOURCE_CONNECTOR_URL);
    private static final URI SUBJECT_CONNECTOR_URI = URI.create(V1 + SUBJECT_CONNECTOR_URL);
    private static final URI ZONE_URI = URI.create(V1 + "/zone/test");
    private static final URI HEALTH_URI = URI.create(HEALTH_URL);

    private static final String CONTENT = "test content";
    private static final String MALFORMED_BEARER_TOKEN = "Bearer foo";

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeClass
    public void setup() {
        // https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#test-mockmvc
        // https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#web-app-security
        // https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#filter-ordering
        // http://projects.spring.io/spring-security-oauth/docs/oauth2.html
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(SecurityMockMvcConfigurers.springSecurity())
                .alwaysDo(print()).build();
    }

    @Test(dataProvider = "anonymousRequestBuilder")
    public void testAnonymousAccess(final RequestBuilder request, final ResultMatcher expectedStatus,
            final ResultMatcher expectedContent) throws Exception {
        this.mockMvc.perform(request).andExpect(expectedStatus).andExpect(expectedContent);
    }

    @Test(dataProvider = "invalidTokenRequestBuilder")
    public void testInvalidTokenAccess(final RequestBuilder request) throws Exception {
        this.mockMvc.perform(request).andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("invalid_token")));
    }

    @DataProvider
    private Object[][] anonymousRequestBuilder() {
        return combine(testAnonymousHealth(), testAnonymousHeartbeat());
    }

    private Object[][] testAnonymousHeartbeat() {
        return new Object[][] {
                { MockMvcRequestBuilders.get(HEARTBEAT_URL), status().isOk(), content().string("alive") } };
    }

    private Object[][] testAnonymousHealth() {
        return new Object[][] { { MockMvcRequestBuilders.get(HEALTH_URL), status().isServiceUnavailable(),
                content().string("{\"status\":\"DOWN\"}") } };
    }

    @DataProvider
    private Object[][] invalidTokenRequestBuilder() throws JsonProcessingException {
        return combine(testHealth(), testZoneController(), testAttributeConnectorController(),
                testPolicyEvaluationController(), testPolicyManagementController(),
                testResourcePrivilegeManagementController(), testSubjectPrivilegeManagementController());
    }

    private Object[][] testHealth() {
        return new Object[][] { get(HEALTH_URI, httpHeaders(MALFORMED_BEARER_TOKEN)) };
    }

    private Object[][] testZoneController() {
        return new Object[][] { put(ZONE_URI, httpHeaders(MALFORMED_BEARER_TOKEN)),
                get(ZONE_URI, httpHeaders(MALFORMED_BEARER_TOKEN)),
                delete(ZONE_URI, httpHeaders(MALFORMED_BEARER_TOKEN)) };
    }

    private Object[][] testAttributeConnectorController() {
        return new Object[][] { putWithMalformedBearerToken(RESOURCE_CONNECTOR_URI),
                getWithMalformedBearerToken(RESOURCE_CONNECTOR_URI),
                deleteWithMalformedBearerToken(RESOURCE_CONNECTOR_URI),
                putWithMalformedBearerToken(SUBJECT_CONNECTOR_URI), getWithMalformedBearerToken(SUBJECT_CONNECTOR_URI),
                deleteWithMalformedBearerToken(SUBJECT_CONNECTOR_URI) };
    }

    private Object[][] testPolicyEvaluationController() {
        return new Object[][] { postWithMalformedBearerToken(POLICY_EVAL_URI) };
    }

    private Object[][] testPolicyManagementController() {
        return new Object[][] { putWithMalformedBearerToken(POLICY_SET_URI),
                getWithMalformedBearerToken(POLICY_SET_URI), deleteWithMalformedBearerToken(POLICY_SET_URI),
                getWithMalformedBearerToken(POLICY_SETS_URI) };
    }

    private Object[][] testResourcePrivilegeManagementController() {
        return new Object[][] { postWithMalformedBearerToken(RESOURCES_URI), getWithMalformedBearerToken(RESOURCES_URI),
                getWithMalformedBearerToken(RESOURCE_URI), putWithMalformedBearerToken(RESOURCE_URI),
                deleteWithMalformedBearerToken(RESOURCE_URI) };
    }

    private Object[][] testSubjectPrivilegeManagementController() {
        return new Object[][] { postWithMalformedBearerToken(SUBJECTS_URI), getWithMalformedBearerToken(SUBJECTS_URI),
                getWithMalformedBearerToken(SUBJECT_URI), putWithMalformedBearerToken(SUBJECT_URI),
                deleteWithMalformedBearerToken(SUBJECT_URI) };
    }

    private static Object[] postWithMalformedBearerToken(final URI uri) {
        return post(uri, httpZoneHeaders(MALFORMED_BEARER_TOKEN));
    }

    private static Object[] post(final URI uri, final HttpHeaders headers) {
        return new Object[] { MockMvcRequestBuilders.post(uri).headers(headers)
                .content(CONTENT).contentType(MediaType.APPLICATION_JSON) };
    }

    private static Object[] putWithMalformedBearerToken(final URI uri) {
        return put(uri, httpZoneHeaders(MALFORMED_BEARER_TOKEN));
    }

    private static Object[] put(final URI uri, final HttpHeaders headers) {
        return new Object[] { MockMvcRequestBuilders.put(uri).headers(headers).content(CONTENT)
                .contentType(MediaType.APPLICATION_JSON) };
    }

    private static Object[] getWithMalformedBearerToken(final URI uri) {
        return get(uri, httpZoneHeaders(MALFORMED_BEARER_TOKEN));
    }

    private static Object[] get(final URI uri, final HttpHeaders headers) {
        return new Object[] { MockMvcRequestBuilders.get(uri).headers(headers).accept(MediaType.APPLICATION_JSON) };
    }

    private static Object[] deleteWithMalformedBearerToken(final URI uri) {
        return delete(uri, httpZoneHeaders(MALFORMED_BEARER_TOKEN));
    }

    private static Object[] delete(final URI uri, final HttpHeaders headers) {
        return new Object[] { MockMvcRequestBuilders.delete(uri).headers(headers) };
    }

    private static HttpHeaders httpHeaders(final String token) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.AUTHORIZATION, token);
        return httpHeaders;
    }

    private static HttpHeaders httpZoneHeaders(final String token) {
        HttpHeaders httpHeaders = httpHeaders(token);
        httpHeaders.add("Predix-Zone-Id", "myzone");
        return httpHeaders;
    }

    private static Object[][] combine(final Object[][]... testData) {
        List<Object[]> result = Lists.newArrayList();
        for (Object[][] t : testData) {
            result.addAll(Arrays.asList(t));
        }
        return result.toArray(new Object[result.size()][]);
    }
}
