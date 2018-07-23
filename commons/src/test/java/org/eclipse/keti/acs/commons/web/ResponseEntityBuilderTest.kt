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

package org.eclipse.keti.acs.commons.web

import org.springframework.http.HttpStatus
import org.testng.Assert
import org.testng.annotations.Test

class ResponseEntityBuilderTest {

    @Test
    fun testCreated() {
        val created = created<Any>()
        Assert.assertNotNull(created)
        Assert.assertNull(created.body)
        Assert.assertEquals(created.statusCode, HttpStatus.CREATED)
    }

    @Test
    fun testCreatedWithLocation() {
        val created = created<Any>("/report/1", java.lang.Boolean.FALSE)

        Assert.assertNotNull(created)
        Assert.assertNull(created.body)
        Assert.assertEquals(created.statusCode, HttpStatus.CREATED)

        Assert.assertNotNull(created.headers)
        val location = created.headers.location
        Assert.assertEquals(location.path, "/report/1")
    }

    @Test
    fun testUpdatedWithLocation() {
        val created = created<Any>("/report/1", java.lang.Boolean.TRUE)

        Assert.assertNotNull(created)
        Assert.assertNull(created.body)
        Assert.assertEquals(created.statusCode, HttpStatus.NO_CONTENT)

        Assert.assertNotNull(created.headers)
        val location = created.headers.location
        Assert.assertEquals(location.path, "/report/1")
    }

    @Test
    fun testOk() {
        val ok = ok<Any>()
        Assert.assertNotNull(ok)
        Assert.assertNull(ok.body)
        Assert.assertEquals(ok.statusCode, HttpStatus.OK)
    }

    @Test
    fun testOkWithContent() {
        val content = "PredixRocks"
        val ok = ok(content)
        Assert.assertNotNull(ok)
        Assert.assertEquals(ok.statusCode, HttpStatus.OK)
        Assert.assertEquals(ok.body, "PredixRocks")

    }

    @Test
    fun testDeleted() {
        val deleted = noContent()
        Assert.assertNotNull(deleted)
        Assert.assertNull(deleted.body)
        Assert.assertEquals(deleted.statusCode, HttpStatus.NO_CONTENT)
    }

}
