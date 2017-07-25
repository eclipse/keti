package com.ge.predix.integration.test;

import java.time.Instant;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ge.predix.audit.rest.PredixAuditRequest;
import com.ge.predix.audit.rest.PredixAuditResponse;
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.ACSTestUtil;
import com.ge.predix.test.utils.ZoneHelper;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class PredixAuditIT extends AbstractTestNGSpringContextTests {

    @Value("${AUDIT_QUERY_URL}")
    private String auditQueryUrl;

    @Value("${AUDIT_ZONE_ID}")
    private String auditZoneId;

    @Value("${AUDIT_UAA_URL}")
    private String auditUaaUrl;

    @Value("${AUDIT_UAA_CLIENT_ID}")
    private String auditClientId;

    @Value("${AUDIT_UAA_CLIENT_SECRET}")
    private String auditClientSecret;

    @Autowired
    private ZoneHelper zoneHelper;

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    private OAuth2RestTemplate auditRestTemplate;

    @BeforeClass(enabled = false)
    public void setup() {
        ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();

        resource.setAccessTokenUri(this.auditUaaUrl);
        resource.setClientId(this.auditClientId);
        resource.setClientSecret(this.auditClientSecret);

        this.auditRestTemplate = new OAuth2RestTemplate(resource);
        HttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        this.auditRestTemplate.setRequestFactory(requestFactory);
    }

    @Test(enabled = false)
    public void testAudit() throws Exception {
        long startTime = Instant.now().toEpochMilli();
        this.zoneHelper.createTestZone(this.acsRestTemplateFactory.getACSTemplateWithPolicyScope(), "predix-audit-zone",
                true);
        HttpHeaders headers = ACSTestUtil.httpHeaders();
        headers.add("Predix-Zone-Id", this.auditZoneId);
        Thread.sleep(5000);
        PredixAuditRequest request = new PredixAuditRequest(1, 10, startTime, Instant.now().toEpochMilli());
        ResponseEntity<PredixAuditResponse> response = this.auditRestTemplate.postForEntity(this.auditQueryUrl,
                new HttpEntity<>(request, headers), PredixAuditResponse.class);

        Assert.assertTrue(response.getBody().getContent().size() >= 1);
    }

}
