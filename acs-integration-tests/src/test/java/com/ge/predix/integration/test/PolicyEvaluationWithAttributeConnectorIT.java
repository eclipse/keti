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
 *******************************************************************************/

package com.ge.predix.integration.test;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.DefaultSpanNamer;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.instrument.web.HttpTraceKeysInjector;
import org.springframework.cloud.sleuth.instrument.web.ZipkinHttpSpanInjector;
import org.springframework.cloud.sleuth.instrument.web.client.TraceRestTemplateInterceptor;
import org.springframework.cloud.sleuth.log.NoOpSpanLogger;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.cloud.sleuth.trace.DefaultTracer;
import org.springframework.cloud.sleuth.util.ArrayListSpanAccumulator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.rest.AttributeAdapterConnection;
import com.ge.predix.acs.rest.AttributeConnector;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.test.utils.ACSITSetUpFactory;
import com.ge.predix.test.utils.ACSTestUtil;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.ZoneFactory;

@Configuration
@ImportResource("classpath:integration-test-spring-context.xml")
class PolicyEvaluationWithAttributeConnectorITConfiguration {

    @Autowired
    private ACSITSetUpFactory acsitSetUpFactory;

    @Autowired
    private DefaultTracer tracer;

    private void setRestTemplateInterceptor(final RestTemplate restTemplate) {
        restTemplate.setInterceptors(Collections.singletonList(
                new TraceRestTemplateInterceptor(this.tracer, new ZipkinHttpSpanInjector(),
                        new HttpTraceKeysInjector(this.tracer, new TraceKeys()))));
    }

    @Bean
    public DefaultTracer tracer() {
        return new DefaultTracer(new AlwaysSampler(), new Random(), new DefaultSpanNamer(), new NoOpSpanLogger(),
                new ArrayListSpanAccumulator(), new TraceKeys());
    }

    @Bean
    public OAuth2RestTemplate acsAdminRestTemplate() throws IOException {
        OAuth2RestTemplate acsAdminRestTemplate;
        this.acsitSetUpFactory.setUp();
        acsAdminRestTemplate = this.acsitSetUpFactory.getAcsAdminRestTemplate2();
        setRestTemplateInterceptor(acsAdminRestTemplate);
        return acsAdminRestTemplate;
    }
    
    @Bean
    public ACSITSetUpFactory acsitSetUpFactory() {
        return this.acsitSetUpFactory;
    }
}

@ContextConfiguration(classes = { PolicyEvaluationWithAttributeConnectorITConfiguration.class })
public class PolicyEvaluationWithAttributeConnectorIT extends AbstractTestNGSpringContextTests {

    @Value("${ZONE1_NAME:testzone1}")
    private String acsZone1Name;

    @Value("${ACS_UAA_URL}")
    private String acsUaaUrl;

    @Value("${ADAPTER_ENDPOINT:${ASSET_ADAPTER_URL}}")
    private String adapterEndpoint;

    @Value("${ADAPTER_UAA_TOKEN_URL:${ASSET_TOKEN_URL}}")
    private String adapterUaaTokenUrl;

    @Value("${ADAPTER_UAA_CLIENT_ID:${ASSET_CLIENT_ID}}")
    private String adapterUaaClientId;

    @Value("${ADAPTER_UAA_CLIENT_SECRET:${ASSET_CLIENT_SECRET}}")
    private String adapterUaaClientSecret;

    @Autowired
    private ZoneFactory zoneFactory;

    @Autowired
    private PolicyHelper policyHelper;

    @Autowired
    private OAuth2RestTemplate acsAdminRestTemplate;

    @Autowired
    private ACSITSetUpFactory acsitSetUpFactory;

    @Autowired
    private DefaultTracer tracer;

    private static final String TEST_PART_ID = "part/03f95db1-4255-4265-a509-f7bca3e1fee4";

    private Zone zone;

    private URI resourceAttributeConnectorUrl;

    private HttpHeaders zoneHeader() throws IOException {
        HttpHeaders httpHeaders = ACSTestUtil.httpHeaders();
        httpHeaders.set(PolicyHelper.PREDIX_ZONE_ID, this.zone.getSubdomain());
        return httpHeaders;
    }

    private void configureAttributeConnector(final boolean isActive) throws IOException {

        List<AttributeAdapterConnection> adapters = Collections.singletonList(
                new AttributeAdapterConnection(this.adapterEndpoint, this.adapterUaaTokenUrl, this.adapterUaaClientId,
                        this.adapterUaaClientSecret));

        AttributeConnector attributeConnector = new AttributeConnector();
        attributeConnector.setIsActive(isActive);
        attributeConnector.setAdapters(new HashSet<>(adapters));
        this.acsAdminRestTemplate.exchange(this.resourceAttributeConnectorUrl, HttpMethod.PUT,
                new HttpEntity<>(attributeConnector, zoneHeader()), AttributeConnector.class);
    }

    @BeforeClass
    void beforeClass() throws IOException {

        this.zone = this.acsitSetUpFactory.getZone1();
        this.resourceAttributeConnectorUrl = URI.create(this.zoneFactory.getAcsBaseURL() + "/v1/connector/resource");
    }

    private void deconfigureAttributeConnector() throws IOException {
        this.acsAdminRestTemplate
                .exchange(this.resourceAttributeConnectorUrl, HttpMethod.DELETE, new HttpEntity<>(zoneHeader()),
                        Void.class);
    }

    @AfterClass
    void afterClass() throws IOException {
        this.acsitSetUpFactory.destroy();
    }

    @Test(dataProvider = "adapterStatusesAndResultingEffects")
    public void testPolicyEvaluationWithAdapters(final boolean adapterActive, final Effect expectedEffect,
            final boolean enableSleuthTracing) throws Exception {
        String testPolicyName = this.policyHelper
                .setTestPolicy(this.acsAdminRestTemplate, zoneHeader(), this.zoneFactory.getAcsBaseURL(),
                        "src/test/resources/policy-set-with-one-policy-using-resource-attributes-from-asset-adapter"
                                + ".json");

        try {
            this.configureAttributeConnector(adapterActive);
            PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                    .createEvalRequest("GET", "testSubject", TEST_PART_ID, null);

            if (enableSleuthTracing) {
                this.tracer.continueSpan(Span.builder().traceId(1L).spanId(2L).parent(3L).build());
            }

            ResponseEntity<PolicyEvaluationResult> policyEvaluationResponse = this.acsAdminRestTemplate
                    .postForEntity(this.zoneFactory.getAcsBaseURL() + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                            new HttpEntity<>(policyEvaluationRequest, zoneHeader()), PolicyEvaluationResult.class);
            Assert.assertEquals(policyEvaluationResponse.getStatusCode(), HttpStatus.OK);

            HttpHeaders responseHeaders = policyEvaluationResponse.getHeaders();
            Assert.assertTrue(
                    responseHeaders.containsKey(Span.TRACE_ID_NAME) && StringUtils.isNotEmpty(Span.TRACE_ID_NAME));
            if (enableSleuthTracing) {
                Assert.assertEquals(Span.hexToId(responseHeaders.get(Span.TRACE_ID_NAME).get(0)), 1L);
            }

            PolicyEvaluationResult policyEvaluationResult = policyEvaluationResponse.getBody();
            Assert.assertEquals(policyEvaluationResult.getEffect(), expectedEffect);
        } finally {
            this.policyHelper
                    .deletePolicySet(this.acsAdminRestTemplate, this.zoneFactory.getAcsBaseURL(), testPolicyName,
                            zoneHeader());
            this.deconfigureAttributeConnector();
        }
    }

    @DataProvider
    private Object[][] adapterStatusesAndResultingEffects() {
        return new Object[][] { { true, Effect.PERMIT, true }, { true, Effect.PERMIT, false },
                { false, Effect.NOT_APPLICABLE, true } };
    }
}