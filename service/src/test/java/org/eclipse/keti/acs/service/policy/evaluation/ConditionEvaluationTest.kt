/*******************************************************************************
 * Copyright 2018 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.service.policy.evaluation

import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionCache
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionShell
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Condition
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.service.policy.validation.PolicySetValidator
import org.eclipse.keti.acs.service.policy.validation.PolicySetValidatorImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.util.ReflectionTestUtils
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.util.Arrays
import java.util.HashSet

/**
 *
 * @author acs-engineers@ge.com
 */
@ContextConfiguration(
    classes = [(GroovyConditionCache::class), (GroovyConditionShell::class), (PolicySetValidatorImpl::class)]
)
class ConditionEvaluationTest : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var policySetValidator: PolicySetValidator

    // TODO: Should no exception just return the effect if the policy matched
    val conditions: Array<Array<Any?>>
        @DataProvider(name = "conditionsProvider")
        get() = arrayOf<Array<Any?>>(
            arrayOf(Arrays.asList(Condition("\"a\" == \"a\"")), true, false),
            arrayOf(Arrays.asList(Condition("\"a\" == \"b\"")), false, false),
            arrayOf(Arrays.asList(Condition("")), false, true),
            arrayOf(emptyList<Any>(), true, true),
            arrayOf(Arrays.asList(Condition("\"a\" == \"a\""), Condition("\"b\" == \"b\"")), true, false),
            arrayOf(Arrays.asList(Condition("\"a\" == \"a\""), Condition("\"a\" == \"b\"")), false, false),
            arrayOf(Arrays.asList(Condition("\"a\" == \"b\""), Condition("\"c\" == \"b\"")), false, false),
            arrayOf(null, true, false)
        )

    val conditionsWithVariables: Array<Array<Any?>>
        @DataProvider(name = "conditionsWithVariablesProvider")
        get() = arrayOf(

            arrayOf(
                HashSet(
                    Arrays.asList(
                        Attribute("acs", "site", "San Ramon"),
                        Attribute("acs", "site", "New York")
                    )
                ),
                HashSet(
                    Arrays.asList(
                        Attribute("acs", "site", "San Ramon"),
                        Attribute("acs", "site", "Boston")
                    )
                ),
                Arrays.asList(Condition("match.any(subject.attributes(\"acs\", \"site\")," + " resource.attributes(\"acs\", \"site\"))")),
                true,
                false,
                "",
                ""
            ),
            arrayOf(
                HashSet(
                    Arrays.asList(
                        Attribute("acs", "site", "San Ramon"),
                        Attribute("acs", "site", "New York")
                    )
                ),
                HashSet(
                    Arrays.asList(
                        Attribute("acs", "site", "LA"),
                        Attribute("acs", "site", "Boston")
                    )
                ),
                Arrays.asList(Condition("match.any(subject.attributes(\"acs\", \"site\")," + " resource.attributes(\"acs\", \"site\"))")),
                false,
                false,
                "",
                ""
            ),
            arrayOf(
                HashSet(
                    Arrays.asList(
                        Attribute("acs", "site", "San Ramon"),
                        Attribute("acs", "site", "New York")
                    )
                ),
                HashSet(
                    Arrays.asList(
                        Attribute("acs", "site", "San Ramon"),
                        Attribute("acs", "site", "New York")
                    )
                ),
                Arrays.asList(Condition("match.any(subject.attributes(\"acs\", \"site\")," + " resource.attributes(\"acs\", \"site\"))")),
                true,
                false,
                "",
                ""
            ),
            arrayOf(
                HashSet(
                    Arrays.asList(
                        Attribute("acs", "location", "San Ramon"),
                        Attribute("acs", "location", "New York")
                    )
                ),
                HashSet(
                    Arrays.asList(
                        Attribute("acs", "site", "San Ramon"),
                        Attribute("acs", "site", "New York")
                    )
                ),
                Arrays.asList(Condition("match.any(resource.attributes(\"acs\", \"location\")," + " subject.attributes(\"acs\", \"site\"))")),
                true,
                false,
                "",
                ""
            ),
            arrayOf(
                HashSet(
                    Arrays.asList(
                        Attribute("acs", "site", "San Ramon"),
                        Attribute("acs", "site", "New York")
                    )
                ), emptySet<Any>(), Arrays.asList(
                    Condition(
                        "match.single(resource.attributes(\"acs\", \"site\"), \"San Ramon\")"
                    )
                ), true, false, "", ""
            ),
            arrayOf(
                emptySet<Any>(),
                emptySet<Any>(),
                Arrays.asList(Condition("resource.uriVariable(\"site_id\").equals(\"boston\")")),
                true,
                false,
                "http://assets.predix.io/site/boston",
                "site/{site_id}"
            ),
            arrayOf(
                emptySet<Any>(),
                emptySet<Any>(),
                Arrays.asList(Condition("resource.uriVariable(\"site_id\").equals(\"newyork\")")),
                false,
                false,
                "http://assets.predix.io/site/boston",
                "site/{site_id}"
            ),
            arrayOf(
                emptySet<Any>(),
                HashSet(
                    Arrays.asList(
                        Attribute("acs", "site", "boston"),
                        Attribute("acs", "site", "New York")
                    )
                ),
                Arrays.asList(Condition("match.single(subject.attributes(\"acs\", \"site\")," + " resource.uriVariable(\"site_id\"))")),
                true,
                false,
                "http://assets.predix.io/site/boston",
                "site/{site_id}"
            ),
            arrayOf(
                emptySet<Any>(),
                HashSet(
                    Arrays.asList(
                        Attribute("acs", "site", "LA"),
                        Attribute("acs", "site", "New York")
                    )
                ),
                Arrays.asList(Condition("match.single(subject.attributes(\"acs\", \"site\")," + " resource.uriVariable(\"site_id\"))")),
                false,
                false,
                "http://assets.predix.io/site/boston",
                "site/{site_id}"
            ),
            arrayOf(
                emptySet<Any>(),
                HashSet(
                    Arrays.asList(
                        Attribute("acs", "site", "LA"),
                        Attribute("acs", "site", "New York")
                    )
                ),
                Arrays.asList(Condition("match.single(subject.attributes(\"acs\", \"site\")," + " resource.uriVariable(\"site_id\"))")),
                false,
                false,
                "http://assets.predix.io/site",
                "site/{site_id}"
            ),
            arrayOf(
                emptySet<Any>(),
                HashSet(
                    Arrays.asList(
                        Attribute("acs", "site", "boston"),
                        Attribute("acs", "department", "sales")
                    )
                ),
                Arrays.asList(
                    Condition("match.single(subject.attributes(\"acs\", \"site\")," + " resource.uriVariable(\"site_id\"))"),
                    Condition("match.single(subject.attributes(\"acs\", \"department\")," + " resource.uriVariable(\"department_id\"))")
                ),
                true,
                false,
                "http://assets.predix.io/site/boston/department/sales",
                "site/{site_id}/department/{department_id}"
            ),
            arrayOf(
                emptySet<Any>(),
                HashSet(
                    Arrays.asList(
                        Attribute("acs", "site", "boston"),
                        Attribute("acs", "department", "sales")
                    )
                ),
                Arrays.asList(
                    Condition(
                        "match.single(subject.attributes(\"acs\", \"site\"),"
                        + " resource.uriVariable(\"site_id\")) "
                        + "and match.single(subject.attributes(\"acs\", \"department\"),"
                        + " resource.uriVariable(\"department_id\"))"
                    )
                ),
                true,
                false,
                "http://assets.predix.io/site/boston/department/sales",
                "site/{site_id}/department/{department_id}"
            )
        )

    @BeforeClass
    fun setup() {
        val policySetValidatorImpl = (this.policySetValidator as PolicySetValidatorImpl)
        policySetValidatorImpl.setValidAcsPolicyHttpActions("GET, POST, PUT, DELETE, PATCH, SUBSCRIBE, MESSAGE")
        policySetValidatorImpl.init()
    }

    @Test(dataProvider = "conditionsProvider")
    fun testConditionEvaluationWithConstants(
        conditions: List<Condition>?,
        expectedResult: Boolean,
        throwsException: Boolean
    ) {
        val evaluationService = PolicyEvaluationServiceImpl()
        ReflectionTestUtils.setField(evaluationService, "policySetValidator", this.policySetValidator)
        val subjectAttributes = emptySet<Attribute>()
        try {
            Assert.assertEquals(
                evaluationService.evaluateConditions(
                    subjectAttributes, HashSet(), "",
                    conditions, ""
                ), expectedResult
            )
        } catch (e: Exception) {
            if (throwsException) {
                Assert.assertTrue(e is PolicyEvaluationException)
            }
        }

    }

    @Test(dataProvider = "conditionsWithVariablesProvider")
    fun testConditionEvaluationWithVariables(
        resourceAttributes: Set<Attribute>,
        subjectAttributes: Set<Attribute>,
        conditions: List<Condition>,
        expectedResult: Boolean,
        throwsException: Boolean,
        resourceURI: String,
        resourceURITemplate: String
    ) {

        val resource = BaseResource()
        resource.attributes = resourceAttributes

        val subject = BaseSubject()
        subject.attributes = subjectAttributes

        val evaluationService = PolicyEvaluationServiceImpl()
        ReflectionTestUtils.setField(evaluationService, "policySetValidator", this.policySetValidator)

        try {
            Assert.assertEquals(
                evaluationService.evaluateConditions(
                    subjectAttributes, resourceAttributes, resourceURI,
                    conditions, resourceURITemplate
                ), expectedResult
            )

        } catch (e: Exception) {

            if (throwsException) {
                Assert.assertTrue(e is PolicyEvaluationException)
            } else {
                Assert.fail("Unexpected exception.", e)
            }

        }

    }

}
