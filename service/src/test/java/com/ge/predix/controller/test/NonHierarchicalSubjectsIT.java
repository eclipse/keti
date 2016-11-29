package com.ge.predix.controller.test;

import com.ge.predix.acs.commons.web.BaseRestApi;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.testutils.MockAcsRequestContext;
import com.ge.predix.acs.testutils.MockMvcContext;
import com.ge.predix.acs.testutils.MockSecurityContext;
import com.ge.predix.acs.testutils.TestActiveProfilesResolver;
import com.ge.predix.acs.zone.management.ZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
public final class NonHierarchicalSubjectsIT extends AbstractTestNGSpringContextTests {
    private static Zone TEST_ZONE;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ConfigurableEnvironment configurableEnvironment;

    @BeforeClass
    public void beforeClass() throws Exception {
        if (Arrays.asList(this.configurableEnvironment.getActiveProfiles()).contains("titan")) {
            throw new SkipException("This test only applies when NOT using the \"titan\" profile");
        }

        TEST_ZONE =
            SubjectPrivilegeManagementControllerIT.TEST_UTILS.createTestZone("SubjectMgmtControllerIT");
        this.zoneService.upsertZone(TEST_ZONE);
        MockSecurityContext.mockSecurityContext(TEST_ZONE);
        MockAcsRequestContext.mockAcsRequestContext(TEST_ZONE);
    }

    @Test
    public void testSubjectWithParentsFailWhenNotUsingTitan() throws Exception {
        BaseSubject subject = SubjectPrivilegeManagementControllerIT.JSON_UTILS.deserializeFromFile(
            "controller-test/a-subject-with-parents.json", BaseSubject.class);
        Assert.assertNotNull(subject);

        MockMvcContext putContext =
            SubjectPrivilegeManagementControllerIT.TEST_UTILS.createWACWithCustomPUTRequestBuilder(
                this.wac, TEST_ZONE.getSubdomain(),
                SubjectPrivilegeManagementControllerIT.SUBJECT_BASE_URL + "/dave-with-parents");
        final MvcResult result = putContext.getMockMvc()
                                           .perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                                                              .content(ResourcePrivilegeManagementControllerIT
                                                                           .OBJECT_MAPPER.writeValueAsString(subject)))
                                           .andExpect(status().isNotImplemented())
                                           .andReturn();

        Assert.assertEquals(result.getResponse().getContentAsString(),
                            "{\"ErrorDetails\":{\"errorCode\":\"FAILURE\","
                            + "\"errorMessage\":\"" + BaseRestApi.PARENTS_ATTR_NOT_SUPPORTED_MSG + "\"}}");
    }
}
