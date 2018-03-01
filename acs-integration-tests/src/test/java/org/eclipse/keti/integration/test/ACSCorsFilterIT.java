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

package org.eclipse.keti.integration.test;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
public class ACSCorsFilterIT extends AbstractTestNGSpringContextTests {

    private static final String SWAGGER_API = "/v2/api-docs?group=acs";

    @Value("${ACS_URL}")
    private String acsBaseUrl;

    private HttpClient client;

    @BeforeClass
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        client = HttpClientBuilder.create().useSystemProperties().build();
    }

    @Test
    public void testCorsXHRRequestFromAllowedOriginForSwaggerUIApi() throws Exception {
        HttpGet request = new HttpGet(this.acsBaseUrl + SWAGGER_API);
        request.setHeader(HttpHeaders.ORIGIN, "http://someone.predix.io");
        request.setHeader("X-Requested-With", "true");
        HttpResponse response = client.execute(request);
        System.out.println("Response Code : " + response.getStatusLine().getStatusCode());

        System.out.println(
                "Access-Control-Allow-Origin : " + response.getHeaders("Access-Control-Allow-Origin")[0].getValue());

        Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

        Assert.assertTrue(response.containsHeader("Access-Control-Allow-Origin"));
    }

    @Test
    public void testCorsXHRRequestFromNotWhitelistedOriginForSwaggerUIApi() throws Exception {
        HttpGet request = new HttpGet(this.acsBaseUrl + SWAGGER_API);
        request.setHeader(HttpHeaders.ORIGIN, "Origin: http://someone.predix.nert");
        request.setHeader("X-Requested-With", "true");
        HttpResponse response = client.execute(request);
        System.out.println("Response Code : " + response.getStatusLine().getStatusCode());

        System.out
                .println("Access-Control-Allow-Origin : " + response.getHeaders("Access-Control-Allow-Origin").length);

        Assert.assertEquals(response.getStatusLine().getStatusCode(), 403);
        Assert.assertFalse(response.containsHeader("Access-Control-Allow-Origin"));
    }

    @Test
    public void testCorsXHRRequestFromWhitelistedOriginForNonSwaggerUIApi() throws Exception {
        HttpGet request = new HttpGet(this.acsBaseUrl + "/acs");
        request.setHeader(HttpHeaders.ORIGIN, "http://someone.predix.io");
        request.setHeader("X-Requested-With", "true");
        HttpResponse response = client.execute(request);
        System.out.println("Response Code : " + response.getStatusLine().getStatusCode());

        System.out
                .println("Access-Control-Allow-Origin : " + response.getHeaders("Access-Control-Allow-Origin").length);

        Assert.assertEquals(response.getStatusLine().getStatusCode(), 403);
        Assert.assertFalse(response.containsHeader("Access-Control-Allow-Origin"));
    }
}
