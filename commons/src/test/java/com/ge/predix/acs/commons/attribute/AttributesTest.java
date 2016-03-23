/*******************************************************************************
 * Copyright 2016 General Electric Company. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *******************************************************************************/
package com.ge.predix.acs.commons.attribute;

import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for Attributes class.
 *
 * @author 212314537
 */
@SuppressWarnings({ "javadoc", "nls" })
public class AttributesTest {
    private static final AttributeType GROUP_ATTR_TYPE = new AttributeType("https://localhost/acs", "group");
    private static final AttributeType ROLE_ATTR_TYPE = new AttributeType("https://localhost/acs", "role");

    @Test
    public void hasAttributeType() {
        Set<Attribute> attributeSet = new HashSet<>();
        Attribute attr1 = new Attribute(GROUP_ATTR_TYPE, "gog");
        attributeSet.add(attr1);

        Attributes attributes = new Attributes(attributeSet);
        AttributeType criteria = new AttributeType("https://localhost/acs", "group");
        Assert.assertTrue(attributes.hasAttributeType(criteria));
    }

    @Test
    public void hasAttributeTypes() {

        Set<Attribute> attributeSet = new HashSet<>();
        Attribute attr1 = new Attribute(GROUP_ATTR_TYPE, "gog");
        attributeSet.add(attr1);
        Attribute attr2 = new Attribute(ROLE_ATTR_TYPE, "dev");
        attributeSet.add(attr2);

        Attributes attributes = new Attributes(attributeSet);

        Set<AttributeType> criteria = new HashSet<>();
        AttributeType criteria1 = new AttributeType("https://localhost/acs", "group");
        criteria.add(criteria1);
        AttributeType criteria2 = new AttributeType("https://localhost/acs", "role");
        criteria.add(criteria2);
        Assert.assertTrue(attributes.hasAttributeTypes(criteria));
    }

    @Test
    public void hasAttributeTypeFailure() {

        Set<Attribute> attributeSet = new HashSet<>();
        Attribute attr1 = new Attribute(ROLE_ATTR_TYPE, "dev");
        attributeSet.add(attr1);

        Attributes attributes = new Attributes(attributeSet);
        AttributeType criteria = new AttributeType("https://localhost/acs", "group");
        Assert.assertFalse(attributes.hasAttributeType(criteria));
    }

    @Test
    public void hasAttribute() {

        Set<Attribute> attributeSet = new HashSet<>();
        Attribute attr1 = new Attribute(GROUP_ATTR_TYPE, "gog");
        attributeSet.add(attr1);

        Attributes attributes = new Attributes(attributeSet);
        Attribute criteria = new Attribute(GROUP_ATTR_TYPE, "gog");
        Assert.assertTrue(attributes.hasAttribute(criteria));
    }

    @Test
    public void hasAttributes() {
        Set<Attribute> attributeSet = new HashSet<>();
        Attribute attr1 = new Attribute(GROUP_ATTR_TYPE, "gog");
        attributeSet.add(attr1);
        Attribute attr2 = new Attribute(ROLE_ATTR_TYPE, "dev");
        attributeSet.add(attr2);

        Attributes attributes = new Attributes(attributeSet);

        Set<Attribute> criteria = new HashSet<>();
        AttributeType criteriaType1 = new AttributeType("https://localhost/acs", "group");
        Attribute criteria1 = new Attribute(criteriaType1, "gog");
        criteria.add(criteria1);
        AttributeType criteriaType2 = new AttributeType("https://localhost/acs", "role");
        Attribute criteria2 = new Attribute(criteriaType2, "dev");
        criteria.add(criteria2);
        Assert.assertTrue(attributes.hasAttributes(criteria));
    }

    @Test
    public void hasAttributeFailure() {

        Set<Attribute> attributeSet = new HashSet<>();
        Attribute attr1 = new Attribute(ROLE_ATTR_TYPE, "dev");
        attributeSet.add(attr1);

        Attributes attributes = new Attributes(attributeSet);
        Attribute criteria = new Attribute(GROUP_ATTR_TYPE, "gog");
        Assert.assertFalse(attributes.hasAttribute(criteria));
    }

    @Test
    public void testAddAll() {
        Attributes attributes = new Attributes();

        Set<Attribute> attributeSet = new HashSet<>();
        Attribute attr1 = new Attribute(GROUP_ATTR_TYPE, "gog");
        attributeSet.add(attr1);
        Assert.assertTrue(attributes.addAll(attributeSet));

        Attribute criteria = new Attribute(GROUP_ATTR_TYPE, "gog");
        Assert.assertTrue(attributes.hasAttribute(criteria));
    }

    @Test
    public void testAddAllWithNoChange() {
        Attributes attributes = new Attributes();

        Set<Attribute> attributeSet = new HashSet<>();
        Attribute attr1 = new Attribute(GROUP_ATTR_TYPE, "gog");
        attributeSet.add(attr1);
        Assert.assertTrue(attributes.addAll(attributeSet));

        Set<Attribute> attributeSet2 = new HashSet<>();
        Attribute attr2 = new Attribute(GROUP_ATTR_TYPE, "gog");
        attributeSet2.add(attr2);
        Assert.assertFalse(attributes.addAll(attributeSet2));
    }

    @Test
    public void testAddAllWithNewAttributeType() {
        Attributes attributes = new Attributes();

        Set<Attribute> attributeSet = new HashSet<>();
        Attribute attr1 = new Attribute(GROUP_ATTR_TYPE, "gog");
        attributeSet.add(attr1);
        Assert.assertTrue(attributes.addAll(attributeSet));

        Set<Attribute> attributeSet2 = new HashSet<>();
        Attribute attr2 = new Attribute(ROLE_ATTR_TYPE, "role");
        attributeSet2.add(attr2);
        Assert.assertTrue(attributes.addAll(attributeSet2));

        AttributeType criteria = new AttributeType("https://localhost/acs", "role");
        Assert.assertTrue(attributes.hasAttributeType(criteria));
    }
}
