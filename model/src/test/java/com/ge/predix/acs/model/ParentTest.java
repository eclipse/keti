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
