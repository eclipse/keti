/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.commons.policy.condition

import org.eclipse.keti.acs.model.Attribute
import org.testng.Assert
import org.testng.annotations.Test
import java.util.HashSet

/**
 * @author acs-engineers@ge.com
 */
class ResourceHandlerTest {

    @Test
    fun testNullAttributes() {
        val handler = ResourceHandler(null, null, null)
        Assert.assertNotNull(handler)
        Assert.assertEquals("", handler.uriVariable("site_id"))
        Assert.assertEquals("", handler.uriVariable(null))
    }

    @Test
    fun testPartialNullAttributes() {
        val handler = ResourceHandler(null, null, "a")
        Assert.assertNotNull(handler)
        Assert.assertEquals("", handler.uriVariable("site_id"))
        Assert.assertEquals("", handler.uriVariable(null))
    }

    @Test
    fun testUriVariableNegative() {
        val attributes = HashSet(
            listOf(Attribute("acs", "site", "boston"), Attribute("acs", "department", "sales"))
        )
        val handler = ResourceHandler(attributes, "", "")
        Assert.assertEquals("", handler.uriVariable(""))
        Assert.assertEquals("", handler.uriVariable(null))
    }

    @Test
    fun testUriVariablePositive() {
        val attributes = HashSet(
            listOf(Attribute("acs", "site", "boston"), Attribute("acs", "department", "sales"))
        )
        val handler = ResourceHandler(
            attributes,
            "http://assets.predix.io/site/boston/department/sales",
            "site/{site_id}/department/{department_id}"
        )
        Assert.assertEquals("boston", handler.uriVariable("site_id"))
        Assert.assertEquals("sales", handler.uriVariable("department_id"))
        Assert.assertNotEquals("hr", handler.uriVariable("department_id"))
    }
}
