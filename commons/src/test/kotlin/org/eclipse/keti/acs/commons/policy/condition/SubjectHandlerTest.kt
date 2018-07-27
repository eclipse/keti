/*******************************************************************************
 * Copyright 2018 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.commons.policy.condition

import org.eclipse.keti.acs.model.Attribute
import org.testng.Assert
import org.testng.annotations.Test
import java.util.Collections

/**
 *
 * @author acs-engineers@ge.com
 */
class SubjectHandlerTest {

    @Test
    fun testNullAttributes() {

        val subjectHandler = SubjectHandler(null)
        Assert.assertNotNull(subjectHandler)
    }

    @Test
    fun testEmptyAttributes() {
        val subjectHandler = SubjectHandler(Collections.emptySet())
        Assert.assertNotNull(subjectHandler)
        Assert.assertTrue(subjectHandler.attributes("acs", "site").isEmpty())
    }

    @Test
    fun testValidAttributes() {

        val attributes = setOf(
            Attribute("acs", "site", "boston"), Attribute("acs", "department", "sales")
        )
        val subjectHandler = SubjectHandler(attributes)
        Assert.assertNotNull(subjectHandler)
        Assert.assertTrue(subjectHandler.attributes("acs", "site").contains("boston"))
        Assert.assertTrue(subjectHandler.attributes("acs", "department").contains("sales"))
        Assert.assertFalse(subjectHandler.attributes("acs", "site").contains("newyork"))
        Assert.assertTrue(subjectHandler.attributes("acs", "region").isEmpty())
    }
}
