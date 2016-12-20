package com.ge.predix.acs.privilege.management.dao;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;

import java.util.Set;

public interface SubjectHierarchicalRepository {
    SubjectEntity getSubjectWithInheritedAttributesForScopes(ZoneEntity zone, String subjectIdentifier,
                                                             Set<Attribute> scopes);

    SubjectEntity getSubjectWithInheritedAttributes(ZoneEntity zone, String subjectIdentifier);

    Set<String> getSubjectEntityAndDescendantsIds(SubjectEntity entity); 
}