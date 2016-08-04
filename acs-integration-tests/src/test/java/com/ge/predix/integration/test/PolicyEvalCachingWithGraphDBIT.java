package com.ge.predix.integration.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.Parent;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.test.TestConfig;
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.PrivilegeHelper;
import com.ge.predix.test.utils.UaaTestUtil;
import com.ge.predix.test.utils.ZacTestUtil;
import com.ge.predix.test.utils.ZoneHelper;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@SuppressWarnings({ "nls" })
public class PolicyEvalCachingWithGraphDBIT extends AbstractTestNGSpringContextTests {

    @Value("${ACS_URL:http://localhost:8181}")
    private String acsUrl;

    @Value("${UAA_URL:http://localhost:8080/uaa}")
    private String uaaUrl;

    private String acsZone1Name;

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    @Autowired
    private PolicyHelper policyHelper;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    @Autowired
    private ZacTestUtil zacTestUtil;

    @Autowired
    Environment env;

    @Autowired
    private ZoneHelper zoneHelper;
    private OAuth2RestTemplate acsAdminRestTemplate;
    private UaaTestUtil uaaTestUtil;
    private final HttpHeaders acsZone1Headers = new HttpHeaders();

    private final String ISSUER_URI = "acs.example.org";
    private final Attribute TOP_SECRET_CLASSIFICATION = new Attribute(this.ISSUER_URI, "classification", "top secret");
    private final Attribute SPECIAL_AGENTS_GROUP_ATTRIBUTE = new Attribute(this.ISSUER_URI, "group", "special-agents");

    private final String FBI = "fbi";
    private final String SPECIAL_AGENTS_GROUP = "special-agents";
    private final String AGENT_MULDER = "mulder";
    private final String AGENT_SCULLY = "scully";
    public static final String EVIDENCE_SCULLYS_TESTIMONY_ID = "/evidence/scullys-testimony";

    @BeforeClass
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        TestConfig.setupForEclipse(); // Starts ACS when running the test in eclipse.

        this.acsZone1Name = this.zoneHelper.getZone1Name();
        this.acsZone1Headers.add("Predix-Zone-Id", this.acsZone1Name);
        if (Arrays.asList(this.env.getActiveProfiles()).contains("public")) {
            setupPublicACS();
        } else {
            setupPredixACS();
        }
    }

    private void setupPredixACS() throws JsonParseException, JsonMappingException, IOException {
        this.zacTestUtil.assumeZacServerAvailable();
        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
        this.zoneHelper.createPrimaryTestZone();
    }

    private void setupPublicACS() throws JsonParseException, JsonMappingException, IOException {
        this.uaaTestUtil = new UaaTestUtil(this.acsRestTemplateFactory.getOAuth2RestTemplateForUaaAdmin(), this.uaaUrl);
        this.uaaTestUtil.setup(Arrays.asList(new String[] { this.acsZone1Name }));
        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getOAuth2RestTemplateForAcsAdmin();
        this.zoneHelper.createTestZone(this.acsAdminRestTemplate, this.acsZone1Name, false);
    }

    @AfterMethod
    public void cleanup() throws Exception {
        this.privilegeHelper.deleteResources(this.acsAdminRestTemplate, this.acsUrl, this.acsZone1Headers);
        this.privilegeHelper.deleteSubjects(this.acsAdminRestTemplate, this.acsUrl, this.acsZone1Headers);
        this.policyHelper.deletePolicySets(this.acsAdminRestTemplate, this.acsUrl, this.acsZone1Headers);
    }
    
    /**
     * This test makes sure that cached policy evaluation results are properly invalidated for a subject and its
     * descendants, when attributes are changed for the parent subject.
     */
    @Test
    public void testPolicyEvalCacheInvalidationWhenSubjectParentChanges() throws Exception {
        BaseSubject fbi = new BaseSubject(this.FBI);

        BaseSubject specialAgentsGroup = new BaseSubject(this.SPECIAL_AGENTS_GROUP);
        specialAgentsGroup
                .setParents(new HashSet<>(Arrays.asList(new Parent[] { new Parent(fbi.getSubjectIdentifier()) })));

        BaseSubject agentMulder = new BaseSubject(this.AGENT_MULDER);
        agentMulder.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(specialAgentsGroup.getSubjectIdentifier()) })));

        BaseSubject agentScully = new BaseSubject(this.AGENT_SCULLY);
        agentScully.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(specialAgentsGroup.getSubjectIdentifier()) })));

        BaseResource scullysTestimony = new BaseResource(EVIDENCE_SCULLYS_TESTIMONY_ID);

        PolicyEvaluationRequestV1 mulderPolicyEvaluationRequest = this.policyHelper
                .createEvalRequest("GET", agentMulder.getSubjectIdentifier(), EVIDENCE_SCULLYS_TESTIMONY_ID, null);
        PolicyEvaluationRequestV1 scullyPolicyEvaluationRequest = this.policyHelper
                .createEvalRequest("GET", agentScully.getSubjectIdentifier(), EVIDENCE_SCULLYS_TESTIMONY_ID, null);

        String endpoint = this.acsUrl;

        // Set up fbi <-- specialAgentsGroup <-- (agentMulder, agentScully) subject hierarchy
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, fbi, endpoint, this.acsZone1Headers);
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, specialAgentsGroup, endpoint,
                this.acsZone1Headers, this.SPECIAL_AGENTS_GROUP_ATTRIBUTE);
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, agentMulder, endpoint, this.acsZone1Headers);
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, agentScully, endpoint, this.acsZone1Headers);
        
        // Set up resource
        this.privilegeHelper.putResource(this.acsAdminRestTemplate, scullysTestimony, endpoint,
                this.acsZone1Headers, this.SPECIAL_AGENTS_GROUP_ATTRIBUTE, this.TOP_SECRET_CLASSIFICATION);

        // Set up policy
        String policyFile = "src/test/resources/policies/complete-sample-policy-set-2.json";
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint,
                policyFile);

        // Verify that policy is evaluated to DENY since top secret classification is not set
        ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(mulderPolicyEvaluationRequest, this.acsZone1Headers),
                PolicyEvaluationResult.class);
        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        PolicyEvaluationResult responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.DENY);

        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(scullyPolicyEvaluationRequest, this.acsZone1Headers),
                PolicyEvaluationResult.class);
        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.DENY);

        // Change parent subject to add top secret classification
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, specialAgentsGroup, endpoint,
                this.acsZone1Headers, this.SPECIAL_AGENTS_GROUP_ATTRIBUTE, this.TOP_SECRET_CLASSIFICATION);

        // Verify that policy is evaluated to PERMIT since top secret classification is now propogated from the parent
        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(mulderPolicyEvaluationRequest, this.acsZone1Headers),
                PolicyEvaluationResult.class);
        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(scullyPolicyEvaluationRequest, this.acsZone1Headers),
                PolicyEvaluationResult.class);
        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated for a resource and its
     * descendants, when attributes are changed for the parent resource.
     */
    @Test
    public void testPolicyEvalCacheInvalidationWhenResourceParentChanges() throws Exception {
        BaseResource grandparentResource = new BaseResource("/secured-by-value/sites/east-bay");
        BaseResource parentResource = new BaseResource("/secured-by-value/sites/sanramon");
        BaseResource childResource = new BaseResource("/secured-by-value/sites/basement");

        parentResource.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(grandparentResource.getResourceIdentifier()) })));
        
        childResource.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(parentResource.getResourceIdentifier()) })));

        BaseSubject agentMulder = new BaseSubject(this.AGENT_MULDER);

        PolicyEvaluationRequestV1 sanramonPolicyEvaluationRequest = this.policyHelper
                .createEvalRequest(agentMulder.getSubjectIdentifier(), "sanramon");
        
        PolicyEvaluationRequestV1 basementPolicyEvaluationRequest = this.policyHelper
                .createEvalRequest(agentMulder.getSubjectIdentifier(), "basement");

        String endpoint = this.acsUrl;

        this.privilegeHelper.putResource(this.acsAdminRestTemplate, grandparentResource, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getDefaultOrgAttribute());
        this.privilegeHelper.putResource(this.acsAdminRestTemplate, parentResource, endpoint, this.acsZone1Headers);
        this.privilegeHelper.putResource(this.acsAdminRestTemplate, childResource, endpoint, this.acsZone1Headers);
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, agentMulder, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getDefaultAttribute(), this.privilegeHelper.getDefaultOrgAttribute());

        String policyFile = "src/test/resources/policies/single-org-based.json";
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint,
                policyFile);

        // Subject policy evaluation request for site "sanramon"
        ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(sanramonPolicyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        PolicyEvaluationResult responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);
        
        // Subject policy evaluation request for site "basement"
        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(basementPolicyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

        // Change grandparent resource attributes from DefaultOrgAttribute to AlternateOrgAttribute
        this.privilegeHelper.putResource(this.acsAdminRestTemplate, grandparentResource, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getAlternateOrgAttribute());

        // Subject policy evaluation request for site "sanramon"
        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(sanramonPolicyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);
        
        // Subject policy evaluation request for site "basement"
        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(basementPolicyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);
    }

}
