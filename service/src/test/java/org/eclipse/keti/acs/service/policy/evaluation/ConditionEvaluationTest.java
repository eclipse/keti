/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.service.policy.evaluation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionCache;
import org.eclipse.keti.acs.commons.policy.condition.groovy.NonCachingGroovyConditionCache;
import org.eclipse.keti.acs.model.Attribute;
import org.eclipse.keti.acs.model.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 *
 * @author acs-engineers@ge.com
 */
@ContextConfiguration(classes = { NonCachingGroovyConditionCache.class })
public class ConditionEvaluationTest extends AbstractTestNGSpringContextTests {

    private static final String ATTRIBUTE_ISSUER = "acs";
    private static final Attribute SITE_SAN_RAMON = getAttribute("site", "San Ramon");
    private static final Attribute SITE_NEW_YORK = getAttribute("site", "New York");
    private static final Attribute SITE_BOSTON = getAttribute("site", "Boston");
    private static final Attribute SITE_LA = getAttribute("site", "LA");
    private static final Attribute LOCATION_SAN_RAMON = getAttribute("location", "San Ramon");
    private static final Attribute LOCATION_NEW_YORK = getAttribute("location", "New York");
    private static final Attribute DEPARTMENT_SALES = getAttribute("department", "sales");

    private PolicyEvaluationServiceImpl evaluationService;

    @Autowired
    private GroovyConditionCache conditionCache;

    @BeforeMethod
    private void setupMethod() throws Exception {
        this.evaluationService = new PolicyEvaluationServiceImpl();
        ReflectionTestUtils.setField(this.evaluationService, "conditionCache", this.conditionCache);
    }

    private static Set<Attribute> getAttributes(final Attribute attributeOne, final Attribute attributeTwo) {
        return new HashSet<>(Arrays.asList(attributeOne, attributeTwo));
    }

    private static Attribute getAttribute(final String name, final String value) {
        return new Attribute(ATTRIBUTE_ISSUER, name, value);
    }

    @Test(dataProvider = "validConditionsWithConstants")
    public void testConditionEvaluationWithConstants(final List<Condition> conditions, final boolean expectedResult) {
        Assert.assertEquals(this.evaluationService.evaluateConditions(Collections.emptySet(), Collections.emptySet(),
                StringUtils.EMPTY, conditions, StringUtils.EMPTY), expectedResult);
    }

    @DataProvider
    private Object[][] validConditionsWithConstants() {
        return new Object[][] { { Arrays.asList(new Condition("\"a\" == \"a\"")), true },
                { Arrays.asList(new Condition("\"a\" == \"b\"")), false },
                { Arrays.asList(new Condition("\"a\" == \"a\""), new Condition("\"b\" == \"b\"")), true },
                { Arrays.asList(new Condition("\"a\" == \"a\""), new Condition("\"a\" == \"b\"")), false },
                { Arrays.asList(new Condition("\"a\" == \"b\""), new Condition("\"c\" == \"b\"")), false },
                { null, true }, { Collections.emptyList(), true } };
    }

    @Test(dataProvider = "validConditionsWithVariables")
    public void testConditionEvaluationWithVariables(final Set<Attribute> resourceAttributes,
            final Set<Attribute> subjectAttributes, final String resourceURI, final List<Condition> conditions,
            final String resourceURITemplate, final boolean expectedResult) {
        Assert.assertEquals(this.evaluationService.evaluateConditions(subjectAttributes, resourceAttributes,
                resourceURI, conditions, resourceURITemplate), expectedResult);
    }

    @DataProvider
    private Object[][] validConditionsWithVariables() {
        return new Object[][] {

                { getAttributes(SITE_SAN_RAMON, SITE_NEW_YORK), getAttributes(SITE_SAN_RAMON, SITE_BOSTON),
                        StringUtils.EMPTY,
                        Arrays.asList(new Condition("match.any(subject.attributes(\"acs\", \"site\"),"
                                + " resource.attributes(\"acs\", \"site\"))")),
                        StringUtils.EMPTY, true },

                { getAttributes(SITE_SAN_RAMON, SITE_NEW_YORK), getAttributes(SITE_LA, SITE_BOSTON), StringUtils.EMPTY,
                        Arrays.asList(new Condition("match.any(subject.attributes(\"acs\", \"site\"),"
                                + " resource.attributes(\"acs\", \"site\"))")),
                        StringUtils.EMPTY, false },

                { getAttributes(SITE_SAN_RAMON, SITE_NEW_YORK), getAttributes(SITE_SAN_RAMON, SITE_NEW_YORK),
                        StringUtils.EMPTY,
                        Arrays.asList(new Condition("match.any(subject.attributes(\"acs\", \"site\"),"
                                + " resource.attributes(\"acs\", \"site\"))")),
                        StringUtils.EMPTY, true },

                { getAttributes(LOCATION_SAN_RAMON, LOCATION_NEW_YORK), getAttributes(SITE_SAN_RAMON, SITE_NEW_YORK),
                        StringUtils.EMPTY,
                        Arrays.asList(new Condition("match.any(resource.attributes(\"acs\", \"location\"),"
                                + " subject.attributes(\"acs\", \"site\"))")),
                        StringUtils.EMPTY, true },

                { getAttributes(SITE_SAN_RAMON, SITE_NEW_YORK), Collections.emptySet(), StringUtils.EMPTY,
                        Arrays.asList(
                                new Condition("match.single(resource.attributes(\"acs\", \"site\"), \"San Ramon\")")),
                        StringUtils.EMPTY, true },

                { Collections.emptySet(), Collections.emptySet(), "http://assets.predix.io/site/Boston",
                        Arrays.asList(new Condition("resource.uriVariable(\"site_id\").equals(\"Boston\")")),
                        "site/{site_id}", true },

                { Collections.emptySet(), Collections.emptySet(), "http://assets.predix.io/site/Boston",
                        Arrays.asList(new Condition("resource.uriVariable(\"site_id\").equals(\"New York\")")),
                        "site/{site_id}", false },

                { Collections.emptySet(), getAttributes(SITE_LA, SITE_BOSTON), "http://assets.predix.io/site/Boston",
                        Arrays.asList(new Condition("match.single(subject.attributes(\"acs\", \"site\"),"
                                + " resource.uriVariable(\"site_id\"))")),
                        "site/{site_id}", true },

                { Collections.emptySet(), getAttributes(SITE_SAN_RAMON, SITE_NEW_YORK),
                        "http://assets.predix.io/site/Boston",
                        Arrays.asList(new Condition("match.single(subject.attributes(\"acs\", \"site\"),"
                                + " resource.uriVariable(\"site_id\"))")),
                        "site/{site_id}", false },

                { Collections.emptySet(), getAttributes(SITE_SAN_RAMON, SITE_NEW_YORK), "http://assets.predix.io/site",
                        Arrays.asList(new Condition("match.single(subject.attributes(\"acs\", \"site\"),"
                                + " resource.uriVariable(\"site_id\"))")),
                        "site/{site_id}", false },

                { Collections.emptySet(), getAttributes(SITE_BOSTON, DEPARTMENT_SALES),
                        "http://assets.predix.io/site/Boston/department/sales",
                        Arrays.asList(
                                new Condition("match.single(subject.attributes(\"acs\", \"site\"),"
                                        + " resource.uriVariable(\"site_id\"))"),
                                new Condition("match.single(subject.attributes(\"acs\", \"department\"),"
                                        + " resource.uriVariable(\"department_id\"))")),
                        "site/{site_id}/department/{department_id}", true },

                { Collections.emptySet(), getAttributes(SITE_BOSTON, DEPARTMENT_SALES),
                        "http://assets.predix.io/site/Boston/department/sales",
                        Arrays.asList(new Condition("match.single(subject.attributes(\"acs\", \"site\"),"
                                + " resource.uriVariable(\"site_id\")) and"
                                + " match.single(subject.attributes(\"acs\", \"department\"),"
                                + " resource.uriVariable(\"department_id\"))")),
                        "site/{site_id}/department/{department_id}", true } };
    }

    @Test(dataProvider = "invalidConditions", expectedExceptions = PolicyEvaluationException.class)
    public void testInvalidConditionEvaluation(final List<Condition> conditions) {
        this.evaluationService.evaluateConditions(Collections.emptySet(), Collections.emptySet(), StringUtils.EMPTY,
                conditions, StringUtils.EMPTY);
    }

    @DataProvider
    private Object[][] invalidConditions() {
        return new Object[][] { { Arrays.asList(new Condition(StringUtils.EMPTY)) },
                { Arrays.asList(new Condition("'a'.equals('a')"), new Condition("System.exit(0)")) } };
    }
}
