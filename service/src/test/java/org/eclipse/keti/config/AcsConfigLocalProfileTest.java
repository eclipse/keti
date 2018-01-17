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

package org.eclipse.keti.config;

import javax.sql.DataSource;

import org.eclipse.keti.testutils.TestActiveProfilesResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

@ContextConfiguration(classes = { InMemoryDataSourceConfig.class })
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
public class AcsConfigLocalProfileTest extends AbstractTestNGSpringContextTests {
    @Autowired
    private DataSource dataSource;

    @Test
    public void testLocalProfile() {
        Assert.assertTrue(EmbeddedDatabase.class.isAssignableFrom(this.dataSource.getClass()));
    }
}
