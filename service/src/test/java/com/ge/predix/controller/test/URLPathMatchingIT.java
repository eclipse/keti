package com.ge.predix.controller.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.testutils.TestActiveProfilesResolver;
import com.ge.predix.acs.utils.JsonUtils;

import org.testng.Assert;

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@Test
public class URLPathMatchingIT extends AbstractTestNGSpringContextTests {
    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;
    private final JsonUtils jsonUtils = new JsonUtils();
    private final ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
    private Zone zone;

    @BeforeClass
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test(dataProvider = "nonMatchedUrlPatternDp")
    public void test_returnNotFoundForNotMatchedURLs(final RequestBuilder request) throws Exception {
        this.mockMvc.perform(request).andExpect(status().isNotFound());
    }
    
    @DataProvider
    public Object[][] nonMatchedUrlPatternDp() throws JsonProcessingException {
    	this.zone = this.jsonUtils.deserializeFromFile("controller-test/createZone.json", Zone.class);
        Assert.assertNotNull(this.zone, "createZone.json file not found or invalid");
        String zoneContent = this.objectWriter.writeValueAsString(this.zone);

        return new Object[][] { 
        	{ put("/ /v1/zone/zone-1", "zone-1").contentType(MediaType.APPLICATION_JSON).content(zoneContent) },
        	{ put("/v1/ /zone/zone-1", "zone-1").contentType(MediaType.APPLICATION_JSON).content(zoneContent) },
        	{ get("/ /v1/zone/zone-2").contentType(MediaType.APPLICATION_JSON) },
        	{ get("/v1/ /zone/zone-2").contentType(MediaType.APPLICATION_JSON) }
        	
        };
    }
    
}
