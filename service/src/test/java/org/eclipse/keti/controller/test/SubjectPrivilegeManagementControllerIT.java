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

// @formatter:off
package org.eclipse.keti.controller.test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.keti.acs.privilege.management.PrivilegeManagementUtility;
import org.eclipse.keti.acs.request.context.AcsRequestContext;
import org.eclipse.keti.acs.request.context.AcsRequestContextHolder;
import org.eclipse.keti.acs.rest.BaseSubject;
import org.eclipse.keti.acs.rest.Zone;
import org.eclipse.keti.acs.testutils.MockAcsRequestContext;
import org.eclipse.keti.acs.testutils.MockMvcContext;
import org.eclipse.keti.acs.testutils.MockSecurityContext;
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver;
import org.eclipse.keti.acs.testutils.TestUtils;
import org.eclipse.keti.acs.utils.JsonUtils;
import org.eclipse.keti.acs.zone.management.ZoneService;

/**
 *
 * @author acs-engineers@ge.com
 */
@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
public class SubjectPrivilegeManagementControllerIT extends AbstractTestNGSpringContextTests {
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static final String SUBJECT_BASE_URL = "/v1/subject";
    static final JsonUtils JSON_UTILS = new JsonUtils();
    static final TestUtils TEST_UTILS = new TestUtils();

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private WebApplicationContext wac;

    private Zone testZone;
    private Zone testZone2;

    @Autowired
    private ConfigurableEnvironment env;

    @BeforeClass
    public void setup() {
        this.testZone = TEST_UTILS.setupTestZone("SubjectMgmtControllerIT", zoneService);
    }


    @Test
    public void subjectZoneDoesNotExistException() throws Exception {
        // NOTE: To throw a ZoneDoesNotExistException, we must ensure that the AcsRequestContext in the
        //       SpringSecurityZoneResolver class returns a null ZoneEntity
        MockSecurityContext.mockSecurityContext(null);
        MockAcsRequestContext.mockAcsRequestContext();

        BaseSubject subject = JSON_UTILS.deserializeFromFile("controller-test/a-subject.json", BaseSubject.class);
        MockMvcContext putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac,
                "zoneDoesNotExist", SUBJECT_BASE_URL + '/' + subject.getSubjectIdentifier());
        ResultActions resultActions = putContext.getMockMvc().perform(putContext.getBuilder()
                .contentType(MediaType.APPLICATION_JSON).content(OBJECT_MAPPER.writeValueAsString(subject)));
        resultActions.andExpect(status().isBadRequest());
        resultActions.andReturn().getResponse().getContentAsString().contentEquals("Zone not found");
        MockSecurityContext.mockSecurityContext(this.testZone);
        MockAcsRequestContext.mockAcsRequestContext();
    }

    @Test
    public void testSameSubjectDifferentZones() throws Exception {
        BaseSubject subject = JSON_UTILS.deserializeFromFile("controller-test/a-subject.json", BaseSubject.class);
        String thisUri = SUBJECT_BASE_URL + "/dave";
        // create subject in first zone
        MockMvcContext putContext =
            TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))).andExpect(status().isCreated());

        // create subject in second zone
        this.testZone2 = TEST_UTILS.setupTestZone("SubjectMgmtControllerIT2", zoneService);

        putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone2.getSubdomain(),
                                                                          thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))).andExpect(status().isCreated());
        // we expect both subjects to be create in each zone
        // set security context back to first test zone
        MockSecurityContext.mockSecurityContext(this.testZone);
    }

    @Test
    public void testSubjectInvalidMediaTypeResponseStatusCheck() throws Exception {

        MockMvcContext postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.TEXT_PLAIN)
                .content("testString")).andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSubjects() throws Exception {
        List<Subject> subjects = JSON_UTILS.deserializeFromFile("controller-test/subjects-collection.json",
                                                                     List.class);
        Assert.assertNotNull(subjects);

        // Append a list of subjects
        MockMvcContext postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subjects))).andExpect(status().isNoContent());

        // Get the list of subjects
        MockMvcContext getContext =
            TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(), SUBJECT_BASE_URL);

        ResultActions resultActions = getContext.getMockMvc().perform(getContext.getBuilder());
        assertSubjects(resultActions, 2, new String[] { "dave", "vineet" });
        assertSubjectsAttributes(resultActions, 2, 2, new String[] { "group", "department" },
                new String[] { "sales", "admin" }, new String[] { "https://acs.attributes.int" });

        BaseSubject subject = JSON_UTILS.deserializeFromFile("controller-test/a-subject.json", BaseSubject.class);
        Assert.assertNotNull(subject);

        // Update a given subject

        String thisUri = SUBJECT_BASE_URL + "/dave";

        MockMvcContext putContext =
            TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))).andExpect(status().isNoContent());

        // Get a given subject
        getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(),
                                                                          thisUri);
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isOk())
                .andExpect(jsonPath("subjectIdentifier", is("dave")))
                .andExpect(jsonPath("attributes[0].value", isIn(new String[] { "supervisor", "it" })))
                .andExpect(jsonPath("attributes[0].issuer", is("https://acs.attributes.int")));

        // Delete resources from created collection
        thisUri = SUBJECT_BASE_URL + "/vineet";
        MockMvcContext deleteContext =
            TEST_UTILS.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone.getSubdomain(), thisUri);
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());
        // Make sure subject does not exist anymore
        getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(),
                                                                          thisUri);
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isNotFound());
        thisUri = SUBJECT_BASE_URL + "/dave";
        deleteContext = TEST_UTILS.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone.getSubdomain(),
                                                                                thisUri);
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());

        // Make sure subject does not exist anymore
        getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(),
                                                                          thisUri);
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isNotFound());
    }

    /*
     * This test posts a collection of subjects where one is missing an subject identifier
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testPOSTSubjectsMissingSubjectIdentifier() throws Exception {

        List<Subject> subjects = JSON_UTILS
                .deserializeFromFile("controller-test/missing-subjectidentifier-collection.json", List.class);

        Assert.assertNotNull(subjects);

        // Append a list of subjects
        MockMvcContext postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        postContext.getMockMvc()
                .perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(subjects)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPOSTSubjectsEmptyIdentifier() throws Exception {

        List<Subject> subjects = JSON_UTILS
                .deserializeFromFile("controller-test/empty-identifier-subjects-collection.json", List.class);
        Assert.assertNotNull(subjects);

        // Append a list of subjects
        MockMvcContext postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        postContext.getMockMvc()
                .perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(subjects)))
                .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPUTSubjectIdentifierMismatch() throws Exception {
        BaseSubject subject = JSON_UTILS.deserializeFromFile("controller-test/a-mismatched-subjectidentifier.json",
                                                                  BaseSubject.class);
        Assert.assertNotNull(subject);

        // Update a given resource
        String thisUri = SUBJECT_BASE_URL + "/dave";
        MockMvcContext putContext =
            TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc()
                .perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(subject)))
                .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testTypeMismatchForQueryParameter() throws Exception {

        // GET a given resource
        String thisUri = SUBJECT_BASE_URL + "/dave?includeInheritedAttributes=true)";
        MockMvcContext getContext =
            TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(), thisUri);
        getContext.getMockMvc()
                .perform(getContext.getBuilder().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(PrivilegeManagementUtility.INCORRECT_PARAMETER_TYPE_ERROR, is(HttpStatus
                .BAD_REQUEST.getReasonPhrase())))
                .andExpect(jsonPath(PrivilegeManagementUtility.INCORRECT_PARAMETER_TYPE_MESSAGE,
                is("Request Parameter " + PrivilegeManagementUtility.INHERITED_ATTRIBUTES_REQUEST_PARAMETER
                                    + " must be a boolean value")));

    }

    /*
     * This tests putting a single subject that is does not have a subject identifier
     */
    @Test
    public void testPUTSubjectNoSubjectIdentifier() throws Exception {

        BaseSubject subject = JSON_UTILS.deserializeFromFile("controller-test/no-subjectidentifier-subject.json",
                                                                  BaseSubject.class);
        Assert.assertNotNull(subject);

        // Update a given resource
        String thisUri = SUBJECT_BASE_URL + "/fermin";
        MockMvcContext putContext =
            TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))).andExpect(status().is2xxSuccessful());

        // Get a given resource
        MockMvcContext getContext =
            TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(), thisUri);
        ResultActions resultActions = getContext.getMockMvc().perform(getContext.getBuilder());

        resultActions.andExpect(status().isOk()).andExpect(jsonPath("subjectIdentifier", is("fermin")))
                .andExpect(jsonPath("attributes[0].value", isIn(new String[] { "sales", "admin" })))
                .andExpect(jsonPath("attributes[0].issuer", is("https://acs.attributes.int")));

        // Delete a given resource
        MockMvcContext deleteContext =
            TEST_UTILS.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone.getSubdomain(), thisUri);
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());

        // Make sure subject does not exist anymore
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isNotFound());
    }

    @Test
    public void testPUTCreateSubjectThenUpdateNoSubjectIdentifier() throws Exception {

        BaseSubject subject = JSON_UTILS.deserializeFromFile("controller-test/with-subjectidentifier-subject.json",
                                                                  BaseSubject.class);
        Assert.assertNotNull(subject);

        String subjectIdentifier = "fermin";
        String thisUri = SUBJECT_BASE_URL + "/" + subjectIdentifier;

        MockMvcContext putContext =
            TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))).andExpect(status().isCreated());

        MockMvcContext getContext =
            TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(), thisUri);
        ResultActions resultActions = getContext.getMockMvc().perform(getContext.getBuilder());

        resultActions.andExpect(status().isOk()).andExpect(jsonPath("subjectIdentifier", is(subjectIdentifier)))
                .andExpect(jsonPath("attributes[0].value", isIn(new String[] { "admin", "sales" })))
                .andExpect(jsonPath("attributes[0].issuer", is("https://acs.attributes.int")));

        BaseSubject subjectNoSubjectIdentifier = JSON_UTILS
                .deserializeFromFile("controller-test/no-subjectidentifier-subject.json", BaseSubject.class);
        Assert.assertNotNull(subjectNoSubjectIdentifier);

        putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone.getSubdomain(),
                                                                          thisUri);
        putContext.getMockMvc()
                .perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(subjectNoSubjectIdentifier)))
                .andExpect(status().isNoContent());

        getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(),
                                                                          thisUri);
        resultActions = getContext.getMockMvc().perform(getContext.getBuilder());

        resultActions.andExpect(status().isOk()).andExpect(jsonPath("subjectIdentifier",
                is(subjectIdentifier)))
                .andExpect(jsonPath("attributes[0].value", isIn(new String[] { "admin", "sales" })))
                .andExpect(jsonPath("attributes[0].issuer", is("https://acs.attributes.int")));

        // Delete a given resource
        MockMvcContext deleteContext =
            TEST_UTILS.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone.getSubdomain(), thisUri);
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());

    }

    @Test
    public void testZoneDoesNotExist() throws Exception {
        Zone testZone3 = new Zone("name", "subdomain", "description");
        MockSecurityContext.mockSecurityContext(testZone3);

        Map<AcsRequestContext.ACSRequestContextAttribute, Object> newMap = new HashMap<>();
        newMap.put(AcsRequestContext.ACSRequestContextAttribute.ZONE_ENTITY, null);

        ReflectionTestUtils.setField(AcsRequestContextHolder.getAcsRequestContext(),
                "unModifiableRequestContextMap", newMap);

        MockMvcContext getContext =
                TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, testZone3.getSubdomain(),
                        SUBJECT_BASE_URL + "/test-subject");
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(PrivilegeManagementUtility.INCORRECT_PARAMETER_TYPE_ERROR, is("Bad Request")))
                .andExpect(jsonPath(PrivilegeManagementUtility.INCORRECT_PARAMETER_TYPE_MESSAGE,
                        is("Zone not found")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPOSTCreateSubjectThenUpdateAttributes() throws Exception {
        // appending two subjects, the key one for this test is dave.
        List<BaseSubject> subjects = JSON_UTILS.deserializeFromFile("controller-test/subjects-collection.json",
                                                                         List.class);
        Assert.assertNotNull(subjects);

        // Append a list of subjects
        MockMvcContext postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subjects))).andExpect(status().isNoContent());

        MockMvcContext getContext =
            TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        ResultActions resultActions = getContext.getMockMvc().perform(getContext.getBuilder());

        assertSubjects(resultActions, 2, new String[] { "dave", "vineet" });
        assertSubjectsAttributes(resultActions, 2, 2, new String[] { "group", "department" },
                new String[] { "admin", "sales" }, new String[] { "https://acs.attributes.int" });

        subjects = JSON_UTILS
                .deserializeFromFile("controller-test/subjects-collection-with-different-attributes.json", List.class);
        Assert.assertNotNull(subjects);

        postContext = TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone.getSubdomain(),
                                                                            SUBJECT_BASE_URL);
        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subjects))).andExpect(status().isNoContent());

        getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(),
                                                                          SUBJECT_BASE_URL);
        resultActions = getContext.getMockMvc().perform(getContext.getBuilder());
        assertSubjects(resultActions, 2, new String[] { "dave", "vineet" });
        assertSubjectsAttributes(resultActions, 2, 2, new String[] { "group", "department" },
                new String[] { "different", "sales" }, new String[] { "https://acs.attributes.int" });

        // delete dave
        MockMvcContext deleteContext =
            TEST_UTILS.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone.getSubdomain(),
                                                               SUBJECT_BASE_URL + "/dave");
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());

        // delete vineet
        deleteContext = TEST_UTILS.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone.getSubdomain(),
                SUBJECT_BASE_URL + "/vineet");
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPOSTCreateSubjectNoDuplicates() throws Exception {
        // appending two subjects, the key one for this test is dave.

        List<BaseSubject> subjects = JSON_UTILS
                .deserializeFromFile("controller-test/a-single-subject-collection.json", List.class);
        Assert.assertNotNull(subjects);

        // Append a list of subjects
        MockMvcContext postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subjects))).andExpect(status().isNoContent());

        MockMvcContext getContext =
            TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(), SUBJECT_BASE_URL);
        ResultActions resultActions = getContext.getMockMvc().perform(getContext.getBuilder());
        assertSubjects(resultActions, 1, new String[] { "dave" });

        MockMvcContext deleteContext =
            TEST_UTILS.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone.getSubdomain(),
                                                               SUBJECT_BASE_URL + "/dave");
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
