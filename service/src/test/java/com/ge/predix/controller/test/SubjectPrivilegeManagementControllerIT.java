// @formatter:off
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

package com.ge.predix.controller.test;

import static com.ge.predix.acs.testutils.XFiles.AGENT_MULDER;
import static com.ge.predix.acs.testutils.XFiles.FBI;
import static com.ge.predix.acs.testutils.XFiles.SITE_BASEMENT;
import static com.ge.predix.acs.testutils.XFiles.SITE_QUANTICO;
import static com.ge.predix.acs.testutils.XFiles.SPECIAL_AGENTS_GROUP;
import static com.ge.predix.acs.testutils.XFiles.SPECIAL_AGENTS_GROUP_ATTRIBUTE;
import static com.ge.predix.acs.testutils.XFiles.TOP_SECRET_CLASSIFICATION;
import static com.ge.predix.acs.testutils.XFiles.TOP_SECRET_GROUP;
import static com.ge.predix.acs.testutils.XFiles.createSubjectHierarchy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import javax.security.auth.Subject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.testutils.MockAcsRequestContext;
import com.ge.predix.acs.testutils.MockMvcContext;
import com.ge.predix.acs.testutils.MockSecurityContext;
import com.ge.predix.acs.testutils.TestActiveProfilesResolver;
import com.ge.predix.acs.testutils.TestUtils;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.ZoneService;

/**
 *
 * @author 212319607
 */
@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
public class SubjectPrivilegeManagementControllerIT extends AbstractTestNGSpringContextTests {

    public static final ConfigureEnvironment CONFIGURE_ENVIRONMENT = new ConfigureEnvironment();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private WebApplicationContext wac;

    private final JsonUtils jsonUtil = new JsonUtils();
    private final TestUtils testUtils = new TestUtils();

    private Zone testZone;
    private Zone testZone2;

    private static final String SUBJECT_BASE_URL = "/v1/subject";

    @Autowired
    ConfigurableEnvironment env;

    @BeforeClass
    public void setup() throws Exception {
        this.testZone = new TestUtils().createTestZone("SubjectMgmtControllerIT");
        this.testZone2 = new TestUtils().createTestZone("SubjectMgmtControllerIT2");
        this.zoneService.upsertZone(this.testZone);
        this.zoneService.upsertZone(this.testZone2);
        MockSecurityContext.mockSecurityContext(this.testZone);
        MockAcsRequestContext.mockAcsRequestContext(this.testZone);
    }

    @Test
    public void testSameSubjectDifferentZones() throws Exception {
        BaseSubject subject = this.jsonUtil.deserializeFromFile("controller-test/a-subject.json", BaseSubject.class);
        String thisUri = SUBJECT_BASE_URL + "/dave";
        // create subject in first zone
        MockMvcContext putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))).andExpect(status().isCreated());

        // create subject in second zone
        MockSecurityContext.mockSecurityContext(this.testZone2);
        MockAcsRequestContext.mockAcsRequestContext(this.testZone2);

        putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone2.getSubdomain(),
                thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))).andExpect(status().isCreated());
        // we expect both subjects to be create in each zone
        // set security context back to first test zone
        MockSecurityContext.mockSecurityContext(this.testZone);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSubjects() throws Exception {
        List<Subject> subjects = this.jsonUtil.deserializeFromFile("controller-test/subjects-collection.json",
                List.class);
        Assert.assertNotNull(subjects);

        // Append a list of subjects
        MockMvcContext postContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subjects))).andExpect(status().isNoContent());

        // Get the list of subjects
        MockMvcContext getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), SUBJECT_BASE_URL);

        ResultActions resultActions = getContext.getMockMvc().perform(getContext.getBuilder());
        assertSubjects(resultActions, 2, new String[] { "dave", "vineet" });
        assertSubjectsAttributes(resultActions, 2, 2, new String[] { "group", "department" },
                new String[] { "sales", "admin" }, new String[] { "https://acs.attributes.int" });

        BaseSubject subject = this.jsonUtil.deserializeFromFile("controller-test/a-subject.json", BaseSubject.class);
        Assert.assertNotNull(subject);

        // Update a given subject

        String thisUri = SUBJECT_BASE_URL + "/dave";

        MockMvcContext putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))).andExpect(status().isNoContent());

        // Get a given subject
        getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(),
                thisUri);
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isOk())
                .andExpect(jsonPath("subjectIdentifier", is("dave")))
                .andExpect(jsonPath("attributes[0].value", isIn(new String[] { "supervisor", "it" })))
                .andExpect(jsonPath("attributes[0].issuer", is("https://acs.attributes.int")));

        // Delete resources from created collection
        thisUri = SUBJECT_BASE_URL + "/vineet";
        MockMvcContext deleteContext = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());
        // Make sure subject does not exist anymore
        getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(),
                thisUri);
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isNotFound());
        thisUri = SUBJECT_BASE_URL + "/dave";
        deleteContext = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone.getSubdomain(),
                thisUri);
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());

        // Make sure subject does not exist anymore
        getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(),
                thisUri);
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isNotFound());
    }

    /*
     * This test posts a collection of subjects where one is missing an subject identifier
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testPOSTSubjectsMissingSubjectIdentifier() throws Exception {

        List<Subject> subjects = this.jsonUtil
                .deserializeFromFile("controller-test/missing-subjectidentifier-collection.json", List.class);

        Assert.assertNotNull(subjects);

        // Append a list of subjects
        MockMvcContext postContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        postContext.getMockMvc()
                .perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(subjects)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPOSTSubjectsEmptyIdentifier() throws Exception {

        List<Subject> subjects = this.jsonUtil
                .deserializeFromFile("controller-test/empty-identifier-subjects-collection.json", List.class);
        Assert.assertNotNull(subjects);

        // Append a list of subjects
        MockMvcContext postContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        postContext.getMockMvc()
                .perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(subjects)))
                .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPUTSubjectIdentifierMismatch() throws Exception {
        BaseSubject subject = this.jsonUtil.deserializeFromFile("controller-test/a-mismatched-subjectidentifier.json",
                BaseSubject.class);
        Assert.assertNotNull(subject);

        // Update a given resource
        String thisUri = SUBJECT_BASE_URL + "/dave";
        MockMvcContext putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc()
                .perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(subject)))
                .andExpect(status().isUnprocessableEntity());

    }

    /*
     * This tests putting a single subject that is does not have a subject identifier
     */
    @Test
    public void testPUTSubjectNoSubjectIdentifier() throws Exception {

        BaseSubject subject = this.jsonUtil.deserializeFromFile("controller-test/no-subjectidentifier-subject.json",
                BaseSubject.class);
        Assert.assertNotNull(subject);

        // Update a given resource
        String thisUri = SUBJECT_BASE_URL + "/fermin";
        MockMvcContext putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))).andExpect(status().is2xxSuccessful());

        // Get a given resource
        MockMvcContext getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        ResultActions resultActions = getContext.getMockMvc().perform(getContext.getBuilder());

        resultActions.andExpect(status().isOk()).andExpect(jsonPath("subjectIdentifier", is("fermin")))
                .andExpect(jsonPath("attributes[0].value", isIn(new String[] { "sales", "admin" })))
                .andExpect(jsonPath("attributes[0].issuer", is("https://acs.attributes.int")));

        // Delete a given resource
        MockMvcContext deleteContext = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());

        // Make sure subject does not exist anymore
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isNotFound());
    }

    @Test
    public void testPUTCreateSubjectThenUpdateNoSubjectIdentifier() throws Exception {

        BaseSubject subject = this.jsonUtil.deserializeFromFile("controller-test/with-subjectidentifier-subject.json",
                BaseSubject.class);
        Assert.assertNotNull(subject);

        String subjectIdentifier = "fermin";
        String thisUri = SUBJECT_BASE_URL + "/" + subjectIdentifier;

        MockMvcContext putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))).andExpect(status().isCreated());

        MockMvcContext getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        ResultActions resultActions = getContext.getMockMvc().perform(getContext.getBuilder());

        resultActions.andExpect(status().isOk()).andExpect(jsonPath("subjectIdentifier", is(subjectIdentifier)))
                .andExpect(jsonPath("attributes[0].value", isIn(new String[] { "admin", "sales" })))
                .andExpect(jsonPath("attributes[0].issuer", is("https://acs.attributes.int")));

        BaseSubject subjectNoSubjectIdentifier = this.jsonUtil
                .deserializeFromFile("controller-test/no-subjectidentifier-subject.json", BaseSubject.class);
        Assert.assertNotNull(subjectNoSubjectIdentifier);

        putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone.getSubdomain(),
                thisUri);
        putContext.getMockMvc()
                .perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(subjectNoSubjectIdentifier)))
                .andExpect(status().isNoContent());

        getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(),
                thisUri);
        resultActions = getContext.getMockMvc().perform(getContext.getBuilder());

        resultActions.andExpect(status().isOk()).andExpect(jsonPath("subjectIdentifier", is(subjectIdentifier)))
                .andExpect(jsonPath("attributes[0].value", isIn(new String[] { "admin", "sales" })))
                .andExpect(jsonPath("attributes[0].issuer", is("https://acs.attributes.int")));

        // Delete a given resource
        MockMvcContext deleteContext = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPOSTCreateSubjectThenUpdateAttributes() throws Exception {
        // appending two subjects, the key one for this test is dave.
        List<BaseSubject> subjects = this.jsonUtil.deserializeFromFile("controller-test/subjects-collection.json",
                List.class);
        Assert.assertNotNull(subjects);

        // Append a list of subjects
        MockMvcContext postContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subjects))).andExpect(status().isNoContent());

        MockMvcContext getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        ResultActions resultActions = getContext.getMockMvc().perform(getContext.getBuilder());

        assertSubjects(resultActions, 2, new String[] { "dave", "vineet" });
        assertSubjectsAttributes(resultActions, 2, 2, new String[] { "group", "department" },
                new String[] { "admin", "sales" }, new String[] { "https://acs.attributes.int" });

        subjects = this.jsonUtil
                .deserializeFromFile("controller-test/subjects-collection-with-different-attributes.json", List.class);
        Assert.assertNotNull(subjects);

        postContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone.getSubdomain(),
                SUBJECT_BASE_URL);
        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subjects))).andExpect(status().isNoContent());

        getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(),
                SUBJECT_BASE_URL);
        resultActions = getContext.getMockMvc().perform(getContext.getBuilder());
        assertSubjects(resultActions, 2, new String[] { "dave", "vineet" });
        assertSubjectsAttributes(resultActions, 2, 2, new String[] { "group", "department" },
                new String[] { "different", "sales" }, new String[] { "https://acs.attributes.int" });

        // delete dave
        MockMvcContext deleteContext = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac,
                this.testZone.getSubdomain(), SUBJECT_BASE_URL + "/dave");
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());

        // delete vineet
        deleteContext = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone.getSubdomain(),
                SUBJECT_BASE_URL + "/vineet");
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());

    }

    @Test
    public void testPostAndGetHierarchicalSubjects() throws Exception {

        List<BaseSubject> subjects = createSubjectHierarchy();

        // Append a list of resources
        MockMvcContext postContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subjects))).andExpect(status().isNoContent());

        // Get the child subject without query string specifier.
        MockMvcContext getContext0 = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), SUBJECT_BASE_URL + "/"
                        + URLEncoder.encode(AGENT_MULDER, "UTF-8"));

        getContext0.getMockMvc().perform(getContext0.getBuilder()).andExpect(status().isOk())
            .andExpect(jsonPath("subjectIdentifier", is(AGENT_MULDER)))
            .andExpect(jsonPath("attributes[0].name", is(SITE_BASEMENT.getName())))
            .andExpect(jsonPath("attributes[0].value", is(SITE_BASEMENT.getValue())))
            .andExpect(jsonPath("attributes[0].issuer", is(SITE_BASEMENT.getIssuer())))
            .andExpect(jsonPath("attributes[1]").doesNotExist());

        // Get the child subject without inherited attributes.
        MockMvcContext getContext1 = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), SUBJECT_BASE_URL + "/"
                        + URLEncoder.encode(AGENT_MULDER, "UTF-8")
                        + "?includeInheritedAttributes=false");

        getContext1.getMockMvc().perform(getContext1.getBuilder()).andExpect(status().isOk())
            .andExpect(jsonPath("subjectIdentifier", is(AGENT_MULDER)))
            .andExpect(jsonPath("attributes[0].name", is(SITE_BASEMENT.getName())))
            .andExpect(jsonPath("attributes[0].value", is(SITE_BASEMENT.getValue())))
            .andExpect(jsonPath("attributes[0].issuer", is(SITE_BASEMENT.getIssuer())))
            .andExpect(jsonPath("attributes[1]").doesNotExist());

        // Get the child subject with inherited attributes.
        MockMvcContext getContext2 = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), SUBJECT_BASE_URL + "/"
                        + URLEncoder.encode(AGENT_MULDER, "UTF-8")
                        + "?includeInheritedAttributes=true");

        if (!Arrays.asList(this.env.getActiveProfiles()).contains("titan")) {
            getContext2.getMockMvc().perform(getContext2.getBuilder()).andExpect(status().isOk())
                .andExpect(jsonPath("subjectIdentifier", is(AGENT_MULDER)))
                .andExpect(jsonPath("attributes[0].name", is(SITE_BASEMENT.getName())))
                .andExpect(jsonPath("attributes[0].value", is(SITE_BASEMENT.getValue())))
                .andExpect(jsonPath("attributes[0].issuer", is(SITE_BASEMENT.getIssuer())))
                .andExpect(jsonPath("attributes[1]").doesNotExist());
        } else {
            getContext2.getMockMvc().perform(getContext2.getBuilder()).andExpect(status().isOk())
                .andExpect(jsonPath("subjectIdentifier", is(AGENT_MULDER)))
                .andExpect(jsonPath("attributes[0].name", is(TOP_SECRET_CLASSIFICATION.getName())))
                .andExpect(jsonPath("attributes[0].value", is(TOP_SECRET_CLASSIFICATION.getValue())))
                .andExpect(jsonPath("attributes[0].issuer", is(TOP_SECRET_CLASSIFICATION.getIssuer())))
                .andExpect(jsonPath("attributes[1].name", is(SITE_QUANTICO.getName())))
                .andExpect(jsonPath("attributes[1].value", is(SITE_QUANTICO.getValue())))
                .andExpect(jsonPath("attributes[1].issuer", is(SITE_QUANTICO.getIssuer())))
                .andExpect(jsonPath("attributes[2].name", is(SPECIAL_AGENTS_GROUP_ATTRIBUTE.getName())))
                .andExpect(jsonPath("attributes[2].value", is(SPECIAL_AGENTS_GROUP_ATTRIBUTE.getValue())))
                .andExpect(jsonPath("attributes[2].issuer", is(SPECIAL_AGENTS_GROUP_ATTRIBUTE.getIssuer())))
                .andExpect(jsonPath("attributes[3].name", is(SITE_BASEMENT.getName())))
                .andExpect(jsonPath("attributes[3].value", is(SITE_BASEMENT.getValue())))
                .andExpect(jsonPath("attributes[3].issuer", is(SITE_BASEMENT.getIssuer())));
        }

        // Clean up after ourselves.
        deleteSubject(AGENT_MULDER);
        deleteSubject(TOP_SECRET_GROUP);
        deleteSubject(SPECIAL_AGENTS_GROUP);
        deleteSubject(FBI);
    }

    private void deleteSubject(final String subjectIdentifier) throws Exception {
        String subjectToDeleteURI = SUBJECT_BASE_URL + "/" + URLEncoder.encode(subjectIdentifier, "UTF-8");
        MockMvcContext deleteContext = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac,
                this.testZone.getSubdomain(), subjectToDeleteURI);
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPOSTCreateSubjectNoDuplicates() throws Exception {
        // appending two subjects, the key one for this test is dave.

        List<BaseSubject> subjects = this.jsonUtil
                .deserializeFromFile("controller-test/a-single-subject-collection.json", List.class);
        Assert.assertNotNull(subjects);

        // Append a list of subjects
        MockMvcContext postContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subjects))).andExpect(status().isNoContent());

        MockMvcContext getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        ResultActions resultActions = getContext.getMockMvc().perform(getContext.getBuilder());
        assertSubjects(resultActions, 1, new String[] { "dave" });

        MockMvcContext deleteContext = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac,
                this.testZone.getSubdomain(), SUBJECT_BASE_URL + "/dave");
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());
    }

    private void assertSubjects(final ResultActions resultActions, final int size, final String[] identifiers)
            throws Exception {
        resultActions.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(size)));

        for (int i = 0; i < size; i++) {
            String subjectIdentifierPath = String.format("$[%s].subjectIdentifier", i);
            resultActions.andExpect(jsonPath(subjectIdentifierPath, isIn(identifiers)));
        }
    }

    private void assertSubjectsAttributes(final ResultActions resultActions, final int numOfSubjects,
            final int numOfAttrs, final String[] attrNames, final String[] attrValues, final String[] attrIssuers)
                    throws Exception {
        resultActions.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(numOfSubjects)));

        for (int i = 0; i < numOfSubjects; i++) {
            for (int j = 0; j < numOfAttrs; j++) {
                String attrValuePath = String.format("$[%s].attributes[%s].value", i, j);
                String attrNamePath = String.format("$[%s].attributes[%s].name", i, j);
                String attrIssuerPath = String.format("$[%s].attributes[%s].issuer", i, j);

                resultActions.andExpect(jsonPath(attrValuePath, isIn(attrValues)))
                        .andExpect(jsonPath(attrNamePath, isIn(attrNames)))
                        .andExpect(jsonPath(attrIssuerPath, isIn(attrIssuers)));
            }
        }
    }

}
// @formatter:on
