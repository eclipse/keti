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

package com.ge.predix.acs.request.context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ge.predix.acs.config.GraphConfig;
import com.ge.predix.acs.config.InMemoryDataSourceConfig;
import com.ge.predix.acs.request.context.AcsRequestContext.ACSRequestContextAttribute;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.testutils.TestActiveProfilesResolver;
import com.ge.predix.acs.testutils.TestUtils;
import com.ge.predix.acs.zone.management.ZoneService;
import com.ge.predix.acs.zone.management.ZoneServiceImpl;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;

@ContextConfiguration(
        classes = { InMemoryDataSourceConfig.class, ZoneServiceImpl.class, AcsRequestContextHolder.class,
                GraphConfig.class })
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@Test
public class AcsRequestContextHolderTest extends AbstractTestNGSpringContextTests {

    private static final String ZONE_NAME = "AcsRequestContextHolderTest";
    private static final String ZONE_NAME_SUFFIX = ".zone";
    private static final String ZONE_SUBDOMAIN_SUFFIX = "-subdomain";

    @Autowired
    private ZoneService zoneService;

    private final TestUtils testUtils = new TestUtils();
    private Zone testZone;

    @BeforeClass
    public void setup() {
        this.testZone = this.testUtils.setupTestZone("AcsRequestContextHolderTest", this.zoneService);
    }

    @AfterClass
    public void cleanup() {
        this.zoneService.deleteZone(this.testZone.getName());
    }

    @Test
    public void testAcsRequestContextSet() {
        AcsRequestContext acsRequestContext = AcsRequestContextHolder.getAcsRequestContext();
        ZoneEntity zoneEntity = (ZoneEntity) acsRequestContext.get(ACSRequestContextAttribute.ZONE_ENTITY);
        Assert.assertEquals(zoneEntity.getName(), ZONE_NAME + ZONE_NAME_SUFFIX);
        Assert.assertEquals(zoneEntity.getSubdomain(), ZONE_NAME + ZONE_SUBDOMAIN_SUFFIX);
    }

    @Test
    public void testClearAcsRequestContext() {
        AcsRequestContextHolder.clear();
        Assert.assertNull(AcsRequestContextHolder.getAcsRequestContext());
    }
}