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
package com.ge.predix.acs.service.policy.admin.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ge.predix.acs.config.InMemoryDataSourceConfig;
import com.ge.predix.acs.testutils.TestActiveProfilesResolver;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.management.dao.ZoneRepository;

@ContextConfiguration(classes = InMemoryDataSourceConfig.class)
@EnableAutoConfiguration
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
public class PolicySetRepositoryTest extends AbstractTransactionalTestNGSpringContextTests {
    private static final String SUBDOMAIN = "PolicySetRepositoryTest-acs";

    @Autowired
    private PolicySetRepository policySetRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Test
    public void testPersistPolicy() {

        ZoneEntity zone = createZone();
        this.zoneRepository.save(zone);

        PolicySetEntity policySetEntity = new PolicySetEntity(zone, "policy-set-2", "{}");
        PolicySetEntity savedPolicySet = this.policySetRepository.save(policySetEntity);
        Assert.assertEquals(this.policySetRepository.count(), 1);
        Assert.assertTrue(savedPolicySet.getId() > 0);
    }

    private ZoneEntity createZone() {
        ZoneEntity zone = new ZoneEntity();
        zone.setName("PolicySetRepositoryTest-ACS");
        zone.setSubdomain(SUBDOMAIN);
        zone.setDescription("PolicySetRepositoryTest zone description");
        return zone;
    }
}
