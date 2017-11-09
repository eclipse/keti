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

package com.ge.predix.acs.migration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.test.utils.ACSITSetUpFactory;
import com.ge.predix.test.utils.ACSTestUtil;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.PrivilegeHelper;

/**
 * This test class is meant for one-off testing of migration of resource, subject attributes from postgres to titan.
 */
@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class AttributeMigrationTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private ACSITSetUpFactory acsitSetUpFactory;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    private OAuth2RestTemplate acsZoneTemplate;

    private final Set<Attribute> oneAttribute = new HashSet<>(
            Arrays.asList(new Attribute("https://issuer1/oauth/token", "role", "admin")));

    private final Set<Attribute> threeAttributes = new HashSet<>(
            Arrays.asList(new Attribute("https://issuer1/oauth/token", "role", "admin"),
                    new Attribute("https://issuer1/oauth/token", "site", "ny"),
                    new Attribute("https://issuer2/oauth/token", "region", "west")));

    private final List<BaseResource> testResources = Arrays.asList(
            new BaseResource("testMigrate-resource-0", Collections.emptySet()),
            new BaseResource("testMigrate-resource-1", this.oneAttribute),
            new BaseResource("testMigrate-resource-2", this.threeAttributes));

    private final List<BaseSubject> testSubjects = Arrays.asList(
            new BaseSubject("testMigrate-subject-0", Collections.emptySet()),
            new BaseSubject("testMigrate-subject-1", this.oneAttribute),
            new BaseSubject("testMigrate-subject-2", this.threeAttributes));

    @BeforeClass
    public void setup() throws Exception {
        this.acsitSetUpFactory.setUp();
        this.acsZoneTemplate = this.acsitSetUpFactory.getAcsZonesAdminRestTemplate();
    }

    @Test(dataProvider = "zones")
    public void pushResourceAndSubjectAttributesForMigration(final String zone) {
        HttpHeaders headers = ACSTestUtil.httpHeaders();
        headers.set(PolicyHelper.PREDIX_ZONE_ID, zone);

        // write resources
        this.privilegeHelper.postResources(this.acsZoneTemplate, this.acsitSetUpFactory.getAcsUrl(), headers,
                (BaseResource[]) this.testResources.toArray());

        // write subjects
        this.privilegeHelper.postSubjects(this.acsZoneTemplate, this.acsitSetUpFactory.getAcsUrl(), headers,
                (BaseSubject[]) this.testSubjects.toArray());

        verifyResourceAndSubjectAttributesPostMigration(zone);
    }

    //@Test(dataProvider = "zones")
    public void testMigratedAttributesPostMigration(final String zone) {
        //Uncomment this after new acs version is deployed and attributes migrated.

        //verifyResourceAndSubjectAttributesPostMigration(zone);
    }

    private void verifyResourceAndSubjectAttributesPostMigration(final String zone) {

        HttpHeaders headers = ACSTestUtil.httpHeaders();
        headers.set(PolicyHelper.PREDIX_ZONE_ID, zone);

        //Retrieve resources and compare
        BaseResource[] migratedResources = this.privilegeHelper.listResources(this.acsZoneTemplate,
                this.acsitSetUpFactory.getAcsUrl(), headers);
        Assert.assertEquals(migratedResources.length, this.testResources.size());
        this.testResources.stream()
                .forEach(testResource -> assertTestResourceExists(testResource, Arrays.asList(migratedResources)));

        //Retrieve subjects and compare
        BaseSubject[] migratedSubjects = this.privilegeHelper.listSubjects(this.acsZoneTemplate,
                this.acsitSetUpFactory.getAcsUrl(), headers);
        Assert.assertEquals(migratedSubjects.length, this.testSubjects.size());
        this.testSubjects.stream()
                .forEach(testSubject -> assertTestSubjectExists(testSubject, Arrays.asList(migratedSubjects)));
    }

    private void assertTestResourceExists(final BaseResource expectedResource,
            final List<BaseResource> actualResources) {
        for (BaseResource actualResource : actualResources) {
            if (expectedResource.getResourceIdentifier().equals(actualResource.getResourceIdentifier())) {
                Assert.assertEquals(expectedResource.getAttributes(), actualResource.getAttributes());
                return;
            }
        }
        Assert.fail("Expected Resource not found: " + expectedResource);
    }

    private void assertTestSubjectExists(final BaseSubject expectedSubject, final List<BaseSubject> actualSubjects) {
        for (BaseSubject actualSubject : actualSubjects) {
            if (expectedSubject.getSubjectIdentifier().equals(actualSubject.getSubjectIdentifier())) {
                Assert.assertEquals(expectedSubject.getAttributes(), actualSubject.getAttributes());
                return;
            }
        }
        Assert.fail("Expected Subject not found: " + expectedSubject);
    }

    @DataProvider
    public Object[][] zones() {
        return new String[][] { { this.acsitSetUpFactory.getAcsZone1Name() },
                { this.acsitSetUpFactory.getAcsZone2Name() } };
    }
}
