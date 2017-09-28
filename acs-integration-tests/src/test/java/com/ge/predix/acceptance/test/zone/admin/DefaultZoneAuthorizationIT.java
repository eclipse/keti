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

package com.ge.predix.acceptance.test.zone.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.test.utils.ACSITSetUpFactory;
import com.ge.predix.test.utils.ACSTestUtil;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.PrivilegeHelper;

@Test
@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class DefaultZoneAuthorizationIT extends AbstractTestNGSpringContextTests {

    @Autowired
    private PrivilegeHelper privilegeHelper;
    @Autowired
    private ACSITSetUpFactory acsitSetUpFactory;

    private String zone2Name;

    @BeforeClass
    public void setup() throws Exception {
        this.acsitSetUpFactory.setUp();
        this.zone2Name = this.acsitSetUpFactory.getZone2().getName();
    }

    @AfterClass
    public void cleanup() {
        this.acsitSetUpFactory.destroy();
        
    }

    /**
     * 1. Create a token from zone issuer with scopes for accessing: a. zone specific resources, AND b.
     * acs.zones.admin
     *
     * 2. Try to access a zone specific resource . This should work 3. Try to access /v1/zone - THIS SHOULD FAIL
     *
     * @throws Exception
     */
    public void testAccessGlobalResourceWithZoneIssuer() throws Exception {
        OAuth2RestTemplate zone2AcsTemplate = this.acsitSetUpFactory.getAcsZone2AdminRestTemplate();

        HttpHeaders zoneTwoHeaders = ACSTestUtil.httpHeaders();
        zoneTwoHeaders.set(PolicyHelper.PREDIX_ZONE_ID, this.zone2Name);

        // Write a resource to zone2. This should work
        ResponseEntity<Object> responseEntity = this.privilegeHelper.postResources(zone2AcsTemplate,
                this.acsitSetUpFactory.getAcsUrl(), zoneTwoHeaders, new BaseResource("/sites/sanramon"));
        Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.NO_CONTENT);

        // Try to get global resource from global/baseUrl. This should FAIL
        try {
            zone2AcsTemplate.exchange(this.acsitSetUpFactory.getAcsUrl() + "/v1/zone/" + this.zone2Name, HttpMethod.GET,
                    null, Zone.class);
            Assert.fail("Able to access non-zone specific resource with a zone specific issuer token!");
        } catch (HttpClientErrorException e) {
            // expected
        }

        // Try to get global resource from zone2Url. This should FAIL
        try {
            zone2AcsTemplate.exchange(this.acsitSetUpFactory.getAcsUrl() + "/v1/zone/" + this.zone2Name, HttpMethod.GET,
                    new HttpEntity<>(zoneTwoHeaders), Zone.class);
            Assert.fail("Able to access non-zone specific resource from a zone specific URL, "
                    + "with a zone specific issuer token!");
        } catch (InvalidRequestException e) {
            // expected
        }

    }

}
