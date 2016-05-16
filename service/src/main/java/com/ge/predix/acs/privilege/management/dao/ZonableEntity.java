package com.ge.predix.acs.privilege.management.dao;

import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public interface ZonableEntity {
    Long getId();
    void setId(Long id);
    ZoneEntity getZone();
    void setZone(ZoneEntity zone);
}
