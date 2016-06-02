package com.ge.predix.controller.test;

import static com.ge.predix.acs.testutils.Constants.ATTR_CLASSIFICATION_0;
import static com.ge.predix.acs.testutils.Constants.ATTR_GROUP_0;
import static com.ge.predix.acs.testutils.Constants.ATTR_SITE_0;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_EVIDENCE_0_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_EVIDENCE_0_ID;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_EVIDENCE_1_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_EVIDENCE_1_ID;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_SITE_0_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_SITE_0_ID;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_SITE_1_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_SITE_1_ID;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_XFILE_0_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_XFILE_0_ID;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_GROUP_0_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_GROUP_0_ID;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_GROUP_1_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_GROUP_1_ID;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_GROUP_2_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_GROUP_2_ID;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_USER_1_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_USER_1_ID;
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
    public void testPolicyEvaluation(final Zone zone, final PolicySet policySet,
            final List<BaseResource> resourceHierarchy, final List<BaseSubject> subjectHierarchy,
            final PolicyEvaluationRequestV1 policyEvalRequest, final Effect expectedEffect) throws Exception {
        // Create policy set.
        String uri = POLICY_SET_URL + "/" + URLEncoder.encode(policySet.getName(), "UTF-8");
        MockMvcContext putPolicySetContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                zone.getSubdomain(), uri);
        putPolicySetContext.getMockMvc().perform(putPolicySetContext.getBuilder()
                .contentType(MediaType.APPLICATION_JSON).content(OBJECT_WRITER.writeValueAsString(policySet)))
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
        return new Object[][] { verifyAttributeInheritance(), verifyScopedAttributeInheritance(),
                verifyEvaluationWithNoSubjectAndNoResource(), verifyEvaluationWithSupplementalAttributes() };
    }

    /**
     * Test that subjects and resources inherit attributes from their parents. The policy set will permit the request
     * if the subject and resource successfully inherit the required attributes from their respective parents.
     */
    Object[] verifyAttributeInheritance() {
        return new Object[] { this.testZone1, this.policySet, createThreeLevelResourceHierarchy(),
                createSubjectHierarchy(), createPolicyEvalRequest("GET", RESOURCE_EVIDENCE_0_ID, SUBJECT_USER_1_ID),
                Effect.PERMIT };
    }

    /**
     * Test that subjects inherit attributes only when accessing resources with the right attributes. The policy set
     * will deny the request if the subject accesses a resource that does not allow it inherit the required
     * attributes.
     */
    Object[] verifyScopedAttributeInheritance() {
        return new Object[] { this.testZone1, this.policySet, createTwoParentResourceHierarchy(),
                createScopedSubjectHierarchy(),
                createPolicyEvalRequest("GET", RESOURCE_EVIDENCE_1_ID, SUBJECT_USER_1_ID), Effect.DENY };
    }

    /**
     * Test that evaluation is successful even when the resource and subject do not exist. The policy set will deny
     * the request but return a successful result.
     */
    Object[] verifyEvaluationWithNoSubjectAndNoResource() {
        return new Object[] { this.testZone1, this.policySet, null, null,
                createPolicyEvalRequest("GET", RESOURCE_EVIDENCE_1_ID, SUBJECT_USER_1_ID), Effect.DENY };
    }

    /**
     * Test that evaluation is successful even when the request provides the attributes. The policy set will return
     * permit because the condition is satisfied by the user provided supplemental attributes.
     */
    Object[] verifyEvaluationWithSupplementalAttributes() {
        return new Object[] { this.testZone1, this.policySet, null, null,
                createPolicyEvalRequest("GET", RESOURCE_EVIDENCE_1_ID, SUBJECT_USER_1_ID,
                        Arrays.asList(new Attribute[] { ATTR_GROUP_0, ATTR_CLASSIFICATION_0 }),
                        Arrays.asList(new Attribute[] { ATTR_GROUP_0, ATTR_CLASSIFICATION_0 })),
                Effect.PERMIT };
    }

    List<BaseSubject> createSubjectHierarchy() {
        BaseSubject subject0 = new BaseSubject(SUBJECT_GROUP_0_ID);
        subject0.setAttributes(SUBJECT_GROUP_0_ATTRS_0);

        BaseSubject subject1 = new BaseSubject(SUBJECT_GROUP_1_ID);
        subject1.setAttributes(SUBJECT_GROUP_1_ATTRS_0);
        subject1.setParents(new HashSet<>(Arrays.asList(new Parent[] { new Parent(subject0.getSubjectIdentifier()) })));

        BaseSubject subject3 = new BaseSubject(SUBJECT_GROUP_2_ID);
        subject3.setAttributes(SUBJECT_GROUP_2_ATTRS_0);

        BaseSubject subject2 = new BaseSubject(SUBJECT_USER_1_ID);
        subject2.setAttributes(SUBJECT_USER_1_ATTRS_0);
        subject2.setParents(new HashSet<>(Arrays.asList(new Parent[] { new Parent(subject1.getSubjectIdentifier()),
                new Parent(subject3.getSubjectIdentifier()) })));

        return Arrays.asList(new BaseSubject[] { subject0, subject1, subject3, subject2 });
    }

    List<BaseResource> createThreeLevelResourceHierarchy() {
        BaseResource resource0 = new BaseResource(RESOURCE_SITE_0_ID);
        resource0.setAttributes(RESOURCE_SITE_0_ATTRS_0);

        BaseResource resource1 = new BaseResource(RESOURCE_XFILE_0_ID);
        resource1.setAttributes(RESOURCE_XFILE_0_ATTRS_0);
        resource1.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(resource0.getResourceIdentifier()) })));

        BaseResource resource2 = new BaseResource(RESOURCE_EVIDENCE_0_ID);
        resource2.setAttributes(RESOURCE_EVIDENCE_0_ATTRS_0);
        resource2.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(resource1.getResourceIdentifier()) })));

        return Arrays.asList(new BaseResource[] { resource0, resource1, resource2 });
    }

    List<BaseResource> createTwoParentResourceHierarchy() {
        BaseResource resource0 = new BaseResource(RESOURCE_SITE_1_ID);
        resource0.setAttributes(RESOURCE_SITE_1_ATTRS_0);

        BaseResource resource1 = new BaseResource(RESOURCE_XFILE_0_ID);
        resource1.setAttributes(RESOURCE_XFILE_0_ATTRS_0);

        BaseResource resource2 = new BaseResource(RESOURCE_EVIDENCE_1_ID);
        resource2.setAttributes(RESOURCE_EVIDENCE_1_ATTRS_0);
        resource2.setParents(new HashSet<>(Arrays.asList(new Parent[] { new Parent(resource0.getResourceIdentifier()),
                new Parent(resource1.getResourceIdentifier()) })));

        return Arrays.asList(new BaseResource[] { resource0, resource1, resource2 });
    }

    List<BaseSubject> createScopedSubjectHierarchy() {
        BaseSubject subject0 = new BaseSubject(SUBJECT_GROUP_0_ID);
        subject0.setAttributes(SUBJECT_GROUP_0_ATTRS_0);

        BaseSubject subject1 = new BaseSubject(SUBJECT_GROUP_1_ID);
        subject1.setAttributes(SUBJECT_GROUP_1_ATTRS_0);
        subject1.setParents(new HashSet<>(Arrays.asList(new Parent[] { new Parent(subject0.getSubjectIdentifier()) })));

        BaseSubject subject3 = new BaseSubject(SUBJECT_GROUP_2_ID);
        subject3.setAttributes(SUBJECT_GROUP_2_ATTRS_0);

        BaseSubject subject2 = new BaseSubject(SUBJECT_USER_1_ID);
        subject2.setAttributes(SUBJECT_USER_1_ATTRS_0);
        subject2.setParents(new HashSet<>(Arrays.asList(
                new Parent[] { new Parent(subject1.getSubjectIdentifier()), new Parent(subject3.getSubjectIdentifier(),
                        new HashSet<>(Arrays.asList(new Attribute[] { ATTR_SITE_0 }))) })));

        return Arrays.asList(new BaseSubject[] { subject0, subject1, subject3, subject2 });
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
