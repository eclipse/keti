package com.ge.predix.integration.test;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ge.predix.audit.rest.PredixAuditRequest;
import com.ge.predix.audit.rest.PredixAuditResponse;
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.ZoneHelper;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class PredixAuditIT extends AbstractTestNGSpringContextTests {

    @Value("${AUDIT_QUERY_URL}")
    private String auditQueryUrl;

    @Value("${AUDIT_ZONE_ID}")
    private String auditZoneId;

    @Autowired
    private ZoneHelper zoneHelper;

    @Autowired
    private OAuth2RestTemplate auditRestTemplate;

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    @Test
    public void testAudit() throws Exception {
        long startTime = Instant.now().toEpochMilli();
        this.zoneHelper.createTestZone(this.acsRestTemplateFactory.getACSTemplateWithPolicyScope(), "predix-audit-zone",
                true);
        PredixAuditRequest request = new PredixAuditRequest(1, 10, startTime, Instant.now().toEpochMilli());
        HttpHeaders headers = new HttpHeaders();
        headers.add("Predix-Zone-Id", auditZoneId);
        Thread.sleep(20000);
        ResponseEntity<PredixAuditResponse> response = this.auditRestTemplate.postForEntity(auditQueryUrl,
                new HttpEntity<>(request, headers), PredixAuditResponse.class);
        
        

        Assert.assertTrue(response.getBody().getContent().size() >= 1);
    }

}
