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
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.PrivilegeHelper;
import com.ge.predix.test.utils.ZoneHelper;

/**
 * This test class is meant for one-off testing of migration of resource, subject attributes from postgres to titan.
 */
@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class AttributeMigrationTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    @Autowired
    private ZoneHelper zoneHelper;

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
        this.zoneHelper.createPrimaryTestZone();
        this.zoneHelper.createTestZone2();
        this.acsZoneTemplate = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
    }

    @Test(dataProvider = "zoneUrls")
    public void pushResourceAndSubjectAttributesForMigration(final String zoneUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
        //write resources
        this.privilegeHelper.postResources(this.acsZoneTemplate, zoneUrl, headers,
                (BaseResource[]) this.testResources.toArray());

        //write subjects
        this.privilegeHelper.postSubjects(this.acsZoneTemplate, zoneUrl, headers,
                (BaseSubject[]) this.testSubjects.toArray());


        verifyResourceAndSubjectAttributesPostMigration(zoneUrl);
    }

    //@Test(dataProvider = "zoneUrls")
    public void testMigratedAttributesPostMigration(final String zoneUrl) {
        //Uncomment this after new acs version is deployed and attributes migrated.
        
        //verifyResourceAndSubjectAttributesPostMigration(zoneUrl);
    }

    private void verifyResourceAndSubjectAttributesPostMigration(final String zoneUrl) {

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        //Retrieve resources and compare
        BaseResource[] migratedResources = this.privilegeHelper.listResources(this.acsZoneTemplate,
                this.zoneHelper.getZone1Url(), headers);
        Assert.assertEquals(migratedResources.length, this.testResources.size());
        this.testResources.stream()
                .forEach(testResource -> assertTestResourceExists(testResource, Arrays.asList(migratedResources)));

        //Retrieve subjects and compare
        BaseSubject[] migratedSubjects = this.privilegeHelper.listSubjects(this.acsZoneTemplate,
                this.zoneHelper.getZone1Url(), new HttpHeaders());
        Assert.assertEquals(migratedSubjects.length, this.testSubjects.size());
        this.testSubjects.stream()
                .forEach(testSubject -> assertTestSubjectExists(testSubject, Arrays.asList(migratedSubjects)));
    }
    
    private void assertTestResourceExists(final BaseResource expectedResource, final List<BaseResource> actualResources) {
        for (BaseResource actualResource : actualResources) {
            if (expectedResource.getResourceIdentifier().equals(actualResource.getResourceIdentifier())) {
                Assert.assertEquals(expectedResource.getAttributes(), actualResource.getAttributes());
                return;
            }
        }
        Assert.fail("Expected Resource not found: "+expectedResource);
    }
    
    private void assertTestSubjectExists(final BaseSubject expectedSubject, final List<BaseSubject> actualSubjects) {
        for (BaseSubject actualSubject : actualSubjects) {
            if (expectedSubject.getSubjectIdentifier().equals(actualSubject.getSubjectIdentifier())) {
                Assert.assertEquals(expectedSubject.getAttributes(), actualSubject.getAttributes());
                return;
            }
        }
        Assert.fail("Expected Subject not found: "+expectedSubject);
    }

    @DataProvider
    public Object[][] zoneUrls() {
        return new String[][] { { this.zoneHelper.getZone1Url() }, { this.zoneHelper.getZone2Url() } };
    }
}
