package com.ge.predix.acs.model;

import java.util.Arrays;
import java.util.HashSet;

import org.testng.annotations.Test;

import com.ge.predix.acs.rest.Parent;

public class ParentTest {
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testMultipleScopesNotAllowed() {
        Attribute a1 = new Attribute("issuer", "a1");
        Attribute a2 = new Attribute("issuer", "a2");
        new Parent("testParent", new HashSet<Attribute>(Arrays.asList(new Attribute[] {  a1, a2 })));
    }

}
