package com.ge.predix.acs.model;

import com.ge.predix.acs.rest.Parent;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;

public class ParentTest {
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMultipleScopesNotAllowed() {
        Attribute a1 = new Attribute("issuer", "a1");
        Attribute a2 = new Attribute("issuer", "a2");
        new Parent("testParent", new HashSet<Attribute>(Arrays.asList(new Attribute[] {  a1, a2 })));
    }

}
