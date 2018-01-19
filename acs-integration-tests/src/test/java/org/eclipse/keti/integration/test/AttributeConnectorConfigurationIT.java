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

import static org.eclipse.keti.acs.commons.web.AcsApiUriTemplates.RESOURCE_CONNECTOR_URL;
import static org.eclipse.keti.acs.commons.web.AcsApiUriTemplates.SUBJECT_CONNECTOR_URL;
import static org.eclipse.keti.acs.commons.web.AcsApiUriTemplates.V1;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import org.eclipse.keti.acs.rest.AttributeAdapterConnection;
import org.eclipse.keti.acs.rest.AttributeConnector;
import org.eclipse.keti.test.utils.ACSITSetUpFactory;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
public class AttributeConnectorConfigurationIT extends AbstractTestNGSpringContextTests {

    @Autowired
    private ACSITSetUpFactory acsitSetUpFactory;

    private String acsUrl;
    private OAuth2RestTemplate zone1Admin;
    private OAuth2RestTemplate zone1ConnectorAdmin;
    private OAuth2RestTemplate zone1ConnectorReadClient;
    private HttpHeaders zone1Headers;

    @BeforeClass
    public void setup() throws Exception {
        this.acsitSetUpFactory.setUp();

        this.acsUrl = this.acsitSetUpFactory.getAcsUrl();

        this.zone1Admin = this.acsitSetUpFactory.getAcsZoneAdminRestTemplate();

        this.zone1ConnectorAdmin = this.acsitSetUpFactory
                .getAcsZoneConnectorAdminRestTemplate(this.acsitSetUpFactory.getAcsZone1Name());
        this.zone1ConnectorReadClient = this.acsitSetUpFactory
                .getAcsZoneConnectorReadRestTemplate(this.acsitSetUpFactory.getAcsZone1Name());

        this.zone1Headers = this.acsitSetUpFactory.getZone1Headers();

    }

    @Test(dataProvider = "requestUrlProvider")
    public void testPutGetDeleteConnector(final String endpointUrl) throws Exception {
        AttributeConnector expectedConnector = new AttributeConnector();
        expectedConnector.setMaxCachedIntervalMinutes(100);
        expectedConnector.setAdapters(Collections.singleton(new AttributeAdapterConnection("https://my-endpoint.com",
                "https://my-uaa.com", "my-client", "my-secret")));
        try {
            this.zone1ConnectorAdmin.put(this.acsUrl + V1 + endpointUrl,
                    new HttpEntity<>(expectedConnector, this.zone1Headers));
        } catch (Exception e) {
            Assert.fail("Unable to create attribute connector. " + e.getMessage());
        }

        try {
            ResponseEntity<AttributeConnector> response = this.zone1ConnectorReadClient.exchange(
                    this.acsUrl + V1 + endpointUrl, HttpMethod.GET, new HttpEntity<>(this.zone1Headers),
                    AttributeConnector.class);
            Assert.assertEquals(response.getBody(), expectedConnector);
        } catch (Exception e) {
            Assert.fail("Unable to retrieve attribute connector." + e.getMessage());
        } finally {
            this.zone1ConnectorAdmin.exchange(this.acsUrl + V1 + endpointUrl, HttpMethod.DELETE,
                    new HttpEntity<>(this.zone1Headers), String.class);
        }
    }

    @Test(dataProvider = "requestUrlProvider")
    public void testCreateConnectorDeniedWithoutOauthToken(final String endpointUrl) throws Exception {
        RestTemplate acs = new RestTemplate();
        try {
            acs.put(this.acsUrl + V1 + endpointUrl,
                    new HttpEntity<>(new AttributeConnector(), this.zone1Headers));
            Assert.fail("No exception thrown when configuring connector without a token.");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNAUTHORIZED);
        }
    }

    @Test(dataProvider = "requestUrlProvider")
    public void testCreateConnectorDeniedWithoutSufficientScope(final String endpointUrl) throws Exception {
        try {
            this.zone1ConnectorReadClient.put(this.acsUrl + V1 + endpointUrl,
                    new HttpEntity<>(new AttributeConnector(), this.zone1Headers));
            Assert.fail("No exception thrown when creating connector without sufficient scope.");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.FORBIDDEN);
        }
    }

    // Due to the issue in spring security, 403 Forbidden response from the server, is received as a 400 Bad Request
    // error code because error is not correctly translated by the JSON deserializer
    //https://github.com/spring-projects/spring-security-oauth/issues/191
    @Test(dataProvider = "requestUrlProvider")
    public void testGetConnectorDeniedWithoutSufficientScope(final String endpointUrl) throws Exception {
        try {
            this.zone1Admin.exchange(this.acsUrl + V1 + endpointUrl, HttpMethod.GET,
                    new HttpEntity<>(this.zone1Headers), AttributeConnector.class);
            Assert.fail("No exception thrown when retrieving connector without sufficient scope.");
        } catch (HttpClientErrorException e) {
            e.printStackTrace();
            Assert.assertEquals(e.getStatusCode(), HttpStatus.FORBIDDEN);
        } catch (OAuth2Exception e) {
            e.printStackTrace();
            Assert.assertEquals(e.getHttpErrorCode(), HttpStatus.BAD_REQUEST.value());
        }
    }

    @Test(dataProvider = "requestUrlProvider")
    public void testDeleteConnectorDeniedWithoutSufficientScope(final String endpointUrl) throws Exception {
        try {
            this.zone1ConnectorReadClient.exchange(this.acsUrl + V1 + endpointUrl, HttpMethod.DELETE,
                    new HttpEntity<>(this.zone1Headers), String.class);
            Assert.fail("No exception thrown when deleting connector without sufficient scope.");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.FORBIDDEN);
        }
    }

    @DataProvider(name = "requestUrlProvider")
    private Object[][] requestUrlProvider() {
        return new String[][] { { RESOURCE_CONNECTOR_URL }, { SUBJECT_CONNECTOR_URL } };
    }

    @AfterClass
    public void cleanup() throws Exception {
        this.acsitSetUpFactory.destroy();
    }
}
