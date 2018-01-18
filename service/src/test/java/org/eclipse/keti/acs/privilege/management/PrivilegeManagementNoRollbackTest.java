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

package org.eclipse.keti.acs.privilege.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.eclipse.keti.acs.SpringSecurityPolicyContextResolver;
import org.eclipse.keti.acs.attribute.cache.AttributeCacheFactory;
import org.eclipse.keti.acs.attribute.connector.management.AttributeConnectorServiceImpl;
import org.eclipse.keti.acs.attribute.readers.AttributeReaderFactory;
import org.eclipse.keti.acs.attribute.readers.PrivilegeServiceResourceAttributeReader;
import org.eclipse.keti.acs.attribute.readers.PrivilegeServiceSubjectAttributeReader;
import org.eclipse.keti.acs.config.GraphBeanDefinitionRegistryPostProcessor;
import org.eclipse.keti.acs.config.GraphConfig;
import org.eclipse.keti.acs.config.InMemoryDataSourceConfig;
import org.eclipse.keti.acs.model.Attribute;
import org.eclipse.keti.acs.policy.evaluation.cache.InMemoryPolicyEvaluationCache;
import org.eclipse.keti.acs.privilege.management.dao.ResourceRepositoryProxy;
import org.eclipse.keti.acs.privilege.management.dao.SubjectRepositoryProxy;
import org.eclipse.keti.acs.request.context.AcsRequestContextHolder;
import org.eclipse.keti.acs.rest.BaseResource;
import org.eclipse.keti.acs.rest.BaseSubject;
import org.eclipse.keti.acs.rest.Zone;
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver;
import org.eclipse.keti.acs.testutils.TestUtils;
import org.eclipse.keti.acs.zone.management.ZoneService;
import org.eclipse.keti.acs.zone.management.ZoneServiceImpl;
import org.eclipse.keti.acs.zone.resolver.SpringSecurityZoneResolver;

@ContextConfiguration(classes = { AcsRequestContextHolder.class, InMemoryDataSourceConfig.class,
        InMemoryPolicyEvaluationCache.class, AttributeCacheFactory.class, PrivilegeManagementServiceImpl.class,
        GraphBeanDefinitionRegistryPostProcessor.class, GraphConfig.class, ZoneServiceImpl.class,
        SpringSecurityPolicyContextResolver.class, SpringSecurityZoneResolver.class, SubjectRepositoryProxy.class,
        ResourceRepositoryProxy.class, AttributeConnectorServiceImpl.class, AttributeReaderFactory.class,
        PrivilegeServiceResourceAttributeReader.class, PrivilegeServiceSubjectAttributeReader.class })
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@TestPropertySource("classpath:application.properties")
@Test
public class PrivilegeManagementNoRollbackTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private PrivilegeManagementService service;

    @Autowired
    private ZoneService zoneService;

    private final AttributesUtilities attributesUtilities = new AttributesUtilities();
    private Set<Attribute> fixedAttributes;
    private final TestUtils testUtils = new TestUtils();
    private Zone testZone;

    @BeforeClass
    public void setup() throws InterruptedException, ExecutionException {
        this.testZone = this.testUtils.setupTestZone("PrivilegeManagementNoRollbackTest", this.zoneService);
        this.fixedAttributes = this.attributesUtilities.getSetOfAttributes(new Attribute("acs", "group", "admin"));
    }

    @AfterClass
    public void cleanup() {
        this.zoneService.deleteZone(this.testZone.getName());
    }

    public void testCreateMultipleSubjectWithConstraintViolationSubjectIdentifier() {
        String subjectIdentifier = "Dave-ID123";

        List<BaseSubject> subjects = new ArrayList<>();
        subjects.add(createSubject(subjectIdentifier));
        subjects.add(createSubject(subjectIdentifier));
        try {
            this.service.appendSubjects(subjects);
        } catch (PrivilegeManagementException e) {
            // not checking id in toString(), just validating rest of error
            // message due to id mismatch on CI
            boolean checkMessage = (e.getMessage().contains("Unable to persist Subject(s) for zone") || (e.getMessage()
                    .contains("Duplicate Subject(s)")));
            Assert.assertTrue(checkMessage, "Invalid Error Message: " + e.getMessage());
            Assert.assertEquals(this.service.getSubjects().size(), 0);
            return;
        }

        Assert.fail("Expected PrivilegeManagementException to be thrown.");
    }

    public void testCreateMultipleResourceWithConstraintViolationResourceIdentifier() {

        List<BaseResource> resourceList = new ArrayList<>();

        String resourceIdentifier = "Brittany123";

        resourceList.add(createResource(resourceIdentifier));
        resourceList.add(createResource(resourceIdentifier));

        try {
            this.service.appendResources(resourceList);

        } catch (PrivilegeManagementException e) {
            boolean checkMessage = (e.getMessage().contains("Unable to persist Resource(s) for zone") || (e.getMessage()
                    .contains("Duplicate Resource(s)")));
            Assert.assertTrue(checkMessage, "Invalid Error Message: " + e.getMessage());
            Assert.assertEquals(this.service.getResources().size(), 0);
            return;
        }
        Assert.fail("Expected PrivilegeManagementException to be thrown.");
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

}
