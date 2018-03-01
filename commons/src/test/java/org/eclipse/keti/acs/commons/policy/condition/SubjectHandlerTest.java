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
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package org.eclipse.keti.acs.commons.policy.condition;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.eclipse.keti.acs.model.Attribute;

/**
 *
 * @author acs-engineers@ge.com
 */
@SuppressWarnings({ "unchecked", "javadoc", "nls" })
public class SubjectHandlerTest {

    @Test
    public void testNullAttributes() {

        SubjectHandler subjectHandler = new SubjectHandler(null);
        Assert.assertNotNull(subjectHandler);
    }

    @Test
    public void testEmptyAttributes() {
        SubjectHandler subjectHandler = new SubjectHandler(Collections.EMPTY_SET);
        Assert.assertNotNull(subjectHandler);
        Assert.assertTrue(subjectHandler.attributes("acs", "site").isEmpty());
    }

    @Test
    public void testValidAttributes() {

        HashSet<Attribute> attributes = new HashSet<Attribute>(
                Arrays.asList(new Attribute("acs", "site", "boston"), new Attribute("acs", "department", "sales")));
        SubjectHandler subjectHandler = new SubjectHandler(attributes);
        Assert.assertNotNull(subjectHandler);
        Assert.assertTrue(subjectHandler.attributes("acs", "site").contains("boston"));
        Assert.assertTrue(subjectHandler.attributes("acs", "department").contains("sales"));
        Assert.assertFalse(subjectHandler.attributes("acs", "site").contains("newyork"));
        Assert.assertTrue(subjectHandler.attributes("acs", "region").isEmpty());
    }

}
