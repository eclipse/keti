package com.ge.predix.acs.attribute.connector.management;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.attribute.readers.AttributeReaderFactory;
import com.ge.predix.acs.rest.AttributeAdapterConnection;
import com.ge.predix.acs.rest.AttributeConnector;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.management.dao.ZoneRepository;
import com.ge.predix.acs.zone.resolver.ZoneResolver;

@Test
public class AttributeConnectorServiceTest {
    @InjectMocks
    private AttributeConnectorServiceImpl connectorService;
    @Mock
    private ZoneResolver zoneResolver;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private AttributeReaderFactory attributeReaderFactory;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.connectorService.setEncryptionKey("1234567890123456");
    }

    @Test(dataProvider = "validConnectorProvider")
    public void testCreateResourceConnector(final AttributeConnector expectedConnector) {
        ZoneEntity zoneEntity = new ZoneEntity();
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        Assert.assertTrue(this.connectorService.upsertResourceConnector(expectedConnector));
        Assert.assertEquals(this.connectorService.retrieveResourceConnector(), expectedConnector);
    }

    @Test(dataProvider = "validConnectorProvider")
    public void testUpdateResourceConnector(final AttributeConnector expectedConnector) throws Exception {
        ZoneEntity zoneEntity = new ZoneEntity();
        zoneEntity.setResourceAttributeConnector(new AttributeConnector());
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        Assert.assertFalse(this.connectorService.upsertResourceConnector(expectedConnector));
        Assert.assertEquals(this.connectorService.retrieveResourceConnector(), expectedConnector);
    }

    @Test(dataProvider = "validConnectorProvider", expectedExceptions = { AttributeConnectorException.class })
    public void testUpsertResourceConnectorWhenSaveFails(final AttributeConnector connector) {
        ZoneEntity zoneEntity = new ZoneEntity();
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        Mockito.doThrow(Exception.class).when(this.zoneRepository).save(Mockito.any(ZoneEntity.class));
        this.connectorService.upsertResourceConnector(connector);
    }

    @Test(dataProvider = "badConnectorProvider", expectedExceptions = { AttributeConnectorException.class })
    public void testUpsertResourceConnectorWhenValidationFails(final AttributeConnector connector) {
        Mockito.doReturn(new ZoneEntity()).when(this.zoneResolver).getZoneEntityOrFail();
        this.connectorService.upsertResourceConnector(connector);
    }

    @Test(dataProvider = "validConnectorProvider")
    public void testGetResourceConnector(final AttributeConnector expectedConnector) throws Exception {
        ZoneEntity zoneEntity = new ZoneEntity();
        zoneEntity.setResourceAttributeConnector(expectedConnector);
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        this.connectorService.upsertResourceConnector(expectedConnector);
        Assert.assertEquals(this.connectorService.retrieveResourceConnector(), expectedConnector);
    }

    @Test
    public void testConnectorsWhichDoNotExist() {
        Mockito.doReturn(new ZoneEntity()).when(this.zoneResolver).getZoneEntityOrFail();
        Assert.assertNull(this.connectorService.retrieveResourceConnector());
        Assert.assertNull(this.connectorService.retrieveSubjectConnector());
        Assert.assertFalse(this.connectorService.deleteResourceConnector());
        Assert.assertFalse(this.connectorService.deleteSubjectConnector());
    }

    @Test(dataProvider = "validConnectorProvider")
    public void testDeleteResourceConnector(final AttributeConnector connector) throws Exception {
        ZoneEntity zoneEntity = new ZoneEntity();
        zoneEntity.setResourceAttributeConnector(connector);
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        Assert.assertTrue(this.connectorService.deleteResourceConnector());
        Assert.assertNull(this.connectorService.retrieveResourceConnector());
    }

    @Test(expectedExceptions = { AttributeConnectorException.class })
    public void testDeleteResourceConnectorWhenSaveFails() throws Exception {
        ZoneEntity zoneEntity = new ZoneEntity();
        zoneEntity.setResourceAttributeConnector(new AttributeConnector());
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        Mockito.doThrow(Exception.class).when(this.zoneRepository).save(Mockito.any(ZoneEntity.class));
        this.connectorService.deleteResourceConnector();
    }

    @Test(dataProvider = "validConnectorProvider")
    public void testCreateSubjectConnector(final AttributeConnector expectedConnector) {
        ZoneEntity zoneEntity = new ZoneEntity();
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        Assert.assertTrue(this.connectorService.upsertSubjectConnector(expectedConnector));
        Assert.assertEquals(this.connectorService.retrieveSubjectConnector(), expectedConnector);
    }

    @Test(dataProvider = "validConnectorProvider")
    public void testUpdateSubjectConnector(final AttributeConnector expectedConnector) throws Exception {
        ZoneEntity zoneEntity = new ZoneEntity();
        zoneEntity.setSubjectAttributeConnector(new AttributeConnector());
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        Assert.assertFalse(this.connectorService.upsertSubjectConnector(expectedConnector));
        Assert.assertEquals(this.connectorService.retrieveSubjectConnector(), expectedConnector);
    }

    @Test(dataProvider = "validConnectorProvider", expectedExceptions = { AttributeConnectorException.class })
    public void testUpsertSubjectConnectorWhenSaveFails(final AttributeConnector connector) {
        ZoneEntity zoneEntity = new ZoneEntity();
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        Mockito.doThrow(Exception.class).when(this.zoneRepository).save(Mockito.any(ZoneEntity.class));
        this.connectorService.upsertSubjectConnector(connector);
    }

    @Test(dataProvider = "badConnectorProvider", expectedExceptions = { AttributeConnectorException.class })
    public void testUpsertSubjectConnectorWhenValidationFails(final AttributeConnector connector) {
        Mockito.doReturn(new ZoneEntity()).when(this.zoneResolver).getZoneEntityOrFail();
        this.connectorService.upsertSubjectConnector(connector);
    }

    @Test(dataProvider = "validConnectorProvider")
    public void testGetSubjectConnector(final AttributeConnector expectedConnector) throws Exception {
        ZoneEntity zoneEntity = new ZoneEntity();
        zoneEntity.setSubjectAttributeConnector(expectedConnector);
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        this.connectorService.upsertSubjectConnector(expectedConnector);
        Assert.assertEquals(this.connectorService.retrieveSubjectConnector(), expectedConnector);
    }

    @Test(dataProvider = "validConnectorProvider")
    public void testDeleteSubjectConnector(final AttributeConnector connector) throws Exception {
        ZoneEntity zoneEntity = new ZoneEntity();
        zoneEntity.setSubjectAttributeConnector(connector);
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        Assert.assertTrue(this.connectorService.deleteSubjectConnector());
        Assert.assertNull(this.connectorService.retrieveSubjectConnector());
    }

    @Test(expectedExceptions = { AttributeConnectorException.class })
    public void testDeleteSubjectConnectorWhenSaveFails() throws Exception {
        ZoneEntity zoneEntity = new ZoneEntity();
        zoneEntity.setSubjectAttributeConnector(new AttributeConnector());
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        Mockito.doThrow(Exception.class).when(this.zoneRepository).save(Mockito.any(ZoneEntity.class));
        this.connectorService.deleteSubjectConnector();
    }

    @DataProvider
    private Object[][] validConnectorProvider() {
        return new Object[][] { getValidConnector(), getConnectorWithAdapterEndpointHavingAMixedCaseScheme(),
                getConnectorWithAdapterUaaTokenUrlHavingAMixedCaseScheme() };
    }

    private Object[] getValidConnector() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(getValidAdapter());
        return new Object[] { connector };
    }

    private Set<AttributeAdapterConnection> getValidAdapter() {
        return Collections.singleton(new AttributeAdapterConnection("https://my-endpoint.com", "https://my-uaa.com",
                "my-client", "my-secret"));
    }

    @DataProvider
    private Object[][] badConnectorProvider() {
        return new Object[][] { getNullConnector(), getConnectorWithoutAdapter(),
                getConnectorWithCachedIntervalBelowThreshold(), getConnectorWithTwoAdapters(),
                getConnectorWithEmptyAdapterEndpointUrl(), getConnectorWithEmptyAdapterUaaTokenUrl(),
                getConnectorWithNullAdapterUaaTokenUrl(), getConnectorWithNullAdapterEndpointUrl(),
                getConnectorWithoutAdapterClientId(), getConnectorWithoutAdapterClientSecret(),
                getConnectorWithAdapterEndpointInsecure(), getConnectorWithAdapterUaaTokenUrlInsecure(),
                getConnectorWithAdapterEndpointHavingNoScheme(), getConnectorWithAdapterUaaTokenUrlHavingNoScheme() };
    }

    private Set<AttributeAdapterConnection> getAdapterWithOnlyEndpointInsecure() {
        return Collections.singleton(new AttributeAdapterConnection("http://my-endpoint.com", "https://my-uaa.com",
                "my-client", "my-secret"));
    }

    private Set<AttributeAdapterConnection> getAdapterWithOnlyUaaTokenUrlInsecure() {
        return Collections.singleton(new AttributeAdapterConnection("https://my-endpoint.com", "http://my-uaa.com",
                "my-client", "my-secret"));
    }

    private Set<AttributeAdapterConnection> getAdapterWithOnlyEndpointHavingAMixedCaseScheme() {
        return Collections.singleton(new AttributeAdapterConnection("hTtPs://my-endpoint.com", "https://my-uaa.com",
                "my-client", "my-secret"));
    }

    private Set<AttributeAdapterConnection> getAdapterWithOnlyUaaTokenUrlHavingAMixedCaseScheme() {
        return Collections.singleton(new AttributeAdapterConnection("https://my-endpoint.com", "hTtPs://my-uaa.com",
                "my-client", "my-secret"));
    }

    private Set<AttributeAdapterConnection> getAdapterWithOnlyEndpointHavingNoScheme() {
        return Collections.singleton(new AttributeAdapterConnection("www.my-endpoint.com", "https://my-uaa.com",
                "my-client", "my-secret"));
    }

    private Set<AttributeAdapterConnection> getAdapterWithOnlyUaaTokenUrlHavingNoScheme() {
        return Collections.singleton(new AttributeAdapterConnection("https://my-endpoint.com", "www.my-uaa.com",
                "my-client", "my-secret"));
    }

    private Set<AttributeAdapterConnection> getAdapterWithoutClientSecret() {
        return Collections.singleton(
                new AttributeAdapterConnection("https://my-endpoint.com", "https://my-uaa.com", "my-client", ""));
    }

    private Set<AttributeAdapterConnection> getAdapterWithoutClientId() {
        return Collections.singleton(
                new AttributeAdapterConnection("https://my-endpoint.com", "https://my-uaa.com", "", "my-secret"));
    }

    private Set<AttributeAdapterConnection> getAdapterWithEmptyUaaTokenUrl() {
        return Collections
                .singleton(new AttributeAdapterConnection("https://my-endpoint.com", "", "my-client", "my-secret"));
    }

    private Set<AttributeAdapterConnection> getAdapterWithEmptyEndpointUrl() {
        return Collections
                .singleton(new AttributeAdapterConnection("", "https://my-uaa.com", "my-client", "my-secret"));
    }

    private Set<AttributeAdapterConnection> getAdapterWithNullUaaTokenUrl() {
        return Collections
                .singleton(new AttributeAdapterConnection("https://my-endpoint.com", null, "my-client", "my-secret"));
    }

    private Set<AttributeAdapterConnection> getAdapterWithNullEndpointUrl() {
        return Collections
                .singleton(new AttributeAdapterConnection(null, "https://my-uaa.com", "my-client", "my-secret"));
    }

    private Object[] getConnectorWithAdapterEndpointInsecure() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(getAdapterWithOnlyEndpointInsecure());
        return new Object[] { connector };
    }

    private Object[] getConnectorWithAdapterUaaTokenUrlInsecure() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(getAdapterWithOnlyUaaTokenUrlInsecure());
        return new Object[] { connector };
    }

    private Object[] getConnectorWithAdapterEndpointHavingAMixedCaseScheme() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(getAdapterWithOnlyEndpointHavingAMixedCaseScheme());
        return new Object[] { connector };
    }

    private Object[] getConnectorWithAdapterUaaTokenUrlHavingAMixedCaseScheme() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(getAdapterWithOnlyUaaTokenUrlHavingAMixedCaseScheme());
        return new Object[] { connector };
    }

    private Object[] getConnectorWithAdapterEndpointHavingNoScheme() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(getAdapterWithOnlyEndpointHavingNoScheme());
        return new Object[] { connector };
    }

    private Object[] getConnectorWithAdapterUaaTokenUrlHavingNoScheme() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(getAdapterWithOnlyUaaTokenUrlHavingNoScheme());
        return new Object[] { connector };
    }

    private Object[] getConnectorWithoutAdapterClientSecret() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(getAdapterWithoutClientSecret());
        return new Object[] { connector };
    }

    private Object[] getConnectorWithoutAdapterClientId() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(getAdapterWithoutClientId());
        return new Object[] { connector };
    }

    private Object[] getConnectorWithEmptyAdapterUaaTokenUrl() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(getAdapterWithEmptyUaaTokenUrl());
        return new Object[] { connector };
    }

    private Object[] getConnectorWithEmptyAdapterEndpointUrl() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(getAdapterWithEmptyEndpointUrl());
        return new Object[] { connector };
    }

    private Object[] getConnectorWithNullAdapterUaaTokenUrl() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(getAdapterWithNullUaaTokenUrl());
        return new Object[] { connector };
    }

    private Object[] getConnectorWithNullAdapterEndpointUrl() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(getAdapterWithNullEndpointUrl());
        return new Object[] { connector };
    }

    private Object[] getConnectorWithTwoAdapters() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        Set<AttributeAdapterConnection> adapters = new HashSet<>();
        adapters.add(new AttributeAdapterConnection("https://my-endpoint", "https://my-uaa", "my-client", "my-secret"));
        adapters.add(
                new AttributeAdapterConnection("https://my-endpoint2", "https://my-uaa2", "my-client2", "my-secret2"));
        connector.setAdapters(adapters);
        return new Object[] { connector };
    }

    private Object[] getConnectorWithCachedIntervalBelowThreshold() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(10);
        connector.setAdapters(getValidAdapter());
        return new Object[] { connector };
    }

    private Object[] getConnectorWithoutAdapter() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        return new Object[] { connector };
    }

    private Object[] getNullConnector() {
        return new Object[] { null };
    }
}
