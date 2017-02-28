package com.ge.predix.acs.attribute.readers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.rest.BaseSubject;

@Component
public class PrivilegeServiceSubjectAttributeReader implements SubjectAttributeReader {
    @Autowired
    private PrivilegeManagementService privilegeManagementService;

    @Override
    public Set<Attribute> getAttributes(final String identifier) {
        return this.getAttributesByScope(identifier, Collections.emptySet());
    }

    @Override
    public Set<Attribute> getAttributesByScope(final String identifier, final Set<Attribute> scopes) {
        Set<Attribute> subjectAttributes = Collections.emptySet();
        BaseSubject subject = this.privilegeManagementService.getBySubjectIdentifierAndScopes(identifier, scopes);
        if (null != subject) {
            subjectAttributes = Collections.unmodifiableSet(new HashSet<>(subject.getAttributes()));
        }
        return subjectAttributes;
    }
}
