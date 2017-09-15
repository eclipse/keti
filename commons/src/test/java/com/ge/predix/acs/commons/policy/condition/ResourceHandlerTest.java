/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.ge.predix.acs.commons.policy.condition;

import java.util.Arrays;
import java.util.HashSet;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.ge.predix.acs.model.Attribute;

/**
 *
 * @author 212406427
 */
@SuppressWarnings({ "nls", "javadoc" })
public class ResourceHandlerTest {

    @Test
    public void testNullAttributes() {
        ResourceHandler handler = new ResourceHandler(null, null, null);
        Assert.assertNotNull(handler);
        Assert.assertEquals("", handler.uriVariable("site_id"));
        Assert.assertEquals("", handler.uriVariable(null));
    }

    @Test
    public void testPartialNullAttributes() {
        ResourceHandler handler = new ResourceHandler(null, null, "a");
        Assert.assertNotNull(handler);
        Assert.assertEquals("", handler.uriVariable("site_id"));
        Assert.assertEquals("", handler.uriVariable(null));
    }

    @Test
    public void testUriVariableNegative() {
        HashSet<Attribute> attributes = new HashSet<Attribute>(
                Arrays.asList(new Attribute("acs", "site", "boston"), new Attribute("acs", "department", "sales")));
        ResourceHandler handler = new ResourceHandler(attributes, "", "");
        Assert.assertEquals("", handler.uriVariable(""));
        Assert.assertEquals("", handler.uriVariable(null));
    }

    @Test
    public void testUriVariablePositive() {
        HashSet<Attribute> attributes = new HashSet<Attribute>(
                Arrays.asList(new Attribute("acs", "site", "boston"), new Attribute("acs", "department", "sales")));
        ResourceHandler handler = new ResourceHandler(attributes,
                "http://assets.predix.io/site/boston/department/sales", "site/{site_id}/department/{department_id}");
        Assert.assertEquals("boston", handler.uriVariable("site_id"));
        Assert.assertEquals("sales", handler.uriVariable("department_id"));
        Assert.assertNotEquals("hr", handler.uriVariable("department_id"));
    }
}
