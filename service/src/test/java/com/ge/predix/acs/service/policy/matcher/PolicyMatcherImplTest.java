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
package com.ge.predix.acs.service.policy.matcher;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.util.UriTemplate;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.commons.policy.condition.groovy.GroovyConditionShell;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Condition;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.model.Policy;
import com.ge.predix.acs.model.PolicySet;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.service.policy.evaluation.MatchedPolicy;
import com.ge.predix.acs.service.policy.evaluation.ResourceAttributeResolver;
import com.ge.predix.acs.service.policy.evaluation.SubjectAttributeResolver;

/**
 * Unit tests for PolicyMatcher class.
 *
 * @author 212314537
 *
 */
@Test
public class PolicyMatcherImplTest {
    private static final String POLICY_DIR_PATH = "src/test/resources/policies";

    @Mock
    private PrivilegeManagementService privilegeManagementService;

    @Mock
    private ResourceAttributeResolver resourceAttributeResolver;

    @Mock
    private SubjectAttributeResolver subjectAttributeResolver;

    @InjectMocks
    private PolicyMatcher policyMatcher;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public void setup() {
        this.policyMatcher = new PolicyMatcherImpl();
        MockitoAnnotations.initMocks(this);
        when(this.resourceAttributeResolver.getResourceAttributes(any(Policy.class)))
                .thenReturn(new HashSet<Attribute>());
        BaseSubject subject = new BaseSubject("test-subject");
        subject.setAttributes(new HashSet<>());
        when(this.privilegeManagementService.getBySubjectIdentifierAndScopes(any(String.class), any(Set.class)))
                .thenReturn(subject);
    }

    @AfterMethod
    public void cleanupResourceAttributes() {
        when(this.resourceAttributeResolver.getResourceAttributes(any(Policy.class)))
                .thenReturn(new HashSet<Attribute>());
    }

    /**
     * Tests matching a blanket policy to a request.
     *
     * @throws IOException
     *             on failure to load policy required for test.
     */
    public void testMatchPolicyWithNoTarget() throws IOException {
        File file = getPolicyFileForTest(getMethodName());
        PolicySet policySet = PolicySets.loadFromFile(file);
        List<Policy> policies = policySet.getPolicies();

        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setAction("GET");
        candidate.setResourceURI("/this/does/not/exist");
        candidate.setSubjectIdentifier("Edward R. Murrow");
        candidate.setSupplementalResourceAttributes(Collections.emptySet());
        candidate.setSupplementalSubjectAttributes(Collections.emptySet());
        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        Assert.assertEquals(matchedPolicies.size(), 1);
        Assert.assertEquals(matchedPolicies.get(0).getPolicy(), policies.get(policies.size() - 1));
        Assert.assertNull(matchedPolicies.get(0).getPolicy().getTarget());
    }

    /**
     * Tests matching a blanket policy to a request.
     *
     * @throws IOException
     *             on failure to load policy required for test.
     */
    public void testMatchPolicyWithNoTargetAction() throws IOException {
        File file = getPolicyFileForTest(getMethodName());
        PolicySet policySet = PolicySets.loadFromFile(file);
        List<Policy> policies = policySet.getPolicies();

        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setAction("GET");
        candidate.setResourceURI("/this/does/not/exist");
        candidate.setSubjectIdentifier("Edward R. Murrow");
        candidate.setSupplementalResourceAttributes(Collections.emptySet());
        candidate.setSupplementalSubjectAttributes(Collections.emptySet());
        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        Assert.assertEquals(matchedPolicies.size(), 1);
        Assert.assertEquals(matchedPolicies.get(0).getPolicy(), policies.get(policies.size() - 1));
        Assert.assertNull(matchedPolicies.get(0).getPolicy().getTarget());
    }

    /**
     * Tests matching a policy that does not specify a subject (i.e. applies to all subjects) to a request.
     *
     * @throws IOException
     *             on failure to load policy required for test.
     */
    public void testMatchPolicyWithNoSubject() throws IOException {
        File file = getPolicyFileForTest(getMethodName());
        PolicySet policySet = PolicySets.loadFromFile(file);
        List<Policy> policies = policySet.getPolicies();

        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setAction("GET");
        candidate.setResourceURI("/public");
        candidate.setSubjectIdentifier("Edward R. Murrow");
        candidate.setSupplementalResourceAttributes(Collections.emptySet());
        candidate.setSupplementalSubjectAttributes(Collections.emptySet());
        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        Assert.assertEquals(matchedPolicies.size(), 1);
        Assert.assertEquals(matchedPolicies.get(0).getPolicy(), policies.get(policies.size() - 1));
        Assert.assertNull(matchedPolicies.get(0).getPolicy().getTarget().getSubject());
    }

    /**
     * Tests matching a policy that does not specify a resource (i.e. applies to all resources) to a request.
     *
     * @throws IOException
     *             on failure to load policy required for test.
     */
    public void testMatchPolicyWithNoResource() throws IOException {
        File file = getPolicyFileForTest(getMethodName());
        PolicySet policySet = PolicySets.loadFromFile(file);
        List<Policy> policies = policySet.getPolicies();

        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setAction("GET");
        candidate.setResourceURI("/public");
        candidate.setSubjectIdentifier("Edward R. Murrow");
        candidate.setSupplementalSubjectAttributes(new HashSet<>(
                Arrays.asList(new Attribute[] { new Attribute("https://acs.attributes.int", "role", "admin") })));
        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        Assert.assertEquals(matchedPolicies.size(), 1);
        Assert.assertEquals(matchedPolicies.get(0).getPolicy(), policies.get(policies.size() - 1));
        Assert.assertNull(matchedPolicies.get(0).getPolicy().getTarget().getResource());
    }

    /**
     * Tests matching a policy that requires the subject to have a particular attribute and the resource to have a
     * particular attribute.
     *
     * @throws IOException
     *             on failure to load policy required for test.
     */
    public void testMatchPolicyWithSubjectAndResourceAttributeRequirements() throws IOException {
        File file = getPolicyFileForTest(getMethodName());
        PolicySet policySet = PolicySets.loadFromFile(file);

        Attribute groupAttr = new Attribute("https://acs.attributes.int", "group", "gog");
        Set<Attribute> resourceAttributes = new HashSet<>();
        resourceAttributes.add(groupAttr);

        BaseResource testResource = new BaseResource();
        testResource.setAttributes(resourceAttributes);
        testResource.setResourceIdentifier("/assets/1123");

        when(this.privilegeManagementService.getByResourceIdentifier("/assets/1123")).thenReturn(testResource);
        when(this.privilegeManagementService.getByResourceIdentifierWithInheritedAttributes("/assets/1123")).thenReturn(testResource);

        List<Policy> policies = policySet.getPolicies();
        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setAction("GET");
        candidate.setResourceURI("/assets/1123");
        candidate.setSubjectIdentifier("Edward R. Murrow");
        candidate.setSupplementalSubjectAttributes(new HashSet<>(Arrays.asList(new Attribute[] { groupAttr })));

        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        Assert.assertEquals(matchedPolicies.size(), 1);
        Assert.assertEquals(matchedPolicies.get(0).getPolicy(), policies.get(policies.size() - 1));
        Assert.assertEquals(
                matchedPolicies.get(0).getPolicy().getTarget().getResource().getAttributes().get(0).getName(), "group");
        Assert.assertEquals(
                matchedPolicies.get(0).getPolicy().getTarget().getSubject().getAttributes().get(0).getName(), "group");
    }

    /**
     * Tests matching a policy that requires the subject to have a particular attribute and the resource to have a
     * particular attribute.
     *
     * @throws IOException
     *             on failure to load policy required for test.
     */
    public void testMatchPolicyFailureBecauseMissingResourceAttribute() throws IOException {
        File file = getPolicyFileForTest(getMethodName());
        PolicySet policySet = PolicySets.loadFromFile(file);
        List<Policy> policies = policySet.getPolicies();

        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setAction("GET");
        candidate.setResourceURI("/assets/1123");

        candidate.setSubjectIdentifier("Edward R. Murrow");
        candidate.setSupplementalSubjectAttributes(new HashSet<>(
                Arrays.asList(new Attribute[] { new Attribute("https://acs.attributes.int", "group", "gog") })));
        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        Assert.assertEquals(matchedPolicies.size(), 0);
    }

    /**
     * Tests matching a policy that requires a specific URI template and a specific subject attribute.
     *
     * @throws IOException
     *             on failure to load policy required for test.
     */
    public void testMatchPolicyWithUriTemplateAndSubjectAttribute() throws IOException {
        File file = getPolicyFileForTest(getMethodName());
        PolicySet policySet = PolicySets.loadFromFile(file);
        List<Policy> policies = policySet.getPolicies();

        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setAction("GET");
        candidate.setResourceURI("/sites/1123");

        candidate.setSubjectIdentifier("Edward R. Murrow");
        Attribute siteAttr = new Attribute("https://acs.attributes.int", "site", "1123");
        candidate.setSupplementalSubjectAttributes(new HashSet<>(Arrays.asList(new Attribute[] { siteAttr })));
        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        Assert.assertEquals(matchedPolicies.size(), 1);
        Assert.assertEquals(matchedPolicies.get(0).getPolicy(), policies.get(policies.size() - 1));
        Assert.assertEquals(matchedPolicies.get(0).getPolicy().getTarget().getResource().getUriTemplate(),
                "/sites/{site_id}");
        Assert.assertEquals(
                matchedPolicies.get(0).getPolicy().getTarget().getSubject().getAttributes().get(0).getName(), "site");
    }

    /**
     * Tests a failure to match any policy to the request.
     *
     * @throws IOException
     */
    public void testMultiplePoliciesNoMatch() throws IOException {
        File file = getPolicyFileForTest(getMethodName());
        PolicySet policySet = PolicySets.loadFromFile(file);
        List<Policy> policies = policySet.getPolicies();

        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setAction("GET");
        candidate.setResourceURI("/this/does/not/exist");
        candidate.setSubjectIdentifier("Edward R. Murrow");
        candidate.setSupplementalSubjectAttributes(Collections.emptySet());
        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        Assert.assertEquals(matchedPolicies.size(), 0);
    }

    public void testMultiplePoliciesMultipleMatches() throws IOException {
        File file = getPolicyFileForTest("policySetWithOverlappingURIs");
        PolicySet policySet = PolicySets.loadFromFile(file);
        List<Policy> policies = policySet.getPolicies();

        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setResourceURI("/alarm/site/site45/asset/asset46");
        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        Assert.assertEquals(matchedPolicies.size(), 2);
    }

    public void testMultiplePoliciesOneMatch() throws IOException {
        File file = getPolicyFileForTest("policySetWithOverlappingURIs");
        PolicySet policySet = PolicySets.loadFromFile(file);
        List<Policy> policies = policySet.getPolicies();

        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setResourceURI("/alarm/site/site45");
        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        Assert.assertEquals(matchedPolicies.size(), 1);
    }

    /**
     * This is test to make sure the policy matcher has the same behavior as regular Spring URITemplate for the given
     * data provider: policySetMatcherDataProvider.
     *
     * @param uriTemplate
     *            The URI template that will be used in the policy set
     * @param uri
     *            the HTTP Request URI
     * @param policyMatcherExpectedMatch
     *            Expected behavior from the Policy Matcher
     * @param varNames
     *            (not used)
     * @param varValues
     *            (not used)
     */
    @Test(dataProvider = "policySetMatcherDataProvider")
    public void testPolicySetMatcher(final String uriTemplate, final String uri,
            final Boolean policyMatcherExpectedMatch) throws IOException {
        doTestForPolicySetMatcher(uriTemplate, uri, policyMatcherExpectedMatch);
    }

    private void doTestForPolicySetMatcher(final String uriTemplate, final String uri,
            final Boolean policyMatcherExpectedMatch) throws IOException {
        PolicySet policySet = PolicySets.loadFromFile(getPolicyFileForTest("singlePolicyNoCondition"));

        policySet.getPolicies().get(0).getTarget().getResource().setUriTemplate(uriTemplate);
        List<Policy> policies = policySet.getPolicies();

        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setAction("GET");
        candidate.setResourceURI(uri);
        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        int expectedSize = policyMatcherExpectedMatch ? 1 : 0;
        Assert.assertEquals(matchedPolicies.size(), expectedSize, "Policy match count is incorrect");
    }

    /**
     * Tests matching multiple actions to a request.
     *
     * @throws IOException
     *             on failure to load policy required for test.
     */
    public void testMatchPolicyWithMultipleActions() throws IOException {
        File file = getPolicyFileForTest(getMethodName());
        PolicySet policySet = PolicySets.loadFromFile(file);
        List<Policy> policies = policySet.getPolicies();

        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setAction("GET");
        candidate.setResourceURI("/assets/45");
        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        Assert.assertEquals(matchedPolicies.size(), 1);
    }

    public void testMatchPolicyWithMultipleActionsNoMatch() throws IOException {
        File file = getPolicyFileForTest(getMethodName());
        PolicySet policySet = PolicySets.loadFromFile(file);
        List<Policy> policies = policySet.getPolicies();

        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setAction("GET");
        candidate.setResourceURI("/assets/45");
        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        Assert.assertEquals(matchedPolicies.size(), 0);
    }

    public void testApmPolicySetLoadsSuccessfully() throws Exception {
        File file = getPolicyFileForTest(getMethodName());
        PolicySet policySet = PolicySets.loadFromFile(file);
        List<Policy> policies = policySet.getPolicies();

        GroovyConditionShell shell = new GroovyConditionShell();
        for (Policy policy : policies) {
            for (Condition condition : policy.getConditions()) {
                shell.parse(condition.getCondition());
            }
        }

        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setAction("GET");
        candidate.setResourceURI("/assets/45");
        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        Assert.assertEquals(matchedPolicies.size(), 1);
        Assert.assertEquals(matchedPolicies.get(0).getPolicy().getEffect(), Effect.DENY);
    }

    /**
     * This test allows us to understand the behavior of the regular Spring URITemplate.
     *
     * @param uriTemplate
     *            The URI template that will be used
     * @param uri
     *            the HTTP Request URI
     * @param uriTemplateExpectedMatch
     *            Expected behavior from the regular URITemplate
     * @param varNames
     *            Expanded variable names
     * @param varValues
     *            Expanded variable values
     */
    @Test(dataProvider = "uriTemplateMatchDataProvider")
    public void testURITemplateMatch(final String uriTemplate, final String uri,

            final Boolean uriTemplateExpectedMatch, final String[] varNames, final String[] varValues) {
        doTestForURITemplateMatch(uriTemplate, uri, uriTemplateExpectedMatch, varNames, varValues);
    }

    private void doTestForURITemplateMatch(final String uriTemplate, final String uri,
            final Boolean uriTemplateExpectedMatch, final String[] varNames, final String[] varValues) {
        UriTemplate template = new UriTemplate(uriTemplate);
        Assert.assertEquals(template.matches(uri), uriTemplateExpectedMatch.booleanValue());

        Map<String, String> matchedVariables = template.match(uri);
        for (int i = 0; i < varNames.length; i++) {
            // skip variable match if name is "n/a"
            if (varNames[i].equals("n/a")) {
                continue;
            }

            Assert.assertEquals(matchedVariables.get(varNames[i]), varValues[i]);
            Assert.assertEquals(matchedVariables.get(varNames[i]), varValues[i]);
        }
    }

    @DataProvider(name = "uriTemplateMatchDataProvider")
    private Object[][] uriTemplateMatchDataProvider() {
        return new Object[][] {
                /**
                 * Each entry has the following data: uriTemplate, HTTP request URI, URITemplate expected match
                 * result, Expected expanded URITemplate variable values
                 */
                // exact match
                { "/one/{var1}", "/one/two", Boolean.TRUE, new String[] { "var1" }, new String[] { "two" } },
                { "/one/{var1}", "/one", Boolean.FALSE, new String[] { "n/a" }, new String[] { "n/a" } },

                // extra sub-path
                { "/one/{var1}", "/one/two/three", Boolean.TRUE, new String[] { "var1" },
                        new String[] { "two/three" } },

                // var matches multiple sub-paths.
                { "/one/{var1}/two", "/one/stuff/in/between/two", Boolean.TRUE, new String[] { "var1" },
                        new String[] { "stuff/in/between" } },

                // variable matches right-most sub-path
                { "/one/{var1}/two/{var2}/four", "/one/two/two/two/three/three/four", Boolean.TRUE,
                        new String[] { "var1", "var2" }, new String[] { "two/two", "three/three" } },

                // no match
                { "/one/{var1}/two", "/one/two/three", Boolean.FALSE, new String[] { "n/a" }, new String[] { "n/a" } },
                { "/one", "/one/two", Boolean.FALSE, new String[] { "n/a" }, new String[] { "n/a" } },

                // Eager match of sub-paths (default behavior)
                { "/customer/{customer_id}", "/customer/c1", Boolean.TRUE, new String[] { "customer_id" },
                        new String[] { "c1" } },
                { "/customer/{customer_id}", "/customer/c1site/s1", Boolean.TRUE, new String[] { "customer_id" },
                        new String[] { "c1site/s1" } },

                // Exact match with sub-paths
                { "/customer/{customer_id:\\w*}", "/customer/c1", Boolean.TRUE, new String[] { "customer_id" },
                        new String[] { "c1" } },
                { "/customer/{customer_id:\\w*}", "/customer/c1/site/s1", Boolean.FALSE, new String[] { "n/a" },
                        new String[] { "n/a" } },

                { "/customer/{customer_id:\\w*}/", "/customer/c1/", Boolean.TRUE, new String[] { "customer_id" },
                        new String[] { "c1" } },
                { "/customer/{customer_id:\\w*}/", "/customer/c1/site/s1/", Boolean.FALSE, new String[] { "n/a" },
                        new String[] { "n/a" } },

                { "/customer/{customer_id:\\w*}/site/{site_id:\\w*}", "/customer/c1/site/s1", Boolean.TRUE,
                        new String[] { "customer_id", "site_id" }, new String[] { "c1", "s1" } },

                { "/customer/{customer_id:\\w*}", "/customer/c1/site/s1", Boolean.FALSE, new String[] { "n/a" },
                        new String[] { "n/a" } },

                { "/asset/", "/asset/", Boolean.TRUE, new String[] { "n/a" }, new String[] { "n/a" } },
                { "/asset", "/asset", Boolean.TRUE, new String[] { "n/a" }, new String[] { "n/a" } },
                { "/asset", "/asset//", Boolean.FALSE, new String[] { "n/a" }, new String[] { "n/a" } },
                { "/asset/", "/asset//", Boolean.FALSE, new String[] { "n/a" }, new String[] { "n/a" } },
                { "/sites/{site_id}", "/sites/sanramon/", Boolean.TRUE, new String[] { "site_id" },
                        new String[] { "sanramon/" } },
                { "/sites/{site_id}", "/sites/sanramon", Boolean.TRUE, new String[] { "site_id" },
                        new String[] { "sanramon" } },
                { "/sites/{site_id}/", "/sites/sanramon/", Boolean.TRUE, new String[] { "site_id" },
                        new String[] { "sanramon" } },
                { "/myservice/{version}/stuff", "/myservice/v1/stuff", Boolean.TRUE, new String[] { "version" },
                        new String[] { "v1" } }, };
    }

    @DataProvider(name = "policySetMatcherDataProvider")
    private Object[][] policySetMatcherDataProvider() {
        return new Object[][] {
                /**
                 * Each entry has the following data: uriTemplate, HTTP request URI, URITemplate expected match
                 * result, Expected expanded URITemplate variable values
                 */
                // exact match
                { "/one/{var1}", "/one/two", Boolean.TRUE }, { "/one/{var1}", "/one", Boolean.FALSE },

                // extra sub-path
                { "/one/{var1}", "/one/two/three", Boolean.TRUE },

                // var matches multiple sub-paths.
                { "/one/{var1}/two", "/one/stuff/in/between/two", Boolean.TRUE },

                // variable matches right-most sub-path
                { "/one/{var1}/two/{var2}/four", "/one/two/two/two/three/three/four", Boolean.TRUE },

                // no match
                { "/one/{var1}/two", "/one/two/three", Boolean.FALSE }, { "/one", "/one/two", Boolean.FALSE },

                // Eager match of sub-paths (default behavior)
                { "/customer/{customer_id}", "/customer/c1", Boolean.TRUE },
                { "/customer/{customer_id}", "/customer/c1site/s1", Boolean.TRUE },

                // Exact match with sub-paths
                { "/customer/{customer_id:\\w*}", "/customer/c1", Boolean.TRUE },
                { "/customer/{customer_id:\\w*}", "/customer/c1/site/s1", Boolean.FALSE },

                { "/customer/{customer_id:\\w*}/", "/customer/c1/", Boolean.TRUE },
                { "/customer/{customer_id:\\w*}/", "/customer/c1/site/s1/", Boolean.FALSE },

                { "/customer/{customer_id:\\w*}/site/{site_id:\\w*}", "/customer/c1/site/s1", Boolean.TRUE },

                { "/customer/{customer_id:\\w*}", "/customer/c1/site/s1", Boolean.FALSE },

                { "/asset/", "/asset/", Boolean.TRUE }, { "/asset/", "/asset", Boolean.TRUE },
                { "/asset", "/asset", Boolean.TRUE }, { "/asset", "/asset//", Boolean.TRUE },
                { "/asset/", "/asset//", Boolean.TRUE }, { "/asset/", "/asset/../asset", Boolean.TRUE },
                { "/asset/", "/asset/.", Boolean.TRUE }, { "/sites/{site_id}", "/sites/sanramon/", Boolean.TRUE },
                { "/sites/{site_id}", "/sites/sanramon", Boolean.TRUE },
                { "/sites/{site_id}/", "/sites/sanramon/", Boolean.TRUE },
                { "/myservice/{version}/stuff", "/myservice/v1/stuff", Boolean.TRUE } };
    }

    /**
     * This is the test to highlight the different behavior for the Spring URITemplate and the Policy Set Matcher with
     * which introduces a trailing slash to create canonical URI.
     */
    @Test(dataProvider = "uriDataProviderDifferentMatchBehavior")
    public void testURITemplateMatchVsPolicySetMatch(final String uriTemplate, final String uri,
            final Boolean uriTemplateExpectedMatch, final Boolean policyMatcherExpectedMatch, final String[] varNames,
            final String[] varValues) throws IOException {
        // test the behavior for the Spring URITemplate matcher
        doTestForURITemplateMatch(uriTemplate, uri, uriTemplateExpectedMatch, varNames, varValues);

        // test the behavior for the policy set matcher
        doTestForPolicySetMatcher(uriTemplate, uri, policyMatcherExpectedMatch);
    }

    public void testMatchPolicyUriCanonicalization() throws IOException {

        File file = getPolicyFileForTest(getMethodName());
        PolicySet policySet = PolicySets.loadFromFile(file);
        List<Policy> policies = policySet.getPolicies();

        PolicyMatchCandidate candidate = new PolicyMatchCandidate();
        candidate.setAction("GET");
        candidate.setResourceURI("/allowed/../not_allowed/gotcha");
        candidate.setSubjectIdentifier("Edward R. Murrow");
        candidate.setSupplementalSubjectAttributes(Collections.emptySet());
        List<MatchedPolicy> matchedPolicies = this.policyMatcher.match(candidate, policies);
        Assert.assertEquals(matchedPolicies.size(), 1);
        Assert.assertEquals(matchedPolicies.get(0).getPolicy().getEffect(), Effect.DENY);
    }

    @DataProvider(name = "uriDataProviderDifferentMatchBehavior")
    private Object[][] uriDataProviderWithSlash() {
        return new Object[][] {
                /**
                 * Each entry has the following data: uriTemplate, HTTP request URI, URITemplate expected match
                 * result, Policy Matcher expected match result, Expected expanded URITemplate variable values
                 */

                { "/asset/", "/asset", Boolean.FALSE, Boolean.TRUE, new String[] { "n/a" }, new String[] { "n/a" } },
                { "/asset", "/asset/", Boolean.FALSE, Boolean.TRUE, new String[] { "n/a" }, new String[] { "n/a" } },
                { "/sites/{site_id}/", "/sites/sanramon", Boolean.FALSE, Boolean.TRUE, new String[] { "n/a" },
                        new String[] { "n/a" } },
                { "/myservice/{version}/stuff", "/myservice/v1/stuff/", Boolean.FALSE, Boolean.TRUE,
                        new String[] { "n/a" }, new String[] { "n/a" } }, };

    }

    public static String getMethodName() {
        return Thread.currentThread().getStackTrace()[2].getMethodName();
    }

    public static File getPolicyFileForTest(final String methodName) {
        File file = new File(POLICY_DIR_PATH, methodName + ".json");
        return file;
    }
}
