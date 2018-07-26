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

package org.eclipse.keti.acs.attribute.readers

import org.eclipse.keti.acs.attribute.cache.AttributeCache
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.rest.AttributeAdapterConnection
import org.eclipse.keti.acs.rest.attribute.adapter.AttributesResponse
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.testng.annotations.DataProvider

object ExternalAttributeReaderHelper {

    internal fun setupMockedAdapterResponse(
        externalAttributeReader: ExternalAttributeReader,
        attributeCache: AttributeCache,
        identifier: String
    ) {
        val attributeResponseResponseEntity = ResponseEntity(
            AttributesResponse(
                setOf(Attribute("issuer", "name", "value")),
                "attributesResponse"
            ), HttpStatus.OK
        )
        val mockRestTemplate = Mockito.mock(OAuth2RestTemplate::class.java)
        Mockito.doReturn(attributeResponseResponseEntity).`when`(mockRestTemplate)
            .getForEntity(Mockito.anyString(), Mockito.eq(AttributesResponse::class.java))

        Mockito
            .doReturn(setOf(AttributeAdapterConnection("https://my-url", "https://my-uaa", "my-client", "my-secret")))
            .`when`(externalAttributeReader).attributeAdapterConnections

        Mockito.doReturn(mockRestTemplate).`when`(externalAttributeReader)
            .getAdapterOauth2RestTemplate(Mockito.any(AttributeAdapterConnection::class.java))
        Mockito.`when`<CachedAttributes>(attributeCache.getAttributes(identifier)).thenReturn(null)
    }

    @DataProvider
    internal fun attributeSizeConstraintDataProvider(): Array<Array<Any?>> {
        return arrayOf<Array<Any?>>(arrayOf(0, 100), arrayOf(100, 0))
    }

} // Hiding constructor because this a test utility class
