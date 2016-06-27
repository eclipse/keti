package com.ge.predix.controller.test;

import static com.ge.predix.acs.testutils.XFiles.AGENT_MULDER;
import static com.ge.predix.acs.testutils.XFiles.ASCENSION_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.ASCENSION_ID;
import static com.ge.predix.acs.testutils.XFiles.BASEMENT_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.BASEMENT_SITE_ID;
import static com.ge.predix.acs.testutils.XFiles.EVIDENCE_IMPLANT_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.EVIDENCE_IMPLANT_ID;
import static com.ge.predix.acs.testutils.XFiles.EVIDENCE_SCULLYS_TESTIMONY_ID;
import static com.ge.predix.acs.testutils.XFiles.FBI;
import static com.ge.predix.acs.testutils.XFiles.FBI_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.MULDERS_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.PENTAGON_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.PENTAGON_SITE_ID;
import static com.ge.predix.acs.testutils.XFiles.SCULLYS_TESTIMONY_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.SITE_BASEMENT;
import static com.ge.predix.acs.testutils.XFiles.SPECIAL_AGENTS_GROUP;
import static com.ge.predix.acs.testutils.XFiles.SPECIAL_AGENTS_GROUP_ATTRIBUTE;
import static com.ge.predix.acs.testutils.XFiles.SPECIAL_AGENTS_GROUP_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.TOP_SECRET_CLASSIFICATION;
import static com.ge.predix.acs.testutils.XFiles.TOP_SECRET_GROUP;
import static com.ge.predix.acs.testutils.XFiles.TOP_SECRET_GROUP_ATTRIBUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.model.PolicySet;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.Parent;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.testutils.MockAcsRequestContext;
import com.ge.predix.acs.testutils.MockMvcContext;
import com.ge.predix.acs.testutils.MockSecurityContext;
import com.ge.predix.acs.testutils.TestUtils;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.ZoneService;

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(profiles = { "h2", "public", "simple-cache", "titan" })
public class PolicyEvalWithGraphDbControllerIT extends AbstractTestNGSpringContextTests {

    public static final ConfigureEnvironment CONFIGURE_ENVIRONMENT = new ConfigureEnvironment();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectWriter OBJECT_WRITER = new ObjectMapper().writer().withDefaultPrettyPrinter();
    private static final String POLICY_EVAL_URL = "v1/policy-evaluation";
    private static final String POLICY_SET_URL = "v1/policy-set";
    private static final String SUBJECT_URL = "v1/subject";
    private static final String RESOURCE_URL = "v1/resource";

    @Autowired
    private PrivilegeManagementService privilegeManagementService;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ZoneService zoneService;

    private PolicySet policySet;

    private final JsonUtils jsonUtils = new JsonUtils();
    private final TestUtils testUtils = new TestUtils();
    private Zone testZone1;
    private Zone testZone2;

    @BeforeClass
    public void setup() throws Exception {
        this.testZone1 = new TestUtils().createTestZone("PolicyEvalWithGraphDbControllerIT1");
        this.testZone2 = new TestUtils().createTestZone("PolicyEvalWithGraphDbControllerIT2");
        this.zoneService.upsertZone(this.testZone1);
        this.zoneService.upsertZone(this.testZone2);
        MockSecurityContext.mockSecurityContext(this.testZone1);
        MockAcsRequestContext.mockAcsRequestContext(this.testZone1);
        this.policySet = this.jsonUtils.deserializeFromFile("complete-sample-policy-set-2.json", PolicySet.class);
        Assert.assertNotNull(this.policySet, "complete-sample-policy-set-2.json file not found or invalid");
    }

    @AfterMethod
    public void testCleanup() {
        for (BaseSubject subject : this.privilegeManagementService.getSubjects()) {
            this.privilegeManagementService.deleteSubject(subject.getSubjectIdentifier());
        }
        for (BaseResource resource : this.privilegeManagementService.getResources()) {
            this.privilegeManagementService.deleteResource(resource.getResourceIdentifier());
        }
    }

    @Test(dataProvider = "policyEvalDataProvider")
    public void testPolicyEvaluation(final Zone zone, final PolicySet testPolicySet,
            final List<BaseResource> resourceHierarchy, final List<BaseSubject> subjectHierarchy,
            final PolicyEvaluationRequestV1 policyEvalRequest, final Effect expectedEffect) throws Exception {
        // Create policy set.
        String uri = POLICY_SET_URL + "/" + URLEncoder.encode(testPolicySet.getName(), "UTF-8");
        MockMvcContext putPolicySetContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                zone.getSubdomain(), uri);
        putPolicySetContext.getMockMvc().perform(putPolicySetContext.getBuilder()
                .contentType(MediaType.APPLICATION_JSON).content(OBJECT_WRITER.writeValueAsString(testPolicySet)))
                .andExpect(status().isCreated());

        // Create resource hierarchy.
        if (null != resourceHierarchy) {
            MockMvcContext postResourcesContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                    zone.getSubdomain(), RESOURCE_URL);
            postResourcesContext.getMockMvc()
                    .perform(postResourcesContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(resourceHierarchy)))
                    .andExpect(status().isNoContent());
        }

        // Create subject hierarchy.
        if (null != subjectHierarchy) {
            MockMvcContext postSubjectsContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                    zone.getSubdomain(), SUBJECT_URL);
            postSubjectsContext.getMockMvc()
                    .perform(postSubjectsContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(subjectHierarchy)))
                    .andExpect(status().isNoContent());
        }

        // Request policy evaluation.
        MockMvcContext postPolicyEvalContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                zone.getSubdomain(), POLICY_EVAL_URL);
        MvcResult mvcResult = postPolicyEvalContext.getMockMvc()
                .perform(postPolicyEvalContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(policyEvalRequest)))
                .andExpect(status().isOk()).andReturn();
        PolicyEvaluationResult policyEvalResult = OBJECT_MAPPER
                .readValue(mvcResult.getResponse().getContentAsByteArray(), PolicyEvaluationResult.class);
        assertThat(policyEvalResult.getEffect(), equalTo(expectedEffect));
    }

    @DataProvider(name = "policyEvalDataProvider")
    private Object[][] policyEvalDataProvider() {
        return new Object[][] { attributeInheritanceData(), scopedAttributeInheritanceData(),
                evaluationWithNoSubjectAndNoResourceData(), evaluationWithSupplementalAttributesData() };
    }

    /**
     * Test that subjects and resources inherit attributes from their parents. The policy set will permit the request
     * if the subject and resource successfully inherit the required attributes from their respective parents.
     */
    Object[] attributeInheritanceData() {
        return new Object[] { this.testZone1, this.policySet, createThreeLevelResourceHierarchy(),
                createSubjectHierarchy(), createPolicyEvalRequest("GET", EVIDENCE_SCULLYS_TESTIMONY_ID, AGENT_MULDER),
                Effect.PERMIT };
    }

    /**
     * Test that subjects inherit attributes only when accessing resources with the right attributes. The policy set
     * will deny the request if the subject accesses a resource that does not allow it inherit the required
     * attributes.
     */
    Object[] scopedAttributeInheritanceData() {
        return new Object[] { this.testZone1, this.policySet, createTwoParentResourceHierarchy(),
                createScopedSubjectHierarchy(), createPolicyEvalRequest("GET", EVIDENCE_IMPLANT_ID, AGENT_MULDER),
                Effect.DENY };
    }

    /**
     * Test that evaluation is successful even when the resource and subject do not exist. The policy set will deny
     * the request but return a successful result.
     */
    Object[] evaluationWithNoSubjectAndNoResourceData() {
        return new Object[] { this.testZone1, this.policySet, null, null,
                createPolicyEvalRequest("GET", EVIDENCE_IMPLANT_ID, AGENT_MULDER), Effect.DENY };
    }

    /**
     * Test that evaluation is successful even when the request provides the attributes. The policy set will return
     * permit because the condition is satisfied by the user provided supplemental attributes.
     */
    Object[] evaluationWithSupplementalAttributesData() {
        return new Object[] { this.testZone1, this.policySet, null, null,
                createPolicyEvalRequest("GET", EVIDENCE_IMPLANT_ID, AGENT_MULDER,
                        Arrays.asList(new Attribute[] { SPECIAL_AGENTS_GROUP_ATTRIBUTE, TOP_SECRET_CLASSIFICATION }),
                        Arrays.asList(new Attribute[] { SPECIAL_AGENTS_GROUP_ATTRIBUTE, TOP_SECRET_CLASSIFICATION })),
                Effect.PERMIT };
    }

    List<BaseSubject> createSubjectHierarchy() {
        BaseSubject fbi = new BaseSubject(FBI);
        fbi.setAttributes(FBI_ATTRIBUTES);

        BaseSubject specialAgentsGroup = new BaseSubject(SPECIAL_AGENTS_GROUP);
        specialAgentsGroup.setAttributes(SPECIAL_AGENTS_GROUP_ATTRIBUTES);
        specialAgentsGroup
                .setParents(new HashSet<>(Arrays.asList(new Parent[] { new Parent(fbi.getSubjectIdentifier()) })));

        BaseSubject topSecretGroup = new BaseSubject(TOP_SECRET_GROUP);
        topSecretGroup.setAttributes(TOP_SECRET_GROUP_ATTRIBUTES);

        BaseSubject agentMulder = new BaseSubject(AGENT_MULDER);
        agentMulder.setAttributes(MULDERS_ATTRIBUTES);
        agentMulder.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(specialAgentsGroup.getSubjectIdentifier()),
                        new Parent(topSecretGroup.getSubjectIdentifier()) })));

        return Arrays.asList(new BaseSubject[] { fbi, specialAgentsGroup, topSecretGroup, agentMulder });
    }

    List<BaseResource> createThreeLevelResourceHierarchy() {
        BaseResource basement = new BaseResource(BASEMENT_SITE_ID);
        basement.setAttributes(BASEMENT_ATTRIBUTES);

        BaseResource ascension = new BaseResource(ASCENSION_ID);
        ascension.setAttributes(ASCENSION_ATTRIBUTES);
        ascension.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(basement.getResourceIdentifier()) })));

        BaseResource scullysTestimony = new BaseResource(EVIDENCE_SCULLYS_TESTIMONY_ID);
        scullysTestimony.setAttributes(SCULLYS_TESTIMONY_ATTRIBUTES);
        scullysTestimony.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(ascension.getResourceIdentifier()) })));

        return Arrays.asList(new BaseResource[] { basement, ascension, scullysTestimony });
    }

    List<BaseResource> createTwoParentResourceHierarchy() {
        BaseResource pentagon = new BaseResource(PENTAGON_SITE_ID);
        pentagon.setAttributes(PENTAGON_ATTRIBUTES);

        BaseResource ascension = new BaseResource(ASCENSION_ID);
        ascension.setAttributes(ASCENSION_ATTRIBUTES);

        BaseResource evidenceImplant = new BaseResource(EVIDENCE_IMPLANT_ID);
        evidenceImplant.setAttributes(EVIDENCE_IMPLANT_ATTRIBUTES);
        evidenceImplant.setParents(new HashSet<>(Arrays.asList(new Parent[] {
                new Parent(pentagon.getResourceIdentifier()), new Parent(ascension.getResourceIdentifier()) })));

        return Arrays.asList(new BaseResource[] { pentagon, ascension, evidenceImplant });
    }

    List<BaseSubject> createScopedSubjectHierarchy() {
        BaseSubject fbi = new BaseSubject(FBI);
        fbi.setAttributes(FBI_ATTRIBUTES);

        BaseSubject specialAgentsGroup = new BaseSubject(SPECIAL_AGENTS_GROUP);
        specialAgentsGroup.setAttributes(SPECIAL_AGENTS_GROUP_ATTRIBUTES);
        specialAgentsGroup
                .setParents(new HashSet<>(Arrays.asList(new Parent[] { new Parent(fbi.getSubjectIdentifier()) })));

        BaseSubject topSecretGroup = new BaseSubject(TOP_SECRET_GROUP);
        topSecretGroup.setAttributes(TOP_SECRET_GROUP_ATTRIBUTES);

        BaseSubject agentMulder = new BaseSubject(AGENT_MULDER);
        agentMulder.setAttributes(MULDERS_ATTRIBUTES);
        agentMulder.setParents(new HashSet<>(Arrays.asList(new Parent[] {
                new Parent(specialAgentsGroup.getSubjectIdentifier()), new Parent(topSecretGroup.getSubjectIdentifier(),
                        new HashSet<>(Arrays.asList(new Attribute[] { SITE_BASEMENT }))) })));

        return Arrays.asList(new BaseSubject[] { fbi, specialAgentsGroup, topSecretGroup, agentMulder });
    }

    PolicyEvaluationRequestV1 createPolicyEvalRequest(final String action, final String resourceIdentifier,
            final String subjectIdentifier) {
        PolicyEvaluationRequestV1 policyEvalRequest = new PolicyEvaluationRequestV1();
        policyEvalRequest.setAction("GET");
        policyEvalRequest.setResourceIdentifier(resourceIdentifier);
        policyEvalRequest.setSubjectIdentifier(subjectIdentifier);
        return policyEvalRequest;
    }

    PolicyEvaluationRequestV1 createPolicyEvalRequest(final String action, final String resourceIdentifier,
            final String subjectIdentifier, final List<Attribute> supplementalResourceAttributes,
            final List<Attribute> supplementalSubjectAttributes) {
        PolicyEvaluationRequestV1 policyEvalRequest = new PolicyEvaluationRequestV1();
        policyEvalRequest.setAction("GET");
        policyEvalRequest.setResourceIdentifier(resourceIdentifier);
        policyEvalRequest.setSubjectIdentifier(subjectIdentifier);
        policyEvalRequest.setResourceAttributes(supplementalResourceAttributes);
        policyEvalRequest.setSubjectAttributes(supplementalSubjectAttributes);
        return policyEvalRequest;
    }
}
