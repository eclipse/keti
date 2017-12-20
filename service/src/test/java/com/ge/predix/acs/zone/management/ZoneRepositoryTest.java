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

package com.ge.predix.acs.zone.management;

import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ge.predix.acs.config.InMemoryDataSourceConfig;
import com.ge.predix.acs.encryption.Encryptor;
import com.ge.predix.acs.rest.AttributeAdapterConnection;
import com.ge.predix.acs.rest.AttributeConnector;
import com.ge.predix.acs.testutils.TestActiveProfilesResolver;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.management.dao.ZoneRepository;

@EnableAutoConfiguration
@ContextConfiguration(classes = { InMemoryDataSourceConfig.class, Encryptor.class })
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@TestPropertySource("/application.properties")
public class ZoneRepositoryTest extends AbstractTransactionalTestNGSpringContextTests {

    @Autowired
    private ZoneRepository zoneRepository;

    @Test
    public void testAddConnector() throws Exception {
        createZoneWithConnectorAndAssert();
    }

    @Test
    public void testUpdateConnector() throws Exception {
        ZoneEntity zone = createZoneWithConnectorAndAssert();

        AttributeConnector expectedConnector = new AttributeConnector();
        expectedConnector.setIsActive(true);
        expectedConnector.setAdapters(Collections.singleton(new AttributeAdapterConnection("http://some-adapter.com",
                "http://some-uaa.com", "some-client", "some-secret")));
        zone.setResourceAttributeConnector(expectedConnector);

        this.zoneRepository.save(zone);

        // Assert that zone connectors and adapters are updated
        AttributeConnector actualConnector = this.zoneRepository.getByName(zone.getName())
                .getResourceAttributeConnector();
        Assert.assertEquals(actualConnector, expectedConnector);
        Assert.assertEquals(actualConnector.getAdapters(), expectedConnector.getAdapters());
    }

    @Test
    public void testDeleteConnector() throws Exception {
        ZoneEntity zone = createZoneWithConnectorAndAssert();

        zone.setResourceAttributeConnector(null);
        this.zoneRepository.save(zone);
        Assert.assertNull(this.zoneRepository.getByName(zone.getName()).getResourceAttributeConnector());
    }

    private ZoneEntity createZoneWithConnectorAndAssert() throws Exception {
        AttributeConnector expectedConnector = new AttributeConnector();
        Set<AttributeAdapterConnection> expectedAdapters = Collections.singleton(
                new AttributeAdapterConnection("http://my-adapter.com", "http://my-uaa", "my-client", "my-secret"));
        expectedConnector.setAdapters(expectedAdapters);
        expectedConnector.setMaxCachedIntervalMinutes(24);
        ZoneEntity zone = new ZoneEntity();
        zone.setName("azone");
        zone.setSubdomain("asubdomain");
        zone.setDescription("adescription");
        zone.setResourceAttributeConnector(expectedConnector);
        this.zoneRepository.save(zone);
        ZoneEntity acutalZone = this.zoneRepository.getByName("azone");
        Assert.assertEquals(acutalZone.getSubjectAttributeConnector(), null);
        Assert.assertEquals(acutalZone.getResourceAttributeConnector(), expectedConnector);
        Assert.assertEquals(acutalZone.getResourceAttributeConnector().getAdapters(), expectedAdapters);
        return zone;
    }
}
