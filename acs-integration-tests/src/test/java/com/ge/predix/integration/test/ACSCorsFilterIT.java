/*******************************************************************************
 * Copyright 2016 General Electric Company. 
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
 *******************************************************************************/
package com.ge.predix.integration.test;

import java.io.IOException;
import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ge.predix.test.utils.ZacTestUtil;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
public class ACSCorsFilterIT extends AbstractTestNGSpringContextTests {

    private static final String SWAGGER_API = "/v2/api-docs?group=acs";

    @Value("${acsUrl:http://localhost:8181}")
    private String acsBaseUrl;

    @Autowired
    private ZacTestUtil zacTestUtil;

    @Autowired
    Environment env;

    @BeforeClass
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        if (!Arrays.asList(this.env.getActiveProfiles()).contains("public")) {
            this.zacTestUtil.assumeZacServerAvailable();
        }
    }

    @Test
    public void testCorsXHRRequestFromAllowedOriginForSwaggerUIApi() throws Exception {
        HttpClient client = HttpClientBuilder.create().build();
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
        HttpClient client = HttpClientBuilder.create().build();
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
        HttpClient client = HttpClientBuilder.create().build();
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
