package com.ge.predix.acs.zone.management;

import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ge.predix.acs.config.InMemoryDataSourceConfig;
import com.ge.predix.acs.testutils.TestActiveProfilesResolver;
import com.ge.predix.acs.zone.management.dao.AttributeAdapterConnectionEntity;
import com.ge.predix.acs.zone.management.dao.AttributeAdapterConnectionRepository;
import com.ge.predix.acs.zone.management.dao.AttributeConnectorEntity;
import com.ge.predix.acs.zone.management.dao.AttributeConnectorRepository;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.management.dao.ZoneRepository;

@EnableAutoConfiguration
@ContextConfiguration(classes = { InMemoryDataSourceConfig.class })
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
public class ZoneRepositoryTest extends AbstractTransactionalTestNGSpringContextTests {

    @Value("ADAPTER_CLIENT_SECRET")
    private String assetAdapterClientSecret;

    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private AttributeConnectorRepository connectorRepository;
    @Autowired
    private AttributeAdapterConnectionRepository adapterRepository;

    @Test
    public void testAddConnector() {
        createZoneWithConnectorAndAssert();
    }

    @Test
    public void testUpdateConnector() {
        ZoneEntity zone = createZoneWithConnectorAndAssert();

        long connectorIdBeforeUpdate = zone.getResourceAttributeConnector().getId();
        long adapterIdBeforeUpdate = zone.getResourceAttributeConnector().getAttributeAdapterConnections().iterator()
                .next().getId();
        AttributeConnectorEntity expectedConnector = new AttributeConnectorEntity();
        expectedConnector.setAttributeAdapterConnections(Collections.singleton(new AttributeAdapterConnectionEntity(
                expectedConnector, "http://some-adapter.com", "http://some-uaa.com", "some-client", "some-secret")));
        zone.setResourceAttributeConnector(expectedConnector);

        this.zoneRepository.save(zone);

        // Assert that zone connectors and adapters are updated
        AttributeConnectorEntity actualConnector = this.zoneRepository.getByName(zone.getName())
                .getResourceAttributeConnector();
        Assert.assertEquals(actualConnector, expectedConnector);
        Assert.assertEquals(actualConnector.getAttributeAdapterConnections(),
                expectedConnector.getAttributeAdapterConnections());
        // Assert that previous connectors and adapters are deleted
        Assert.assertNull(this.connectorRepository.findOne(connectorIdBeforeUpdate));
        Assert.assertNull(this.adapterRepository.findOne(adapterIdBeforeUpdate));
    }

    @Test
    public void testDeleteConnector() {
        ZoneEntity zone = createZoneWithConnectorAndAssert();

        long adapterId = zone.getResourceAttributeConnector().getAttributeAdapterConnections().iterator().next()
                .getId();
        long connectorId = zone.getResourceAttributeConnector().getId();
        zone.setResourceAttributeConnector(null);
        this.zoneRepository.save(zone);
        Assert.assertNull(this.zoneRepository.getByName(zone.getName()).getResourceAttributeConnector());
        Assert.assertNull(this.connectorRepository.findOne(connectorId));
        Assert.assertNull(this.adapterRepository.findOne(adapterId));
    }

    @Test
    public void testDeleteConnectorWithCascade() {
        ZoneEntity zone = createZoneWithConnectorAndAssert();

        long adapterId = zone.getResourceAttributeConnector().getAttributeAdapterConnections().iterator().next()
                .getId();
        long connectorId = zone.getResourceAttributeConnector().getId();
        this.zoneRepository.delete(zone);
        Assert.assertNull(this.zoneRepository.getByName(zone.getName()));
        Assert.assertNull(this.connectorRepository.findOne(connectorId));
        Assert.assertNull(this.adapterRepository.findOne(adapterId));
    }

    @Test
    public void testEnvClientSecret() {
        ZoneEntity zone = createZoneWithConnectorAndAssert();

        Assert.assertEquals(zone.getResourceAttributeConnector().getAttributeAdapterConnections().iterator().next()
                .getUaaClientSecret(), null);
    }

    private ZoneEntity createZoneWithConnectorAndAssert() {
        AttributeConnectorEntity expectedConnector = new AttributeConnectorEntity();
        Set<AttributeAdapterConnectionEntity> expectedAdapters = Collections
                .singleton(new AttributeAdapterConnectionEntity(expectedConnector, "http://my-adapter.com",
                        "http://my-uaa", "my-client", "my-secret"));
        expectedConnector.setAttributeAdapterConnections(expectedAdapters);
        expectedConnector.setCachedIntervalMinutes(24);
        ZoneEntity zone = new ZoneEntity();
        zone.setName("azone");
        zone.setSubdomain("asubdomain");
        zone.setDescription("adescription");
        zone.setResourceAttributeConnector(expectedConnector);
        this.zoneRepository.save(zone);
        ZoneEntity acutalZone = this.zoneRepository.getByName("azone");
        Assert.assertEquals(acutalZone.getSubjectAttributeConnector(), null);
        Assert.assertEquals(acutalZone.getResourceAttributeConnector(), expectedConnector);
        Assert.assertEquals(acutalZone.getResourceAttributeConnector().getAttributeAdapterConnections(),
                expectedAdapters);
        return zone;
    }
}
