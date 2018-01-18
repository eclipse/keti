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

package org.eclipse.keti.controller.test;

import org.eclipse.keti.acs.rest.BaseSubject;
import org.eclipse.keti.acs.rest.Zone;
import org.eclipse.keti.acs.testutils.MockAcsRequestContext;
import org.eclipse.keti.acs.testutils.MockMvcContext;
import org.eclipse.keti.acs.testutils.MockSecurityContext;
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver;
import org.eclipse.keti.acs.zone.management.ZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import static org.eclipse.keti.acs.testutils.XFiles.AGENT_MULDER;
import static org.eclipse.keti.acs.testutils.XFiles.FBI;
import static org.eclipse.keti.acs.testutils.XFiles.SITE_BASEMENT;
import static org.eclipse.keti.acs.testutils.XFiles.SITE_QUANTICO;
import static org.eclipse.keti.acs.testutils.XFiles.SPECIAL_AGENTS_GROUP;
import static org.eclipse.keti.acs.testutils.XFiles.SPECIAL_AGENTS_GROUP_ATTRIBUTE;
import static org.eclipse.keti.acs.testutils.XFiles.TOP_SECRET_CLASSIFICATION;
import static org.eclipse.keti.acs.testutils.XFiles.TOP_SECRET_GROUP;
import static org.eclipse.keti.acs.testutils.XFiles.createSubjectHierarchy;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
public class HierarchicalSubjectsIT extends AbstractTestNGSpringContextTests {
    private static final Zone TEST_ZONE =
        SubjectPrivilegeManagementControllerIT.TEST_UTILS.createTestZone("SubjectMgmtControllerIT");

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ConfigurableEnvironment configurableEnvironment;

    @BeforeClass
    public void beforeClass() throws Exception {
        if (!Arrays.asList(this.configurableEnvironment.getActiveProfiles()).contains("titan")) {
            throw new SkipException("This test only applies when using the \"titan\" profile");
        }

        this.zoneService.upsertZone(TEST_ZONE);
        MockSecurityContext.mockSecurityContext(TEST_ZONE);
        MockAcsRequestContext.mockAcsRequestContext();
    }

    @Test
    public void testPostAndGetHierarchicalSubjects() throws Exception {
        List<BaseSubject> subjects = createSubjectHierarchy();

        // Append a list of resources
        MockMvcContext postContext =
            SubjectPrivilegeManagementControllerIT.TEST_UTILS.createWACWithCustomPOSTRequestBuilder(
                this.wac,
                TEST_ZONE.getSubdomain(),
                SubjectPrivilegeManagementControllerIT.SUBJECT_BASE_URL);

        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                                                    .content(SubjectPrivilegeManagementControllerIT.OBJECT_MAPPER
                                                                 .writeValueAsString(subjects)))
                   .andExpect(status().isNoContent());

        // Get the child subject without query string specifier.
        MockMvcContext getContext0 =
            SubjectPrivilegeManagementControllerIT.TEST_UTILS.createWACWithCustomGETRequestBuilder(
                this.wac,
                TEST_ZONE.getSubdomain(),
                SubjectPrivilegeManagementControllerIT.SUBJECT_BASE_URL + "/"
                + URLEncoder.encode(AGENT_MULDER, "UTF-8"));

        getContext0.getMockMvc().perform(getContext0.getBuilder()).andExpect(status().isOk())
                   .andExpect(jsonPath("subjectIdentifier", is(AGENT_MULDER)))
                   .andExpect(jsonPath("attributes[0].name", is(SITE_BASEMENT.getName())))
                   .andExpect(jsonPath("attributes[0].value", is(SITE_BASEMENT.getValue())))
                   .andExpect(jsonPath("attributes[0].issuer", is(SITE_BASEMENT.getIssuer())))
                   .andExpect(jsonPath("attributes[1]").doesNotExist());

        // Get the child subject without inherited attributes.
        MockMvcContext getContext1 =
            SubjectPrivilegeManagementControllerIT.TEST_UTILS.createWACWithCustomGETRequestBuilder(
                this.wac,
                TEST_ZONE.getSubdomain(),
                SubjectPrivilegeManagementControllerIT.SUBJECT_BASE_URL + "/"
                + URLEncoder.encode(AGENT_MULDER, "UTF-8") + "?includeInheritedAttributes=false");

        getContext1.getMockMvc().perform(getContext1.getBuilder()).andExpect(status().isOk())
                   .andExpect(jsonPath("subjectIdentifier", is(AGENT_MULDER)))
                   .andExpect(jsonPath("attributes[0].name", is(SITE_BASEMENT.getName())))
                   .andExpect(jsonPath("attributes[0].value", is(SITE_BASEMENT.getValue())))
                   .andExpect(jsonPath("attributes[0].issuer", is(SITE_BASEMENT.getIssuer())))
                   .andExpect(jsonPath("attributes[1]").doesNotExist());

        // Get the child subject with inherited attributes.
        MockMvcContext getContext2 =
            SubjectPrivilegeManagementControllerIT.TEST_UTILS.createWACWithCustomGETRequestBuilder(
                this.wac,
                TEST_ZONE.getSubdomain(),
                SubjectPrivilegeManagementControllerIT.SUBJECT_BASE_URL + "/"
                + URLEncoder.encode(AGENT_MULDER, "UTF-8") + "?includeInheritedAttributes=true");

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

        // Clean up after ourselves.
        deleteSubject(AGENT_MULDER);
        deleteSubject(TOP_SECRET_GROUP);
        deleteSubject(SPECIAL_AGENTS_GROUP);
        deleteSubject(FBI);
    }

    private void deleteSubject(final String subjectIdentifier) throws Exception {
        String subjectToDeleteURI = SubjectPrivilegeManagementControllerIT.SUBJECT_BASE_URL + "/"
                                    + URLEncoder.encode(subjectIdentifier, "UTF-8");

        MockMvcContext deleteContext =
            SubjectPrivilegeManagementControllerIT.TEST_UTILS.createWACWithCustomDELETERequestBuilder(
                this.wac,
                TEST_ZONE.getSubdomain(), subjectToDeleteURI);

        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());
    }
}
