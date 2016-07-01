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
package com.ge.predix.acs.zone.management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ge.predix.acs.SpringSecurityPolicyContextResolver;
import com.ge.predix.acs.config.GraphBeanDefinitionRegistryPostProcessor;
import com.ge.predix.acs.config.GraphConfig;
import com.ge.predix.acs.config.InMemoryDataSourceConfig;
import com.ge.predix.acs.privilege.management.dao.GraphResourceRepository;
import com.ge.predix.acs.privilege.management.dao.GraphSubjectRepository;
import com.ge.predix.acs.privilege.management.dao.ResourceEntity;
import com.ge.predix.acs.privilege.management.dao.ResourceRepository;
import com.ge.predix.acs.privilege.management.dao.SubjectEntity;
import com.ge.predix.acs.privilege.management.dao.SubjectRepository;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.service.policy.admin.dao.PolicySetEntity;
import com.ge.predix.acs.service.policy.admin.dao.PolicySetRepository;
import com.ge.predix.acs.testutils.TestActiveProfilesResolver;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.management.dao.ZoneRepository;
import com.ge.predix.acs.zone.resolver.SpringSecurityZoneResolver;

@ContextConfiguration(
        classes = { GraphBeanDefinitionRegistryPostProcessor.class, GraphConfig.class, GraphResourceRepository.class,
                GraphSubjectRepository.class, InMemoryDataSourceConfig.class, SpringSecurityPolicyContextResolver.class,
                SpringSecurityZoneResolver.class, ZoneServiceImpl.class })
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@Test(singleThreaded = true)
public class ZoneEntityTest extends AbstractTransactionalTestNGSpringContextTests {

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private PolicySetRepository policySetRepository;

    @Autowired
    private ZoneService service;

    // zone with 1 issuer - issuer1
    private Zone zone1;

    // Some tests modify this standard setup. Reset before every test.
    @BeforeMethod
    public void setup() {
        this.zone1 = new Zone("zone1", "zone1", "description");
    }

    public void testCreateZoneWithConstraintViolationSubdomain() {
        Zone testzone1 = new Zone("zone1", "subdomain1", "description");
        Zone zone2SameSubdomain = new Zone("zone2", "subdomain1", "description");

        try {
            this.service.upsertZone(testzone1);
            this.service.upsertZone(zone2SameSubdomain);

        } catch (ZoneManagementException e) {

            Assert.assertEquals(e.getMessage(), "Subdomain subdomain1 for zoneName = zone2 is already being used.");
            this.service.deleteZone(testzone1.getName());
            this.service.deleteZone(zone2SameSubdomain.getName());
            return;
        }
        this.service.deleteZone(testzone1.getName());
        this.service.deleteZone(zone2SameSubdomain.getName());
        Assert.fail("Expected ZoneManagementException to be thrown.");
    }

    public void testUpdateZoneSubdomain() {
        this.service.upsertZone(this.zone1);
        Zone updatedZone1 = new Zone("zone1", "updated-subdomain", "description");
        this.service.upsertZone(updatedZone1);
        ZoneEntity actualZone = this.zoneRepository.getByName(this.zone1.getName());
        Assert.assertEquals(actualZone.getSubdomain(), "updated-subdomain");
    }

    public void testCreateZone() {
        this.service.upsertZone(this.zone1);
        Zone zone1Actual = this.service.retrieveZone(this.zone1.getName());
        Assert.assertEquals(zone1Actual, this.zone1);
    }

    public void testDeleteZoneWithCascade() {

        // create test zone
        Zone testZone = new Zone("test-zone", "test-zone-subdomain", "description");
        this.service.upsertZone(testZone);
        ZoneEntity testZoneEntity = this.zoneRepository.getByName(testZone.getName());

        // put subject, resource, and policy
        SubjectEntity subjectEntity = new SubjectEntity(testZoneEntity, "bob");
        subjectEntity.setAttributesAsJson("{}");
        this.subjectRepository.save(subjectEntity);

        ResourceEntity resourceEntity = new ResourceEntity(testZoneEntity, "bob");
        resourceEntity.setAttributesAsJson("[]");
        this.resourceRepository.save(resourceEntity);

        PolicySetEntity policySetEntity = new PolicySetEntity(testZoneEntity, "policy-set-2", "{}");
        this.policySetRepository.save(policySetEntity);

        // Check if in repo
        Assert.assertEquals(this.subjectRepository.getByZoneAndSubjectIdentifier(testZoneEntity, "bob"), subjectEntity);
        Assert.assertEquals(this.resourceRepository.getByZoneAndResourceIdentifier(testZoneEntity, "bob"),
                resourceEntity);
        Assert.assertEquals(this.policySetRepository.getByZoneAndPolicySetId(testZoneEntity, "policy-set-2"),
                policySetEntity);
        Assert.assertEquals(testZoneEntity.getName(), testZone.getName());

        // delete zone and assert proper cascading
        this.service.deleteZone(testZone.getName());
        Assert.assertEquals(this.subjectRepository.getByZoneAndSubjectIdentifier(testZoneEntity, "bob"), null);
        Assert.assertEquals(this.resourceRepository.getByZoneAndResourceIdentifier(testZoneEntity, "bob"), null);
        Assert.assertEquals(this.policySetRepository.getByZoneAndPolicySetId(testZoneEntity, "policy-set-2"), null);
        testZoneEntity = this.zoneRepository.getByName(testZone.getName());
        Assert.assertEquals(testZoneEntity, null);

    }
}
