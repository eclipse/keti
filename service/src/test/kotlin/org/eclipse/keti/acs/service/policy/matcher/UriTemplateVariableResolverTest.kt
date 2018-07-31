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

package org.eclipse.keti.acs.service.policy.matcher

import org.springframework.web.util.UriTemplate
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

@Test
class UriTemplateVariableResolverTest {

    private val attributeUriResolver = UriTemplateVariableResolver()

    @Test(dataProvider = "uriDataProvider")
    fun testMatch(
        uri: String,
        attributeUriTemplate: UriTemplate,
        matchedURI: Any?
    ) {
        Assert.assertEquals(this.attributeUriResolver.resolve(uri, attributeUriTemplate, "attribute_uri"), matchedURI)
    }

    @DataProvider
    fun uriDataProvider(): Array<Array<out Any?>> {
        return arrayOf(

            arrayOf("/v1/site/123", UriTemplate("/v1{attribute_uri}"), "/site/123"),
            arrayOf("/v1/site/123/asset/345", UriTemplate("/v1{attribute_uri}/asset/345"), "/site/123"),
            arrayOf("/v1/site/123/asset/345", UriTemplate("/v1{attribute_uri}/asset/{site_id}"), "/site/123"),
            arrayOf("/v1/site/123/asset/345", UriTemplate("/v1/site/123{attribute_uri}"), "/asset/345"),
            arrayOf("/v1/site/123/asset/345", UriTemplate("/v1{attribute_uri}"), "/site/123/asset/345"),

            arrayOf(
                "/v1/site/123/asset/345/report",
                UriTemplate("/v1{attribute_uri}/report"),
                "/site/123/asset/345"
            ),

            // template doesnt match uri
            arrayOf("/v1/site/123/asset/345/report", UriTemplate("/v2{attribute_uri}"), null),
            // no attribute_uri variable in template
            arrayOf("/v1/site/123/asset/345/report", UriTemplate("/v1{non_existent_variable}"), null),
            // multiple attribute_uri variables in template
            arrayOf("/v1/site/123/asset/345", UriTemplate("/v1{attribute_uri}/{attribute_uri}"), "345")
        )
    }
}
