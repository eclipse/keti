package com.ge.predix.acs.attribute.readers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.rest.BaseResource;

@Test
public class DefaultResourceAttributeReaderTest {
    @Mock
    private PrivilegeManagementService privilegeManagementService;

    @Autowired
    @InjectMocks
    private PrivilegeServiceResourceAttributeReader defaultResourceAttributeReader;

    @BeforeMethod
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAttributes() throws Exception {
        Set<Attribute> resourceAttributes = new HashSet<>();
        resourceAttributes.add(new Attribute("https://acs.attributes.int", "site", "sanramon"));
        BaseResource testResource = new BaseResource("/test/resource", resourceAttributes);

        when(this.privilegeManagementService.getByResourceIdentifierWithInheritedAttributes(any()))
                .thenReturn(testResource);
        Assert.assertTrue(this.defaultResourceAttributeReader.getAttributes(testResource.getResourceIdentifier())
                .containsAll(resourceAttributes));
    }

    @Test
    public void testGetAttributesForNonExistentResource() throws Exception {
        when(this.privilegeManagementService.getByResourceIdentifierWithInheritedAttributes(any())).thenReturn(null);
        Assert.assertTrue(this.defaultResourceAttributeReader.getAttributes("nonexistentResource").isEmpty());
    }
}
