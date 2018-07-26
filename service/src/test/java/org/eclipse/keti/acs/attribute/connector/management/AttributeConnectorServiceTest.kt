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

package org.eclipse.keti.acs.attribute.connector.management

import org.eclipse.keti.acs.attribute.readers.AttributeReaderFactory
import org.eclipse.keti.acs.rest.AttributeAdapterConnection
import org.eclipse.keti.acs.rest.AttributeConnector
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.eclipse.keti.acs.zone.management.dao.ZoneRepository
import org.eclipse.keti.acs.zone.resolver.ZoneResolver
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.util.HashSet

@Test
class AttributeConnectorServiceTest {

    @InjectMocks
    private lateinit var connectorService: AttributeConnectorServiceImpl
    @Mock
    private lateinit var zoneResolver: ZoneResolver
    @Mock
    private lateinit var zoneRepository: ZoneRepository
    @Mock
    private lateinit var attributeReaderFactory: AttributeReaderFactory

    private val validConnector: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            connector.adapters = validAdapter
            return arrayOf(connector)
        }

    private val validAdapter: Set<AttributeAdapterConnection>
        get() = setOf(
            AttributeAdapterConnection(
                "https://my-endpoint.com", "https://my-uaa.com",
                "my-client", "my-secret"
            )
        )

    private val adapterWithOnlyEndpointInsecure: Set<AttributeAdapterConnection>
        get() = setOf(
            AttributeAdapterConnection(
                "http://my-endpoint.com", "https://my-uaa.com",
                "my-client", "my-secret"
            )
        )

    private val adapterWithOnlyUaaTokenUrlInsecure: Set<AttributeAdapterConnection>
        get() = setOf(
            AttributeAdapterConnection(
                "https://my-endpoint.com", "http://my-uaa.com",
                "my-client", "my-secret"
            )
        )

    private val adapterWithOnlyEndpointHavingAMixedCaseScheme: Set<AttributeAdapterConnection>
        get() = setOf(
            AttributeAdapterConnection(
                "hTtPs://my-endpoint.com", "https://my-uaa.com",
                "my-client", "my-secret"
            )
        )

    private val adapterWithOnlyUaaTokenUrlHavingAMixedCaseScheme: Set<AttributeAdapterConnection>
        get() = setOf(
            AttributeAdapterConnection(
                "https://my-endpoint.com", "hTtPs://my-uaa.com",
                "my-client", "my-secret"
            )
        )

    private val adapterWithOnlyEndpointHavingNoScheme: Set<AttributeAdapterConnection>
        get() = setOf(
            AttributeAdapterConnection(
                "www.my-endpoint.com", "https://my-uaa.com",
                "my-client", "my-secret"
            )
        )

    private val adapterWithOnlyUaaTokenUrlHavingNoScheme: Set<AttributeAdapterConnection>
        get() = setOf(
            AttributeAdapterConnection(
                "https://my-endpoint.com", "www.my-uaa.com",
                "my-client", "my-secret"
            )
        )

    private val adapterWithoutClientSecret: Set<AttributeAdapterConnection>
        get() = setOf(AttributeAdapterConnection("https://my-endpoint.com", "https://my-uaa.com", "my-client", ""))

    private val adapterWithoutClientId: Set<AttributeAdapterConnection>
        get() = setOf(AttributeAdapterConnection("https://my-endpoint.com", "https://my-uaa.com", "", "my-secret"))

    private val adapterWithEmptyUaaTokenUrl: Set<AttributeAdapterConnection>
        get() = setOf(AttributeAdapterConnection("https://my-endpoint.com", "", "my-client", "my-secret"))

    private val adapterWithEmptyEndpointUrl: Set<AttributeAdapterConnection>
        get() = setOf(AttributeAdapterConnection("", "https://my-uaa.com", "my-client", "my-secret"))

    private val adapterWithNullUaaTokenUrl: Set<AttributeAdapterConnection>
        get() = setOf(AttributeAdapterConnection("https://my-endpoint.com", null, "my-client", "my-secret"))

    private val adapterWithNullEndpointUrl: Set<AttributeAdapterConnection>
        get() = setOf(AttributeAdapterConnection(null, "https://my-uaa.com", "my-client", "my-secret"))

    private val connectorWithAdapterEndpointInsecure: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            connector.adapters = adapterWithOnlyEndpointInsecure
            return arrayOf(connector)
        }

    private val connectorWithAdapterUaaTokenUrlInsecure: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            connector.adapters = adapterWithOnlyUaaTokenUrlInsecure
            return arrayOf(connector)
        }

    private val connectorWithAdapterEndpointHavingAMixedCaseScheme: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            connector.adapters = adapterWithOnlyEndpointHavingAMixedCaseScheme
            return arrayOf(connector)
        }

    private val connectorWithAdapterUaaTokenUrlHavingAMixedCaseScheme: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            connector.adapters = adapterWithOnlyUaaTokenUrlHavingAMixedCaseScheme
            return arrayOf(connector)
        }

    private val connectorWithAdapterEndpointHavingNoScheme: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            connector.adapters = adapterWithOnlyEndpointHavingNoScheme
            return arrayOf(connector)
        }

    private val connectorWithAdapterUaaTokenUrlHavingNoScheme: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            connector.adapters = adapterWithOnlyUaaTokenUrlHavingNoScheme
            return arrayOf(connector)
        }

    private val connectorWithoutAdapterClientSecret: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            connector.adapters = adapterWithoutClientSecret
            return arrayOf(connector)
        }

    private val connectorWithoutAdapterClientId: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            connector.adapters = adapterWithoutClientId
            return arrayOf(connector)
        }

    private val connectorWithEmptyAdapterUaaTokenUrl: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            connector.adapters = adapterWithEmptyUaaTokenUrl
            return arrayOf(connector)
        }

    private val connectorWithEmptyAdapterEndpointUrl: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            connector.adapters = adapterWithEmptyEndpointUrl
            return arrayOf(connector)
        }

    private val connectorWithNullAdapterUaaTokenUrl: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            connector.adapters = adapterWithNullUaaTokenUrl
            return arrayOf(connector)
        }

    private val connectorWithNullAdapterEndpointUrl: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            connector.adapters = adapterWithNullEndpointUrl
            return arrayOf(connector)
        }

    private val connectorWithTwoAdapters: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            val adapters = HashSet<AttributeAdapterConnection>()
            adapters.add(AttributeAdapterConnection("https://my-endpoint", "https://my-uaa", "my-client", "my-secret"))
            adapters.add(
                AttributeAdapterConnection("https://my-endpoint2", "https://my-uaa2", "my-client2", "my-secret2")
            )
            connector.adapters = adapters
            return arrayOf(connector)
        }

    private val connectorWithCachedIntervalBelowThreshold: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 10
            connector.adapters = validAdapter
            return arrayOf(connector)
        }

    private val connectorWithoutAdapter: Array<Any?>
        get() {
            val connector = AttributeConnector()
            connector.maxCachedIntervalMinutes = 100
            return arrayOf(connector)
        }

    private val nullConnector: Array<Any?>
        get() = arrayOf(null)

    @BeforeMethod
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        this.connectorService.setEncryptionKey("1234567890123456")
    }

    @Test(dataProvider = "validConnectorProvider")
    fun testCreateResourceConnector(expectedConnector: AttributeConnector) {
        val zoneEntity = ZoneEntity()
        Mockito.doReturn(zoneEntity).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        Assert.assertTrue(this.connectorService.upsertResourceConnector(expectedConnector))
        Assert.assertEquals(this.connectorService.retrieveResourceConnector(), expectedConnector)
    }

    @Test(dataProvider = "validConnectorProvider")
    @Throws(Exception::class)
    fun testUpdateResourceConnector(expectedConnector: AttributeConnector) {
        val zoneEntity = ZoneEntity()
        zoneEntity.resourceAttributeConnector = AttributeConnector()
        Mockito.doReturn(zoneEntity).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        Assert.assertFalse(this.connectorService.upsertResourceConnector(expectedConnector))
        Assert.assertEquals(this.connectorService.retrieveResourceConnector(), expectedConnector)
    }

    @Test(dataProvider = "validConnectorProvider", expectedExceptions = [(AttributeConnectorException::class)])
    fun testUpsertResourceConnectorWhenSaveFails(connector: AttributeConnector) {
        val zoneEntity = ZoneEntity()
        Mockito.doReturn(zoneEntity).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        Mockito.doAnswer { _ -> throw Exception() }.`when`<ZoneRepository>(this.zoneRepository)
            .save(Mockito.any(ZoneEntity::class.java))
        this.connectorService.upsertResourceConnector(connector)
    }

    @Test(dataProvider = "badConnectorProvider", expectedExceptions = [(AttributeConnectorException::class)])
    fun testUpsertResourceConnectorWhenValidationFails(connector: AttributeConnector?) {
        Mockito.doReturn(ZoneEntity()).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        this.connectorService.upsertResourceConnector(connector)
    }

    @Test(dataProvider = "validConnectorProvider")
    @Throws(Exception::class)
    fun testGetResourceConnector(expectedConnector: AttributeConnector) {
        val zoneEntity = ZoneEntity()
        zoneEntity.resourceAttributeConnector = expectedConnector
        Mockito.doReturn(zoneEntity).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        this.connectorService.upsertResourceConnector(expectedConnector)
        Assert.assertEquals(this.connectorService.retrieveResourceConnector(), expectedConnector)
    }

    @Test
    fun testConnectorsWhichDoNotExist() {
        Mockito.doReturn(ZoneEntity()).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        Assert.assertNull(this.connectorService.retrieveResourceConnector())
        Assert.assertNull(this.connectorService.retrieveSubjectConnector())
        Assert.assertFalse(this.connectorService.deleteResourceConnector())
        Assert.assertFalse(this.connectorService.deleteSubjectConnector())
    }

    @Test(dataProvider = "validConnectorProvider")
    @Throws(Exception::class)
    fun testDeleteResourceConnector(connector: AttributeConnector) {
        val zoneEntity = ZoneEntity()
        zoneEntity.resourceAttributeConnector = connector
        Mockito.doReturn(zoneEntity).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        Assert.assertTrue(this.connectorService.deleteResourceConnector())
        Assert.assertNull(this.connectorService.retrieveResourceConnector())
    }

    @Test(expectedExceptions = [(AttributeConnectorException::class)])
    @Throws(Exception::class)
    fun testDeleteResourceConnectorWhenSaveFails() {
        val zoneEntity = ZoneEntity()
        zoneEntity.resourceAttributeConnector = AttributeConnector()
        Mockito.doReturn(zoneEntity).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        Mockito.doAnswer { _ -> throw Exception() }.`when`<ZoneRepository>(this.zoneRepository)
            .save(Mockito.any(ZoneEntity::class.java))
        this.connectorService.deleteResourceConnector()
    }

    @Test(dataProvider = "validConnectorProvider")
    fun testCreateSubjectConnector(expectedConnector: AttributeConnector) {
        val zoneEntity = ZoneEntity()
        Mockito.doReturn(zoneEntity).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        Assert.assertTrue(this.connectorService.upsertSubjectConnector(expectedConnector))
        Assert.assertEquals(this.connectorService.retrieveSubjectConnector(), expectedConnector)
    }

    @Test(dataProvider = "validConnectorProvider")
    @Throws(Exception::class)
    fun testUpdateSubjectConnector(expectedConnector: AttributeConnector) {
        val zoneEntity = ZoneEntity()
        zoneEntity.subjectAttributeConnector = AttributeConnector()
        Mockito.doReturn(zoneEntity).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        Assert.assertFalse(this.connectorService.upsertSubjectConnector(expectedConnector))
        Assert.assertEquals(this.connectorService.retrieveSubjectConnector(), expectedConnector)
    }

    @Test(dataProvider = "validConnectorProvider", expectedExceptions = [(AttributeConnectorException::class)])
    fun testUpsertSubjectConnectorWhenSaveFails(connector: AttributeConnector) {
        val zoneEntity = ZoneEntity()
        Mockito.doReturn(zoneEntity).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        Mockito.doAnswer { _ -> throw Exception() }.`when`<ZoneRepository>(this.zoneRepository)
            .save(Mockito.any(ZoneEntity::class.java))
        this.connectorService.upsertSubjectConnector(connector)
    }

    @Test(dataProvider = "badConnectorProvider", expectedExceptions = [(AttributeConnectorException::class)])
    fun testUpsertSubjectConnectorWhenValidationFails(connector: AttributeConnector?) {
        Mockito.doReturn(ZoneEntity()).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        this.connectorService.upsertSubjectConnector(connector)
    }

    @Test(dataProvider = "validConnectorProvider")
    @Throws(Exception::class)
    fun testGetSubjectConnector(expectedConnector: AttributeConnector) {
        val zoneEntity = ZoneEntity()
        zoneEntity.subjectAttributeConnector = expectedConnector
        Mockito.doReturn(zoneEntity).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        this.connectorService.upsertSubjectConnector(expectedConnector)
        Assert.assertEquals(this.connectorService.retrieveSubjectConnector(), expectedConnector)
    }

    @Test(dataProvider = "validConnectorProvider")
    @Throws(Exception::class)
    fun testDeleteSubjectConnector(connector: AttributeConnector) {
        val zoneEntity = ZoneEntity()
        zoneEntity.subjectAttributeConnector = connector
        Mockito.doReturn(zoneEntity).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        Assert.assertTrue(this.connectorService.deleteSubjectConnector())
        Assert.assertNull(this.connectorService.retrieveSubjectConnector())
    }

    @Test(expectedExceptions = [(AttributeConnectorException::class)])
    @Throws(Exception::class)
    fun testDeleteSubjectConnectorWhenSaveFails() {
        val zoneEntity = ZoneEntity()
        zoneEntity.subjectAttributeConnector = AttributeConnector()
        Mockito.doReturn(zoneEntity).`when`<ZoneResolver>(this.zoneResolver).zoneEntityOrFail
        Mockito.doAnswer { _ -> throw Exception() }.`when`<ZoneRepository>(this.zoneRepository)
            .save(Mockito.any(ZoneEntity::class.java))
        this.connectorService.deleteSubjectConnector()
    }

    @DataProvider
    private fun validConnectorProvider(): Array<Array<Any?>> {
        return arrayOf(
            validConnector,
            connectorWithAdapterEndpointHavingAMixedCaseScheme,
            connectorWithAdapterUaaTokenUrlHavingAMixedCaseScheme
        )
    }

    @DataProvider
    private fun badConnectorProvider(): Array<Array<Any?>> {
        return arrayOf(
            nullConnector,
            connectorWithoutAdapter,
            connectorWithCachedIntervalBelowThreshold,
            connectorWithTwoAdapters,
            connectorWithEmptyAdapterEndpointUrl,
            connectorWithEmptyAdapterUaaTokenUrl,
            connectorWithNullAdapterUaaTokenUrl,
            connectorWithNullAdapterEndpointUrl,
            connectorWithoutAdapterClientId,
            connectorWithoutAdapterClientSecret,
            connectorWithAdapterEndpointInsecure,
            connectorWithAdapterUaaTokenUrlInsecure,
            connectorWithAdapterEndpointHavingNoScheme,
            connectorWithAdapterUaaTokenUrlHavingNoScheme
        )
    }
}
