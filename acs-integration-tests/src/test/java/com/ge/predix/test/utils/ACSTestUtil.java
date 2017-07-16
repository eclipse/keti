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

package com.ge.predix.test.utils;

import java.net.ConnectException;
import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;

/**
 * @author 212406427
 */
@Component
@SuppressWarnings({ "nls", "javadoc" })
public class ACSTestUtil {

    // used in all integration tests - if changed, it will be reflected on all tests
    public static final String ACS_VERSION = "/v1";

    public void assertExceptionResponseBody(final HttpClientErrorException e, final String message) {
        String responseBody = e.getResponseBodyAsString();
        Assert.assertNotNull(responseBody);
        Assert.assertTrue(responseBody.contains(message),
                String.format("Expected=[%s], Actual=[%s]", message, responseBody));
    }

    public static HttpHeaders httpHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.ALL_VALUE);
        return httpHeaders;
    }

    public static boolean isServerListening(final URI url) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            restTemplate.getForObject(url, String.class);
        } catch (RestClientException e) {
            if (e.getCause() instanceof ConnectException) {
                return false;
            }
        }
        return true;
    }
}
