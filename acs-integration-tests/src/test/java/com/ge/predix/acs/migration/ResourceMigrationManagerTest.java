package com.ge.predix.acs.migration;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ge.predix.acs.privilege.management.dao.ResourceMigrationManager;
import com.ge.predix.test.utils.ZoneHelper;

public class ResourceMigrationManagerTest {
    @Autowired
    ResourceMigrationManager resourceMigrationManager;

    @Autowired
    private ZoneHelper zoneHelper;

    @Autowired
    public AttributeMigrationTest attributeMigrationTest;

    @Test
    public void doMigrationTest() {
        attributeMigrationTest.pushResourceAndSubjectAttributesForMigration(this.zoneHelper.getZone1Url());
        int resourceCount = resourceMigrationManager.doMigration();
        Assert.assertEquals(resourceCount, 3);
    }
}
