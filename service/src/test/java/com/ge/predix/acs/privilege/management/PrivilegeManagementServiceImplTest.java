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
package com.ge.predix.acs.privilege.management;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.SpringSecurityPolicyContextResolver;
import com.ge.predix.acs.config.InMemoryDataSourceConfig;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.policy.evaluation.cache.HystrixPolicyEvaluationCacheCircuitBreaker;
import com.ge.predix.acs.policy.evaluation.cache.InMemoryPolicyEvaluationCache;
import com.ge.predix.acs.request.context.AcsRequestContextHolder;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.testutils.MockSecurityContext;
import com.ge.predix.acs.testutils.TestUtils;
import com.ge.predix.acs.zone.management.ZoneService;
import com.ge.predix.acs.zone.management.ZoneServiceImpl;
import com.ge.predix.acs.zone.resolver.SpringSecurityZoneResolver;

@ContextConfiguration(
        classes = { AcsRequestContextHolder.class, HystrixPolicyEvaluationCacheCircuitBreaker.class,
                InMemoryDataSourceConfig.class, InMemoryPolicyEvaluationCache.class,
                PrivilegeManagementServiceImpl.class, SpringSecurityPolicyContextResolver.class,
                SpringSecurityZoneResolver.class, ZoneServiceImpl.class })
@ActiveProfiles(profiles = { "h2", "public", "simple-cache" })
public class PrivilegeManagementServiceImplTest extends AbstractTransactionalTestNGSpringContextTests {

    @Autowired
    private PrivilegeManagementService service;

    private final AttributesUtilities attributesUtilities = new AttributesUtilities();

    private Set<Attribute> fixedAttributes;

    @Autowired
    private ZoneService zoneService;

    private final TestUtils testUtils = new TestUtils();
    private Zone testZone;

    @BeforeClass
    public void beforeClass() {
        this.testZone = this.testUtils.setupTestZone("PrivilegeManagementServiceImplTest", this.zoneService);
    }

    @AfterClass
    public void cleanupAfterClass() {
        this.zoneService.deleteZone(this.testZone.getName());
    }

    @BeforeMethod
    public void setup() {
        this.fixedAttributes = this.attributesUtilities.getSetOfAttributes(new Attribute("acs", "group", "admin"));
    }

    @Test
    public void testAppendResources() {
        doAppendResourcesAndAssert("/asset/sanramon", "/asset/ny");
    }

    @Test(dataProvider = "emptyIdDataProvider", expectedExceptions = PrivilegeManagementException.class)
    public void testAppendResourceWithEmptyResourceId(final String resourceIdentifier) {
        BaseResource resource = createResource(resourceIdentifier);
        this.service.appendResources(asList(resource));
    }

    @DataProvider(name = "emptyIdDataProvider")
    private Object[][] emptyIdDataProvider() {
        return new Object[][] {

                { "" }, { null }, };
    }

    @Test(expectedExceptions = PrivilegeManagementException.class)
    public void testAppendNullResources() {
        this.service.appendResources(null);
    }

    @SuppressWarnings("unchecked")
    @Test(expectedExceptions = PrivilegeManagementException.class)
    public void testAppendEmptyResources() {
        this.service.appendResources(Collections.EMPTY_LIST);
    }

    @Test
    public void testCreateResource() {
        doCreateResourceAndAssert("/asset/sanrafael");
    }

    // @Test
    // public void testCreateResourceGetWithDifferentIssuer()
    // {
    // String resourceIdentifier = "/asset/sanrafael";
    // Resource resource = doCreateResourceAndAssert(resourceIdentifier);
    //
    // configureSecurityContextFixture("ampid", "ampusername",
    // "dave@predix.com", ISSUER_2);
    // Assert.assertNull(this.service.getByResourceId(resource.getResourceId()));
    // Assert.assertNull(this.service.getByResourceIdentifier(resourceIdentifier));
    //
    // }

    @Test
    public void testUpdateResource() {
        String resourceIdentifier = "/asset/sananselmo";
        BaseResource resource = doCreateResourceAndAssert(resourceIdentifier);

        resource.getAttributes().add(new Attribute("acs", "group", "analyst"));

        boolean created = this.service.upsertResource(resource);
        Assert.assertFalse(created);
        BaseResource savedResource = this.service.getByResourceIdentifier(resource.getResourceIdentifier());
        assertResource(savedResource, resource, 2);
    }

    @Test
    public void testDeleteResource() {
        String resourceIdentifier = "/asset/santarita";
        BaseResource resource = doCreateResourceAndAssert(resourceIdentifier);
        String id = resource.getResourceIdentifier();

        boolean deleted = this.service.deleteResource(id);
        Assert.assertTrue(deleted);
        Assert.assertNull(this.service.getByResourceIdentifier(id));
    }

    @Test
    public void testDeleteInvalidResource() {
        Assert.assertFalse(this.service.deleteResource("invalid_id"));
    }

    // @Test
    // public void testGetResourcesEmptyResult()
    // {
    // doAppendResourcesAndAssert("/asset/macfarland", "/asset/oregon");
    //
    // configureSecurityContextFixture("ampid", "ampusername",
    // "dave@predix.com", ISSUER_2);
    // List<Resource> resources = this.service.getResources();
    // Assert.assertEquals(resources.size(), 0);
    //
    // }

    @Test
    public void testAppendSubjects() {
        BaseSubject s1 = createSubject("dave");
        BaseSubject s2 = createSubject("sanjeev");

        this.service.appendSubjects(asList(s1, s2));

        // able to get subject by identifier
        Assert.assertNotNull(this.service.getBySubjectIdentifier(s1.getSubjectIdentifier()));
        Assert.assertNotNull(this.service.getBySubjectIdentifier(s2.getSubjectIdentifier()));
    }

    @Test(expectedExceptions = PrivilegeManagementException.class)
    public void testAppendNullSubjects() {
        this.service.appendSubjects(null);
    }

    @SuppressWarnings("unchecked")
    @Test(expectedExceptions = PrivilegeManagementException.class)
    public void testAppendEmptySubjects() {
        this.service.appendSubjects(Collections.EMPTY_LIST);
    }

    @Test
    public void testCreateSubject() {
        String subjectIdentifier = "marissa";
        BaseSubject subject = createSubject(subjectIdentifier);

        boolean created = this.service.upsertSubject(subject);
        Assert.assertTrue(created);
        Assert.assertTrue(this.service.getBySubjectIdentifier(subjectIdentifier).equals(subject));

    }

    // TODO enable it back when the zone resolver is fully implemented
    @Test(expectedExceptions = SecurityException.class, enabled = false)
    public void testCreateSubjectAndGetWithDifferentClientId() {
        String subjectIdentifier = "Dave-ID123";
        BaseSubject subject = createSubject(subjectIdentifier);

        boolean created = this.service.upsertSubject(subject);
        Assert.assertTrue(created);
        Assert.assertTrue(this.service.getBySubjectIdentifier(subjectIdentifier).equals(subject));

        MockSecurityContext.mockSecurityContext(this.testZone);
        BaseSubject returnedSubject = this.service.getBySubjectIdentifier(subjectIdentifier);
        Assert.assertNull(returnedSubject);
    }

    // TODO enable it back when the zone resolver is fully implemented
    @Test(expectedExceptions = SecurityException.class, enabled = false)
    public void testCreateResourceAndGetWithDifferentClientId() {
        String resourceIdentifier = "Dave-ID123";
        BaseResource resource = createResource(resourceIdentifier);

        boolean created = this.service.upsertResource(resource);
        Assert.assertTrue(created);
        Assert.assertTrue(this.service.getByResourceIdentifier(resourceIdentifier).equals(resource));

        MockSecurityContext.mockSecurityContext(this.testZone);
        BaseResource returnedResource = this.service.getByResourceIdentifier(resourceIdentifier);
        Assert.assertNull(returnedResource);
    }

    /*
     * TODO Need this test when we start supporting multiple issuers
     *
     * public void testCreateSubjectAndGetWithDifferentIssuerId(){ String subjectIdentifier = "Dave-ID123"; Subject
     * subject = createSubject(subjectIdentifier); boolean created = this.service.upsertSubject(subject);
     * Assert.assertTrue(created);
     *
     * PolicyContextResloverUtilTest scope = new PolicyContextResloverUtilTest();
     * scope.mockSecurityContext("ISSUER_1234", CLIENT_ID); }
     */

    @Test
    public void testUpdateSubject() {
        String subjectIdentifier = "/asset/sananselmo";
        BaseSubject subject = createSubject(subjectIdentifier);

        boolean created = this.service.upsertSubject(subject);
        Assert.assertTrue(created);
        Assert.assertTrue(this.service.getBySubjectIdentifier(subjectIdentifier).equals(subject));

        subject.getAttributes().add(new Attribute("acs", "group", "analyst"));

        created = this.service.upsertSubject(subject);
        Assert.assertFalse(created);

        BaseSubject savedSubject = this.service.getBySubjectIdentifier(subject.getSubjectIdentifier());
        assertSubject(savedSubject, subject, 2);
    }

    @Test
    public void testDeleteSubject() {
        String subjectIdentifier = "/asset/santarita";
        BaseSubject subject = createSubject(subjectIdentifier);
        this.service.upsertSubject(subject);

        boolean deleted = this.service.deleteSubject(subjectIdentifier);
        Assert.assertTrue(deleted);
        Assert.assertNull(this.service.getBySubjectIdentifier(subjectIdentifier));
    }

    @Test
    public void testDeleteInvalidSubject() {
        Assert.assertFalse(this.service.deleteSubject("invalid_id"));
    }

    @Test
    public void testGetSubjects() {
        BaseSubject r1 = createSubject("/asset/macfarland");
        BaseSubject r2 = createSubject("/asset/oregon");

        this.service.appendSubjects(asList(r1, r2));
        List<BaseSubject> subjects = this.service.getSubjects();
        Assert.assertEquals(subjects.size(), 2);

    }

    private BaseSubject createSubject(final String subjectIdentifier) {
        BaseSubject subject = new BaseSubject();
        subject.setSubjectIdentifier(subjectIdentifier);
        subject.setAttributes(this.fixedAttributes);
        return subject;
    }

    private BaseResource createResource(final String resourceIdentifier) {
        BaseResource resource = new BaseResource();
        resource.setResourceIdentifier(resourceIdentifier);
        resource.setAttributes(this.fixedAttributes);
        return resource;
    }

    private void assertResource(final BaseResource savedResource, final BaseResource resource,
            final int numOfAttributes) {
        Assert.assertNotNull(savedResource);
        Assert.assertEquals(savedResource.getResourceIdentifier(), resource.getResourceIdentifier());
        Assert.assertEquals(savedResource.getAttributes().size(), numOfAttributes);
    }

    private void assertSubject(final BaseSubject savedSubject, final BaseSubject subject, final int numOfAttributes) {
        Assert.assertNotNull(savedSubject);
        Assert.assertEquals(savedSubject.getSubjectIdentifier(), subject.getSubjectIdentifier());
        Assert.assertEquals(savedSubject.getAttributes().size(), numOfAttributes);
    }

    public void doAppendResourcesAndAssert(final String identifier1, final String identifier2) {
        BaseResource r1 = createResource(identifier1);
        BaseResource r2 = createResource(identifier2);

        this.service.appendResources(asList(r1, r2));

        BaseResource fetchedResource1 = this.service.getByResourceIdentifier(r1.getResourceIdentifier());
        BaseResource fetchedResource2 = this.service.getByResourceIdentifier(r2.getResourceIdentifier());

        List<BaseResource> resources = this.service.getResources();
        Assert.assertTrue(resources.size() >= 2);

        Assert.assertEquals(fetchedResource1, r1);
        Assert.assertEquals(fetchedResource2, r2);

    }

    private BaseResource doCreateResourceAndAssert(final String resourceIdentifier) {
        BaseResource resource = createResource(resourceIdentifier);

        boolean created = this.service.upsertResource(resource);
        Assert.assertTrue(created);

        Assert.assertTrue(this.service.getByResourceIdentifier(resourceIdentifier).equals(resource));
        return resource;
    }

}
