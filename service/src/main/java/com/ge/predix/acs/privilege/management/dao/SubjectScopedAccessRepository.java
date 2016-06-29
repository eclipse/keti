package com.ge.predix.acs.privilege.management.dao;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public interface SubjectScopedAccessRepository {
    SubjectEntity getByZoneAndSubjectIdentifierAndScopes(final ZoneEntity zone, final String resourceIdentifier,
            final Set<Attribute> scopes);
}
