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

package org.eclipse.keti.acs.service.policy.matcher

import com.nhaarman.mockito_kotlin.any
import org.eclipse.keti.acs.attribute.readers.AttributeReaderFactory
import org.eclipse.keti.acs.attribute.readers.PrivilegeServiceResourceAttributeReader
import org.eclipse.keti.acs.attribute.readers.PrivilegeServiceSubjectAttributeReader
import org.eclipse.keti.acs.attribute.readers.ResourceAttributeReader
import org.eclipse.keti.acs.attribute.readers.SubjectAttributeReader
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionShell
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.web.util.UriTemplate
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File
import java.io.IOException
import java.util.HashSet

private const val POLICY_DIR_PATH = "src/test/resources/policies"

val methodName: String
    get() = Thread.currentThread().stackTrace[2].methodName

fun getPolicyFileForTest(methodName: String): File {
    return File(POLICY_DIR_PATH, "$methodName.json")
}

/**
 * Unit tests for PolicyMatcher class.
 *
 * @author acs-engineers@ge.com
 */
@Test
class PolicyMatcherImplTest {

    @Mock
    private lateinit var attributeReaderFactory: AttributeReaderFactory
    @Mock
    private lateinit var defaultResourceAttributeReader: PrivilegeServiceResourceAttributeReader
    @Mock
    private lateinit var defaultSubjectAttributeReader: PrivilegeServiceSubjectAttributeReader
    @InjectMocks
    private lateinit var policyMatcher: PolicyMatcher

    @BeforeClass
    fun setup() {
        this.policyMatcher = PolicyMatcherImpl()
        MockitoAnnotations.initMocks(this)
        `when`<ResourceAttributeReader>(this.attributeReaderFactory.resourceAttributeReader)
            .thenReturn(this.defaultResourceAttributeReader)
        `when`<SubjectAttributeReader>(this.attributeReaderFactory.subjectAttributeReader)
            .thenReturn(this.defaultSubjectAttributeReader)
        `when`(this.defaultResourceAttributeReader.getAttributes(anyString())).thenReturn(emptySet())
        val subject = BaseSubject("test-subject")
        subject.attributes = HashSet()
        `when`(
            this.defaultSubjectAttributeReader.getAttributesByScope(
                anyString(),
                any()
            )
        )
            .thenReturn(subject.attributes)
    }

    /**
     * Tests matching a blanket policy to a request.
     *
     * @throws IOException
     * on failure to load policy required for test.
     */
    @Throws(IOException::class)
    fun testMatchPolicyWithNoTarget() {
        val file = getPolicyFileForTest(methodName)
        val policySet = loadFromFile(file)
        val policies = policySet.policies

        val candidate = PolicyMatchCandidate()
        candidate.action = "GET"
        candidate.resourceURI = "/this/does/not/exist"
        candidate.subjectIdentifier = "Edward R. Murrow"
        candidate.supplementalResourceAttributes = emptySet()
        candidate.supplementalSubjectAttributes = emptySet()
        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        Assert.assertEquals(matchedPolicies.size, 1)
        Assert.assertEquals(matchedPolicies[0].policy, policies[policies.size - 1])
        Assert.assertNull(matchedPolicies[0].policy!!.target)
    }

    /**
     * Tests matching a blanket policy to a request.
     *
     * @throws IOException
     * on failure to load policy required for test.
     */
    @Throws(IOException::class)
    fun testMatchPolicyWithNoTargetAction() {
        val file = getPolicyFileForTest(methodName)
        val policySet = loadFromFile(file)
        val policies = policySet.policies

        val candidate = PolicyMatchCandidate()
        candidate.action = "GET"
        candidate.resourceURI = "/this/does/not/exist"
        candidate.subjectIdentifier = "Edward R. Murrow"
        candidate.supplementalResourceAttributes = emptySet()
        candidate.supplementalSubjectAttributes = emptySet()
        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        Assert.assertEquals(matchedPolicies.size, 1)
        Assert.assertEquals(matchedPolicies[0].policy, policies[policies.size - 1])
        Assert.assertNull(matchedPolicies[0].policy!!.target)
    }

    /**
     * Tests matching a policy that does not specify a subject (i.e. applies to all subjects) to a request.
     *
     * @throws IOException
     * on failure to load policy required for test.
     */
    @Throws(IOException::class)
    fun testMatchPolicyWithNoSubject() {
        val file = getPolicyFileForTest(methodName)
        val policySet = loadFromFile(file)
        val policies = policySet.policies

        val candidate = PolicyMatchCandidate()
        candidate.action = "GET"
        candidate.resourceURI = "/public"
        candidate.subjectIdentifier = "Edward R. Murrow"
        candidate.supplementalResourceAttributes = emptySet()
        candidate.supplementalSubjectAttributes = emptySet()
        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        Assert.assertEquals(matchedPolicies.size, 1)
        Assert.assertEquals(matchedPolicies[0].policy, policies[policies.size - 1])
        Assert.assertNull(matchedPolicies[0].policy!!.target!!.subject)
    }

    /**
     * Tests matching a policy that does not specify a resource (i.e. applies to all resources) to a request.
     *
     * @throws IOException
     * on failure to load policy required for test.
     */
    @Throws(IOException::class)
    fun testMatchPolicyWithNoResource() {
        val file = getPolicyFileForTest(methodName)
        val policySet = loadFromFile(file)
        val policies = policySet.policies

        val candidate = PolicyMatchCandidate()
        candidate.action = "GET"
        candidate.resourceURI = "/public"
        candidate.subjectIdentifier = "Edward R. Murrow"
        candidate.supplementalSubjectAttributes = HashSet(
            listOf(Attribute("https://acs.attributes.int", "role", "admin"))
        )
        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        Assert.assertEquals(matchedPolicies.size, 1)
        Assert.assertEquals(matchedPolicies[0].policy, policies[policies.size - 1])
        Assert.assertNull(matchedPolicies[0].policy!!.target!!.resource)
    }

    /**
     * Tests matching a policy that requires the subject to have a particular attribute and the resource to have a
     * particular attribute.
     *
     * @throws IOException
     * on failure to load policy required for test.
     */
    @Throws(IOException::class)
    fun testMatchPolicyWithSubjectAndResourceAttributeRequirements() {
        val file = getPolicyFileForTest(methodName)
        val policySet = loadFromFile(file)

        val groupAttr = Attribute("https://acs.attributes.int", "group", "gog")
        val resourceAttributes = HashSet<Attribute>()
        resourceAttributes.add(groupAttr)

        val testResource = BaseResource()
        testResource.attributes = resourceAttributes
        testResource.resourceIdentifier = "/assets/1123"

        `when`(this.defaultResourceAttributeReader.getAttributes(testResource.resourceIdentifier!!))
            .thenReturn(testResource.attributes)

        val policies = policySet.policies
        val candidate = PolicyMatchCandidate()
        candidate.action = "GET"
        candidate.resourceURI = "/assets/1123"
        candidate.subjectIdentifier = "Edward R. Murrow"
        candidate.supplementalSubjectAttributes = HashSet(listOf(groupAttr))

        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        Assert.assertEquals(matchedPolicies.size, 1)
        Assert.assertEquals(matchedPolicies[0].policy, policies[policies.size - 1])
        Assert.assertEquals(
            matchedPolicies[0].policy!!.target!!.resource!!.attributes!![0].name, "group"
        )
        Assert.assertEquals(
            matchedPolicies[0].policy!!.target!!.subject!!.attributes[0].name, "group"
        )
    }

    /**
     * Tests matching a policy that requires the subject to have a particular attribute and the resource to have a
     * particular attribute.
     *
     * @throws IOException
     * on failure to load policy required for test.
     */
    @Throws(IOException::class)
    fun testMatchPolicyFailureBecauseMissingResourceAttribute() {
        val file = getPolicyFileForTest(methodName)
        val policySet = loadFromFile(file)
        val policies = policySet.policies

        val candidate = PolicyMatchCandidate()
        candidate.action = "GET"
        candidate.resourceURI = "/assets/1123"

        candidate.subjectIdentifier = "Edward R. Murrow"
        candidate.supplementalSubjectAttributes = HashSet(
            listOf(Attribute("https://acs.attributes.int", "group", "gog"))
        )
        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        Assert.assertEquals(matchedPolicies.size, 0)
    }

    /**
     * Tests matching a policy that requires a specific URI template and a specific subject attribute.
     *
     * @throws IOException
     * on failure to load policy required for test.
     */
    @Throws(IOException::class)
    fun testMatchPolicyWithUriTemplateAndSubjectAttribute() {
        val file = getPolicyFileForTest(methodName)
        val policySet = loadFromFile(file)
        val policies = policySet.policies

        val candidate = PolicyMatchCandidate()
        candidate.action = "GET"
        candidate.resourceURI = "/sites/1123"

        candidate.subjectIdentifier = "Edward R. Murrow"
        val siteAttr = Attribute("https://acs.attributes.int", "site", "1123")
        candidate.supplementalSubjectAttributes = HashSet(listOf(siteAttr))
        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        Assert.assertEquals(matchedPolicies.size, 1)
        Assert.assertEquals(matchedPolicies[0].policy, policies[policies.size - 1])
        Assert.assertEquals(
            matchedPolicies[0].policy!!.target!!.resource!!.uriTemplate,
            "/sites/{site_id}"
        )
        Assert.assertEquals(
            matchedPolicies[0].policy!!.target!!.subject!!.attributes[0].name, "site"
        )
    }

    /**
     * Tests a failure to match any policy to the request.
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    fun testMultiplePoliciesNoMatch() {
        val file = getPolicyFileForTest(methodName)
        val policySet = loadFromFile(file)
        val policies = policySet.policies

        val candidate = PolicyMatchCandidate()
        candidate.action = "GET"
        candidate.resourceURI = "/this/does/not/exist"
        candidate.subjectIdentifier = "Edward R. Murrow"
        candidate.supplementalSubjectAttributes = emptySet()
        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        Assert.assertEquals(matchedPolicies.size, 0)
    }

    @Throws(IOException::class)
    fun testMultiplePoliciesMultipleMatches() {
        val file = getPolicyFileForTest("policySetWithOverlappingURIs")
        val policySet = loadFromFile(file)
        val policies = policySet.policies

        val candidate = PolicyMatchCandidate()
        candidate.resourceURI = "/alarm/site/site45/asset/asset46"
        candidate.subjectIdentifier = "Edward R. Murrow"
        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        Assert.assertEquals(matchedPolicies.size, 2)
    }

    @Throws(IOException::class)
    fun testMultiplePoliciesOneMatch() {
        val file = getPolicyFileForTest("policySetWithOverlappingURIs")
        val policySet = loadFromFile(file)
        val policies = policySet.policies

        val candidate = PolicyMatchCandidate()
        candidate.resourceURI = "/alarm/site/site45"
        candidate.subjectIdentifier = "Edward R. Murrow"
        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        Assert.assertEquals(matchedPolicies.size, 1)
    }

    /**
     * This is test to make sure the policy matcher has the same behavior as regular Spring URITemplate for the given
     * data provider: policySetMatcherDataProvider.
     *
     * @param uriTemplate
     * The URI template that will be used in the policy set
     * @param uri
     * the HTTP Request URI
     * @param policyMatcherExpectedMatch
     * Expected behavior from the Policy Matcher
     */
    @Test(dataProvider = "policySetMatcherDataProvider")
    @Throws(IOException::class)
    fun testPolicySetMatcher(
        uriTemplate: String,
        uri: String,
        policyMatcherExpectedMatch: Boolean
    ) {
        doTestForPolicySetMatcher(uriTemplate, uri, policyMatcherExpectedMatch)
    }

    @Throws(IOException::class)
    private fun doTestForPolicySetMatcher(
        uriTemplate: String,
        uri: String,
        policyMatcherExpectedMatch: Boolean
    ) {
        val policySet = loadFromFile(getPolicyFileForTest("singlePolicyNoCondition"))

        policySet.policies[0].target!!.resource!!.uriTemplate = uriTemplate
        val policies = policySet.policies

        val candidate = PolicyMatchCandidate()
        candidate.action = "GET"
        candidate.resourceURI = uri
        candidate.subjectIdentifier = "Edward R. Murrow"
        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        val expectedSize = if (policyMatcherExpectedMatch) 1 else 0
        Assert.assertEquals(matchedPolicies.size, expectedSize, "Policy match count is incorrect")
    }

    /**
     * Tests matching multiple actions to a request.
     *
     * @throws IOException
     * on failure to load policy required for test.
     */
    @Throws(IOException::class)
    fun testMatchPolicyWithMultipleActions() {
        val file = getPolicyFileForTest(methodName)
        val policySet = loadFromFile(file)
        val policies = policySet.policies

        val candidate = PolicyMatchCandidate()
        candidate.action = "GET"
        candidate.resourceURI = "/assets/45"
        candidate.subjectIdentifier = "Edward R. Murrow"
        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        Assert.assertEquals(matchedPolicies.size, 1)
    }

    @Throws(IOException::class)
    fun testMatchPolicyWithMultipleActionsNoMatch() {
        val file = getPolicyFileForTest(methodName)
        val policySet = loadFromFile(file)
        val policies = policySet.policies

        val candidate = PolicyMatchCandidate()
        candidate.action = "GET"
        candidate.resourceURI = "/assets/45"
        candidate.subjectIdentifier = "Edward R. Murrow"
        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        Assert.assertEquals(matchedPolicies.size, 0)
    }

    @Throws(Exception::class)
    fun testApmPolicySetLoadsSuccessfully() {
        val file = getPolicyFileForTest(methodName)
        val policySet = loadFromFile(file)
        val policies = policySet.policies

        val shell = GroovyConditionShell()
        for (policy in policies) {
            for (condition in policy.conditions) {
                shell.parse(condition.condition)
            }
        }

        val candidate = PolicyMatchCandidate()
        candidate.action = "GET"
        candidate.resourceURI = "/assets/45"
        candidate.subjectIdentifier = "Edward R. Murrow"
        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        Assert.assertEquals(matchedPolicies.size, 1)
        Assert.assertEquals(matchedPolicies[0].policy!!.effect, Effect.DENY)
    }

    /**
     * This test allows us to understand the behavior of the regular Spring URITemplate.
     *
     * @param uriTemplate
     * The URI template that will be used
     * @param uri
     * the HTTP Request URI
     * @param uriTemplateExpectedMatch
     * Expected behavior from the regular URITemplate
     * @param varNames
     * Expanded variable names
     * @param varValues
     * Expanded variable values
     */
    @Test(dataProvider = "uriTemplateMatchDataProvider")
    fun testURITemplateMatch(
        uriTemplate: String,
        uri: String,

        uriTemplateExpectedMatch: Boolean?,
        varNames: Array<String>,
        varValues: Array<String>
    ) {
        doTestForURITemplateMatch(uriTemplate, uri, uriTemplateExpectedMatch!!, varNames, varValues)
    }

    private fun doTestForURITemplateMatch(
        uriTemplate: String,
        uri: String,
        uriTemplateExpectedMatch: Boolean,
        varNames: Array<String>,
        varValues: Array<String>
    ) {
        val template = UriTemplate(uriTemplate)
        Assert.assertEquals(template.matches(uri), uriTemplateExpectedMatch)

        val matchedVariables = template.match(uri)
        for (i in varNames.indices) {
            // skip variable match if name is "n/a"
            if (varNames[i] == "n/a") {
                continue
            }

            Assert.assertEquals(matchedVariables[varNames[i]], varValues[i])
            Assert.assertEquals(matchedVariables[varNames[i]], varValues[i])
        }
    }

    @DataProvider(name = "uriTemplateMatchDataProvider")
    private fun uriTemplateMatchDataProvider(): Array<Array<out Any?>> {
        return arrayOf(
            /**
             * Each entry has the following data: uriTemplate, HTTP request URI, URITemplate expected match
             * result, Expected expanded URITemplate variable values
             */
            // exact match
            arrayOf("/one/{var1}", "/one/two", java.lang.Boolean.TRUE, arrayOf("var1"), arrayOf("two")),
            arrayOf("/one/{var1}", "/one", java.lang.Boolean.FALSE, arrayOf("n/a"), arrayOf("n/a")),

            // extra sub-path
            arrayOf(
                "/one/{var1}",
                "/one/two/three",
                java.lang.Boolean.TRUE,
                arrayOf("var1"),
                arrayOf("two/three")
            ),

            // var matches multiple sub-paths.
            arrayOf(
                "/one/{var1}/two",
                "/one/stuff/in/between/two",
                java.lang.Boolean.TRUE,
                arrayOf("var1"),
                arrayOf("stuff/in/between")
            ),

            // variable matches right-most sub-path
            arrayOf(
                "/one/{var1}/two/{var2}/four",
                "/one/two/two/two/three/three/four",
                java.lang.Boolean.TRUE,
                arrayOf("var1", "var2"),
                arrayOf("two/two", "three/three")
            ),

            // no match
            arrayOf("/one/{var1}/two", "/one/two/three", java.lang.Boolean.FALSE, arrayOf("n/a"), arrayOf("n/a")),
            arrayOf("/one", "/one/two", java.lang.Boolean.FALSE, arrayOf("n/a"), arrayOf("n/a")),

            // Eager match of sub-paths (default behavior)
            arrayOf(
                "/customer/{customer_id}",
                "/customer/c1",
                java.lang.Boolean.TRUE,
                arrayOf("customer_id"),
                arrayOf("c1")
            ),
            arrayOf(
                "/customer/{customer_id}",
                "/customer/c1site/s1",
                java.lang.Boolean.TRUE,
                arrayOf("customer_id"),
                arrayOf("c1site/s1")
            ),

            // Exact match with sub-paths
            arrayOf(
                "/customer/{customer_id:\\w*}",
                "/customer/c1",
                java.lang.Boolean.TRUE,
                arrayOf("customer_id"),
                arrayOf("c1")
            ),
            arrayOf(
                "/customer/{customer_id:\\w*}",
                "/customer/c1/site/s1",
                java.lang.Boolean.FALSE,
                arrayOf("n/a"),
                arrayOf("n/a")
            ),

            arrayOf(
                "/customer/{customer_id:\\w*}/",
                "/customer/c1/",
                java.lang.Boolean.TRUE,
                arrayOf("customer_id"),
                arrayOf("c1")
            ),
            arrayOf(
                "/customer/{customer_id:\\w*}/",
                "/customer/c1/site/s1/",
                java.lang.Boolean.FALSE,
                arrayOf("n/a"),
                arrayOf("n/a")
            ),

            arrayOf(
                "/customer/{customer_id:\\w*}/site/{site_id:\\w*}",
                "/customer/c1/site/s1",
                java.lang.Boolean.TRUE,
                arrayOf("customer_id", "site_id"),
                arrayOf("c1", "s1")
            ),

            arrayOf(
                "/customer/{customer_id:\\w*}",
                "/customer/c1/site/s1",
                java.lang.Boolean.FALSE,
                arrayOf("n/a"),
                arrayOf("n/a")
            ),

            arrayOf("/asset/", "/asset/", java.lang.Boolean.TRUE, arrayOf("n/a"), arrayOf("n/a")),
            arrayOf("/asset", "/asset", java.lang.Boolean.TRUE, arrayOf("n/a"), arrayOf("n/a")),
            arrayOf("/asset", "/asset//", java.lang.Boolean.FALSE, arrayOf("n/a"), arrayOf("n/a")),
            arrayOf("/asset/", "/asset//", java.lang.Boolean.FALSE, arrayOf("n/a"), arrayOf("n/a")),
            arrayOf(
                "/sites/{site_id}",
                "/sites/sanramon/",
                java.lang.Boolean.TRUE,
                arrayOf("site_id"),
                arrayOf("sanramon/")
            ),
            arrayOf(
                "/sites/{site_id}",
                "/sites/sanramon",
                java.lang.Boolean.TRUE,
                arrayOf("site_id"),
                arrayOf("sanramon")
            ),
            arrayOf(
                "/sites/{site_id}/",
                "/sites/sanramon/",
                java.lang.Boolean.TRUE,
                arrayOf("site_id"),
                arrayOf("sanramon")
            ),
            arrayOf(
                "/myservice/{version}/stuff",
                "/myservice/v1/stuff",
                java.lang.Boolean.TRUE,
                arrayOf("version"),
                arrayOf("v1")
            )
        )
    }

    @DataProvider(name = "policySetMatcherDataProvider")
    private fun policySetMatcherDataProvider(): Array<Array<out Any?>> {
        return arrayOf(
            /**
             * Each entry has the following data: uriTemplate, HTTP request URI, URITemplate expected match
             * result, Expected expanded URITemplate variable values
             */
            // exact match
            arrayOf("/one/{var1}", "/one/two", java.lang.Boolean.TRUE),
            arrayOf("/one/{var1}", "/one", java.lang.Boolean.FALSE),

            // extra sub-path
            arrayOf("/one/{var1}", "/one/two/three", java.lang.Boolean.TRUE),

            // var matches multiple sub-paths.
            arrayOf("/one/{var1}/two", "/one/stuff/in/between/two", java.lang.Boolean.TRUE),

            // variable matches right-most sub-path
            arrayOf("/one/{var1}/two/{var2}/four", "/one/two/two/two/three/three/four", java.lang.Boolean.TRUE),

            // no match
            arrayOf("/one/{var1}/two", "/one/two/three", java.lang.Boolean.FALSE),
            arrayOf("/one", "/one/two", java.lang.Boolean.FALSE),

            // Eager match of sub-paths (default behavior)
            arrayOf("/customer/{customer_id}", "/customer/c1", java.lang.Boolean.TRUE),
            arrayOf("/customer/{customer_id}", "/customer/c1site/s1", java.lang.Boolean.TRUE),

            // Exact match with sub-paths
            arrayOf("/customer/{customer_id:\\w*}", "/customer/c1", java.lang.Boolean.TRUE),
            arrayOf("/customer/{customer_id:\\w*}", "/customer/c1/site/s1", java.lang.Boolean.FALSE),

            arrayOf("/customer/{customer_id:\\w*}/", "/customer/c1/", java.lang.Boolean.TRUE),
            arrayOf("/customer/{customer_id:\\w*}/", "/customer/c1/site/s1/", java.lang.Boolean.FALSE),

            arrayOf("/customer/{customer_id:\\w*}/site/{site_id:\\w*}", "/customer/c1/site/s1", java.lang.Boolean.TRUE),

            arrayOf("/customer/{customer_id:\\w*}", "/customer/c1/site/s1", java.lang.Boolean.FALSE),

            arrayOf("/asset/", "/asset/", java.lang.Boolean.TRUE),
            arrayOf("/asset/", "/asset", java.lang.Boolean.TRUE),
            arrayOf("/asset", "/asset", java.lang.Boolean.TRUE),
            arrayOf("/asset", "/asset//", java.lang.Boolean.TRUE),
            arrayOf("/asset/", "/asset//", java.lang.Boolean.TRUE),
            arrayOf("/asset/", "/asset/../asset", java.lang.Boolean.TRUE),
            arrayOf("/asset/", "/asset/.", java.lang.Boolean.TRUE),
            arrayOf("/sites/{site_id}", "/sites/sanramon/", java.lang.Boolean.TRUE),
            arrayOf("/sites/{site_id}", "/sites/sanramon", java.lang.Boolean.TRUE),
            arrayOf("/sites/{site_id}/", "/sites/sanramon/", java.lang.Boolean.TRUE),
            arrayOf("/myservice/{version}/stuff", "/myservice/v1/stuff", java.lang.Boolean.TRUE)
        )
    }

    /**
     * This is the test to highlight the different behavior for the Spring URITemplate and the Policy Set Matcher with
     * which introduces a trailing slash to create canonical URI.
     */
    @Test(dataProvider = "uriDataProviderDifferentMatchBehavior")
    @Throws(IOException::class)
    fun testURITemplateMatchVsPolicySetMatch(
        uriTemplate: String,
        uri: String,
        uriTemplateExpectedMatch: Boolean,
        policyMatcherExpectedMatch: Boolean,
        varNames: Array<String>,
        varValues: Array<String>
    ) {
        // test the behavior for the Spring URITemplate matcher
        doTestForURITemplateMatch(uriTemplate, uri, uriTemplateExpectedMatch, varNames, varValues)

        // test the behavior for the policy set matcher
        doTestForPolicySetMatcher(uriTemplate, uri, policyMatcherExpectedMatch)
    }

    @Throws(IOException::class)
    fun testMatchPolicyUriCanonicalization() {

        val file = getPolicyFileForTest(methodName)
        val policySet = loadFromFile(file)
        val policies = policySet.policies

        val candidate = PolicyMatchCandidate()
        candidate.action = "GET"
        candidate.resourceURI = "/allowed/../not_allowed/gotcha"
        candidate.subjectIdentifier = "Edward R. Murrow"
        candidate.supplementalSubjectAttributes = emptySet()
        val matchedPolicies = this.policyMatcher.match(candidate, policies)
        Assert.assertEquals(matchedPolicies.size, 1)
        Assert.assertEquals(matchedPolicies[0].policy!!.effect, Effect.DENY)
    }

    @DataProvider(name = "uriDataProviderDifferentMatchBehavior")
    private fun uriDataProviderWithSlash(): Array<Array<out Any?>> {
        return arrayOf(
            /**
             * Each entry has the following data: uriTemplate, HTTP request URI, URITemplate expected match
             * result, Policy Matcher expected match result, Expected expanded URITemplate variable values
             */

            arrayOf(
                "/asset/",
                "/asset",
                java.lang.Boolean.FALSE,
                java.lang.Boolean.TRUE,
                arrayOf("n/a"),
                arrayOf("n/a")
            ),
            arrayOf(
                "/asset",
                "/asset/",
                java.lang.Boolean.FALSE,
                java.lang.Boolean.TRUE,
                arrayOf("n/a"),
                arrayOf("n/a")
            ),
            arrayOf(
                "/sites/{site_id}/",
                "/sites/sanramon",
                java.lang.Boolean.FALSE,
                java.lang.Boolean.TRUE,
                arrayOf("n/a"),
                arrayOf("n/a")
            ),
            arrayOf(
                "/myservice/{version}/stuff",
                "/myservice/v1/stuff/",
                java.lang.Boolean.FALSE,
                java.lang.Boolean.TRUE,
                arrayOf("n/a"),
                arrayOf("n/a")
            )
        )
    }
}
