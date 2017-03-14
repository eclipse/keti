package com.ge.predix.controller.test;

import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.RESOURCE_CONNECTOR_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.V1;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.ZONE_URL;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.ge.predix.acs.rest.AttributeConnector;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.testutils.MockAcsRequestContext;
import com.ge.predix.acs.testutils.MockSecurityContext;
import com.ge.predix.acs.testutils.TestActiveProfilesResolver;
import com.ge.predix.acs.utils.JsonUtils;

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
public class AttributeConnectorControllerIT extends AbstractTestNGSpringContextTests {
    private static final String V1_ZONE_URL = V1 + ZONE_URL;
    private static final String V1_RESOURCE_CONNECTOR_URL = V1 + RESOURCE_CONNECTOR_URL;

    @Autowired
    private WebApplicationContext wac;

    private final JsonUtils jsonUtils = new JsonUtils();
    private final ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

    private MockMvc mockMvc;

    @BeforeClass
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void testCreateAndGetAndDeleteResourceConnector() throws Exception {
        createZone1AndAssert();

        AttributeConnector resourceConfig = this.jsonUtils
                .deserializeFromFile("controller-test/createResourceConnector.json", AttributeConnector.class);
        Assert.assertNotNull(resourceConfig, "createResourceConnector.json file not found or invalid");
        String resourceConfigContent = this.objectWriter.writeValueAsString(resourceConfig);
        this.mockMvc.perform(put(V1_RESOURCE_CONNECTOR_URL).contentType(MediaType.APPLICATION_JSON)
                .content(resourceConfigContent)).andExpect(status().isCreated());

        this.mockMvc.perform(get(V1_RESOURCE_CONNECTOR_URL))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                // .andExpect(jsonPath("isActive", is("true")))
                .andExpect(jsonPath("maxCachedIntervalMinutes", is(60)))
                .andExpect(jsonPath("adapters[0].adapterEndpoint", is("https://my-adapter.com")))
                .andExpect(jsonPath("adapters[0].uaaTokenUrl", is("https://my-uaa.com")))
                .andExpect(jsonPath("adapters[0].uaaClientId", is("adapter-client")))
                // .andExpect(jsonPath("adapters[0].uaaClientSecret", is("adapter-secret")))
                .andExpect(status().isOk());

        this.mockMvc.perform(delete(V1_RESOURCE_CONNECTOR_URL)).andExpect(status().isNoContent());
    }

    @Test
    public void testGetResourceConnectorWhichDoesNotExists() throws Exception {
        createZone1AndAssert();
        this.mockMvc.perform(get(V1_RESOURCE_CONNECTOR_URL)).andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteResourceConnectorWhichDoesNotExist() throws Exception {
        createZone1AndAssert();
        this.mockMvc.perform(delete(V1_RESOURCE_CONNECTOR_URL)).andExpect(status().isNotFound());
    }

    @Test
    public void testCreateResourceConnectorWithEmptyAdapters() throws Exception {
        createZone1AndAssert();
        AttributeConnector connector = this.jsonUtils.deserializeFromFile(
                "controller-test/createResourceConnectorWithEmptyAdapters.json", AttributeConnector.class);
        Assert.assertNotNull(connector, "createResourceConnectorWithEmptyAdapters.json file not found or invalid");
        String connectorContent = this.objectWriter.writeValueAsString(connector);

        this.mockMvc.perform(put(V1_RESOURCE_CONNECTOR_URL).contentType(MediaType.APPLICATION_JSON)
                .content(connectorContent)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testCreateResourceConnectorWithCachedIntervalBelowThreshold() throws Exception {
        createZone1AndAssert();
        AttributeConnector connector = this.jsonUtils.deserializeFromFile(
                "controller-test/createResourceConnectorWithLowValueForCache.json", AttributeConnector.class);
        System.out.println(connector);
        Assert.assertNotNull(connector,
                "createResourceConnectorWithLowValueForCache.json file not found or invalid");
        String connectorContent = this.objectWriter.writeValueAsString(connector);

        this.mockMvc.perform(put(V1_RESOURCE_CONNECTOR_URL).contentType(MediaType.APPLICATION_JSON)
                .content(connectorContent)).andExpect(status().isUnprocessableEntity());
    }

    private void createZone1AndAssert() throws JsonProcessingException, Exception {
        Zone zone = this.jsonUtils.deserializeFromFile("controller-test/createZone.json", Zone.class);
        Assert.assertNotNull(zone, "createZone.json file not found or invalid");
        String zoneContent = this.objectWriter.writeValueAsString(zone);
        this.mockMvc
                .perform(put(V1_ZONE_URL, zone.getName()).contentType(MediaType.APPLICATION_JSON).content(zoneContent))
                .andExpect(status().isCreated());

        MockSecurityContext.mockSecurityContext(zone);
        MockAcsRequestContext.mockAcsRequestContext(zone);
    }
}
