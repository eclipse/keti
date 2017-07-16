package com.ge.predix.acs.security;

import java.net.URI;
import java.util.Collections;

import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.commons.web.ResponseEntityBuilder;

public final class AbstractHttpMethodsFilterTest {

    private static final String V1_DUMMY = "/v1/dummy";

    private static final class DummyHttpMethodsFilter extends AbstractHttpMethodsFilter {

        DummyHttpMethodsFilter() {
            super(Collections.singletonMap("\\A" + V1_DUMMY + "/??\\Z", Collections.singleton(HttpMethod.GET)));
        }
    }

    @RestController
    private static final class DummyController {

        @RequestMapping(method = RequestMethod.GET, value = V1_DUMMY)
        public ResponseEntity<String> getDummy() {
            return ResponseEntityBuilder.ok();
        }
    }

    @InjectMocks
    private DummyController dummyController;

    private MockMvc mockMvc;

    @BeforeClass
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc =
                MockMvcBuilders.standaloneSetup(this.dummyController).addFilters(new DummyHttpMethodsFilter()).build();
    }

    @Test(dataProvider = "mediaTypesAndExpectedStatuses")
    public void testUnacceptableMediaTypes(final String mediaType, final ResultMatcher resultMatcher) throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.request(HttpMethod.GET, URI.create(V1_DUMMY)).accept(mediaType))
                .andExpect(resultMatcher);
    }

    @DataProvider
    public Object[][] mediaTypesAndExpectedStatuses() {
        return new Object[][] { new Object[] { MediaType.ALL_VALUE, MockMvcResultMatchers.status().isOk() },
                { MediaType.APPLICATION_JSON_VALUE, MockMvcResultMatchers.status().isOk() },
                { MediaType.TEXT_PLAIN_VALUE, MockMvcResultMatchers.status().isOk() },
                { "fake/type", MockMvcResultMatchers.status().isNotAcceptable() } };
    }
}