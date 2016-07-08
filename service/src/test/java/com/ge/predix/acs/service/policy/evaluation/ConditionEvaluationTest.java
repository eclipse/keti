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

package com.ge.predix.acs.service.policy.evaluation;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.commons.policy.condition.ConditionShell;
import com.ge.predix.acs.commons.policy.condition.groovy.GroovyConditionCache;
import com.ge.predix.acs.commons.policy.condition.groovy.GroovyConditionShell;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Condition;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.service.policy.validation.PolicySetValidator;
import com.ge.predix.acs.service.policy.validation.PolicySetValidatorImpl;

/**
 *
 * @author 212406427
 */
@ContextConfiguration(classes = { GroovyConditionCache.class, PolicySetValidatorImpl.class })
public class ConditionEvaluationTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private PolicySetValidator policySetValidator;

    @BeforeClass
    public void setup() {
        ((PolicySetValidatorImpl) this.policySetValidator)
                .setValidAcsPolicyHttpActions("GET, POST, PUT, DELETE, PATCH, SUBSCRIBE, MESSAGE");
        ((PolicySetValidatorImpl) this.policySetValidator).init();
    }

    @Test(dataProvider = "conditionsProvider")
    public void testConditionEvaluationWithConstants(final List<Condition> conditions, final boolean expectedResult,
            final boolean throwsException) {
        PolicyEvaluationServiceImpl evaluationService = new PolicyEvaluationServiceImpl();
        Whitebox.setInternalState(evaluationService, "policySetValidator", this.policySetValidator);
        Whitebox.setInternalState(evaluationService, "privilegeService", mock(PrivilegeManagementService.class));
        Set<Attribute> subjectAttributes = Collections.emptySet();
        ConditionShell conditionShell = new GroovyConditionShell();
        try {
            Assert.assertEquals(evaluationService.evaluateConditions(subjectAttributes, new HashSet<Attribute>(), "",
                    conditions, "", conditionShell), expectedResult);
        } catch (Exception e) {
            if (throwsException) {
                Assert.assertTrue(e instanceof PolicyEvaluationException);
            }
        }
    }

    @DataProvider(name = "conditionsProvider")
    public Object[][] getConditions() {
        Object[][] data = new Object[][] { { Arrays.asList(new Condition("\"a\" == \"a\"")), true, false },
                { Arrays.asList(new Condition("\"a\" == \"b\"")), false, false },
                { Arrays.asList(new Condition("")), false, true }, { Collections.emptyList(), true, true }, // TODO:
                                                                                                            // Should
                // no
                // exception
                // just
                // return the
                // effect
                // if the
                // policy
                // matched
                { Arrays.asList(new Condition("\"a\" == \"a\""), new Condition("\"b\" == \"b\"")), true, false },
                { Arrays.asList(new Condition("\"a\" == \"a\""), new Condition("\"a\" == \"b\"")), false, false },
                { Arrays.asList(new Condition("\"a\" == \"b\""), new Condition("\"c\" == \"b\"")), false, false },
                { null, true, false }

        };
        return data;
    }

    @Test(dataProvider = "conditionsWithVariablesProvider")
    public void testConditionEvaluationWithVariables(final Set<Attribute> resourceAttributes,
            final Set<Attribute> subjectAttributes, final List<Condition> conditions, final boolean expectedResult,
            final boolean throwsException, final String resourceURI, final String resourceURITemplate) {

        PrivilegeManagementService mockPrivilegeManagementService = mock(PrivilegeManagementService.class);

        BaseResource resource = new BaseResource();
        resource.setAttributes(resourceAttributes);

        BaseSubject subject = new BaseSubject();
        subject.setAttributes(subjectAttributes);

        Mockito.when(mockPrivilegeManagementService.getByResourceIdentifier(anyString())).thenReturn(resource);
        Mockito.when(mockPrivilegeManagementService.getBySubjectIdentifier(anyString())).thenReturn(subject);

        PolicyEvaluationServiceImpl evaluationService = new PolicyEvaluationServiceImpl();
        Whitebox.setInternalState(evaluationService, "policySetValidator", this.policySetValidator);
        Whitebox.setInternalState(evaluationService, "privilegeService", mockPrivilegeManagementService);

        ConditionShell conditionShell = new GroovyConditionShell();

        try {
            Assert.assertEquals(evaluationService.evaluateConditions(subjectAttributes, resourceAttributes, resourceURI,
                    conditions, resourceURITemplate, conditionShell), expectedResult);

        } catch (Exception e) {

            if (throwsException) {
                Assert.assertTrue(e instanceof PolicyEvaluationException);
            } else {
                Assert.fail("Unexpected exception.", e);
            }

        }

    }

    @DataProvider(name = "conditionsWithVariablesProvider")
    public Object[][] getConditionsWithVariables() {
        Object[][] data = new Object[][] {

                { new HashSet<>(Arrays.asList(new Attribute("acs", "site", "San Ramon"),
                        new Attribute("acs", "site", "New York"))),
                        new HashSet<>(Arrays.asList(new Attribute("acs", "site", "San Ramon"),
                                new Attribute("acs", "site", "Boston"))),
                        Arrays.asList(new Condition("match.any(subject.attributes(\"acs\", \"site\"),"
                                + " resource.attributes(\"acs\", \"site\"))")),
                        true, false, "", "" },
                { new HashSet<>(Arrays.asList(new Attribute("acs", "site", "San Ramon"),
                        new Attribute("acs", "site", "New York"))),
                        new HashSet<>(Arrays.asList(new Attribute("acs", "site", "LA"),
                                new Attribute("acs", "site", "Boston"))),
                        Arrays.asList(new Condition("match.any(subject.attributes(\"acs\", \"site\"),"
                                + " resource.attributes(\"acs\", \"site\"))")),
                        false, false, "", "" },
                { new HashSet<>(Arrays.asList(new Attribute("acs", "site", "San Ramon"),
                        new Attribute("acs", "site", "New York"))),
                        new HashSet<>(Arrays.asList(new Attribute("acs", "site", "San Ramon"),
                                new Attribute("acs", "site", "New York"))),
                        Arrays.asList(new Condition("match.any(subject.attributes(\"acs\", \"site\"),"
                                + " resource.attributes(\"acs\", \"site\"))")),
                        true, false, "", ""

                }, { new HashSet<>(Arrays.asList(new Attribute("acs", "location", "San Ramon"), new Attribute("acs", "location", "New York"))), new HashSet<>(Arrays.asList(new Attribute("acs", "site", "San Ramon"), new Attribute("acs", "site", "New York"))), Arrays.asList(new Condition("match.any(resource.attributes(\"acs\", \"location\")," + " subject.attributes(\"acs\", \"site\"))")), true, false, "", ""

                }, { new HashSet<>(Arrays.asList(new Attribute("acs", "site", "San Ramon"), new Attribute("acs", "site", "New York"))), Collections.emptySet(), Arrays.asList(new Condition("match.single(resource.attributes(\"acs\", \"site\"), \"San Ramon\")")), true, false, "", ""

                }, { Collections.emptySet(), Collections.emptySet(), Arrays.asList(new Condition("resource.uriVariable(\"site_id\").equals(\"boston\")")), true, false, "http://assets.predix.io/site/boston", "site/{site_id}"

                }, { Collections.emptySet(), Collections.emptySet(), Arrays.asList(new Condition("resource.uriVariable(\"site_id\").equals(\"newyork\")")), false, false, "http://assets.predix.io/site/boston", "site/{site_id}" }, { Collections.emptySet(), new HashSet<>(Arrays.asList(new Attribute("acs", "site", "boston"), new Attribute("acs", "site", "New York"))), Arrays.asList(new Condition("match.single(subject.attributes(\"acs\", \"site\")," + " resource.uriVariable(\"site_id\"))")), true, false, "http://assets.predix.io/site/boston", "site/{site_id}"

                }, { Collections.emptySet(), new HashSet<>(Arrays.asList(new Attribute("acs", "site", "LA"), new Attribute("acs", "site", "New York"))), Arrays.asList(new Condition("match.single(subject.attributes(\"acs\", \"site\")," + " resource.uriVariable(\"site_id\"))")), false, false, "http://assets.predix.io/site/boston", "site/{site_id}" }, { Collections.emptySet(), new HashSet<>(Arrays.asList(new Attribute("acs", "site", "LA"), new Attribute("acs", "site", "New York"))), Arrays.asList(new Condition("match.single(subject.attributes(\"acs\", \"site\")," + " resource.uriVariable(\"site_id\"))")), false, false, "http://assets.predix.io/site", "site/{site_id}" }, { Collections.emptySet(), new HashSet<>(Arrays.asList(new Attribute("acs", "site", "boston"), new Attribute("acs", "department", "sales"))), Arrays.asList(new Condition("match.single(subject.attributes(\"acs\", \"site\")," + " resource.uriVariable(\"site_id\"))"), new Condition("match.single(subject.attributes(\"acs\", \"department\")," + " resource.uriVariable(\"department_id\"))")), true, false, "http://assets.predix.io/site/boston/department/sales", "site/{site_id}/department/{department_id}" }, { Collections.emptySet(), new HashSet<>(Arrays.asList(new Attribute("acs", "site", "boston"), new Attribute("acs", "department", "sales"))), Arrays.asList(new Condition("match.single(subject.attributes(\"acs\", \"site\")," + " resource.uriVariable(\"site_id\")) " + "and match.single(subject.attributes(\"acs\", \"department\")," + " resource.uriVariable(\"department_id\"))")), true, false, "http://assets.predix.io/site/boston/department/sales", "site/{site_id}/department/{department_id}" }, };
        return data;
    }

}
