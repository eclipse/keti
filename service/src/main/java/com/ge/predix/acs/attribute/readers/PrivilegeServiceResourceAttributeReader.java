package com.ge.predix.acs.attribute.readers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.rest.BaseResource;

@Component
public class PrivilegeServiceResourceAttributeReader implements ResourceAttributeReader {
    @Autowired
    private PrivilegeManagementService privilegeManagementService;

    @Override
    public Set<Attribute> getAttributes(final String identifier) {
        Set<Attribute> resourceAttributes = Collections.emptySet();
        BaseResource resource =
            this.privilegeManagementService.getByResourceIdentifierWithInheritedAttributes(identifier);
        if (null != resource) {
            resourceAttributes = Collections.unmodifiableSet(new HashSet<>(resource.getAttributes()));
        }
        return resourceAttributes;
    }
}
