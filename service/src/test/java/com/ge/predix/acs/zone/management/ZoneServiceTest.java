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

import static org.mockito.Matchers.anyList;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.SpringSecurityPolicyContextResolver;
import com.ge.predix.acs.config.GraphBeanDefinitionRegistryPostProcessor;
import com.ge.predix.acs.config.GraphConfig;
import com.ge.predix.acs.config.InMemoryDataSourceConfig;
import com.ge.predix.acs.privilege.management.dao.GraphResourceRepository;
import com.ge.predix.acs.privilege.management.dao.GraphSubjectRepository;
import com.ge.predix.acs.privilege.management.dao.ResourceRepository;
import com.ge.predix.acs.privilege.management.dao.SubjectRepository;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.testutils.TestActiveProfilesResolver;
import com.ge.predix.acs.testutils.TestUtils;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.management.dao.ZoneRepository;
import com.ge.predix.acs.zone.resolver.SpringSecurityZoneResolver;
import com.ge.predix.uaa.token.lib.ZoneOAuth2Authentication;

/**
 *
 * @author 212319607
 */

@ContextConfiguration(
        classes = { GraphBeanDefinitionRegistryPostProcessor.class, GraphConfig.class, GraphResourceRepository.class,
                GraphSubjectRepository.class, InMemoryDataSourceConfig.class, SpringSecurityPolicyContextResolver.class,
                SpringSecurityZoneResolver.class, ZoneServiceImpl.class })
@TestPropertySource("/application.properties")
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@Test
@SuppressWarnings("nls")
public class ZoneServiceTest extends AbstractTransactionalTestNGSpringContextTests {

    @Autowired
    private ZoneService zoneService;

    private final TestUtils testUtils = new TestUtils();

    @BeforeMethod
    public void createSampleData() {
        this.zoneService.upsertZone(this.testUtils.createZone("zone1", "subdomain1"));
        ZoneOAuth2Authentication acsAuth = new ZoneOAuth2Authentication(Mockito.mock(OAuth2Request.class), null,
                "subdomain1");
        SecurityContextHolder.getContext().setAuthentication(acsAuth);
    }

    @Test(dataProvider = "badSubDomainDataProvider")
    public void testZoneCreationWithIllegalZoneNames(final String zoneSubdomain) {
        try {
            this.zoneService.upsertZone(new Zone("illegal_zone", zoneSubdomain, "desc"));
            Assert.fail("Expected an exception for invalid zone name");
        } catch (ZoneManagementException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid Zone Subdomain"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Subject Deletion Failed!")
    public void deleteZoneFailsWhenDeleteSubjectFails() {
        ZoneRepository zoneRepository = Mockito.mock(ZoneRepository.class);
        Mockito.when(zoneRepository.getByName("test-zone")).thenReturn(new ZoneEntity(1L, "test-zone"));
        ResourceRepository resourceRepository = Mockito.mock(ResourceRepository.class);
        Mockito.doNothing().when(resourceRepository).delete(anyList());
        SubjectRepository subjectRepository = Mockito.mock(SubjectRepository.class);
        Mockito.doThrow(new RuntimeException("Subject Deletion Failed!")).when(subjectRepository).delete(anyList());
        this.testUtils.setField(this.zoneService, "subjectRepository", subjectRepository);
        this.testUtils.setField(this.zoneService, "resourceRepository", resourceRepository);
        this.testUtils.setField(this.zoneService, "zoneRepository", zoneRepository);
        this.zoneService.deleteZone("test-zone");
    }

    @DataProvider(name = "badSubDomainDataProvider")
    private Object[][] badSubDomainDataProvider() {
        return new String[][] { { "-baddomain" }, { "baddomain-" }, { "bad.domain" }, { ".baddomain" },
                { "baddomain." }, { "bad$#%#$" }, { "_baddomain" } };
    }
}
