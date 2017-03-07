package com.ge.predix.acs.audit;

import static org.mockito.Matchers.anyObject;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ge.predix.audit.AuditEvent;
import com.ge.predix.audit.sdk.AuditClient;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import com.ge.predix.eventhub.EventHubClientException;

public class PredixEventProcessorTest {

    @InjectMocks
    private PredixEventProcessor eventProcessor;

    @Mock
    private AuditClient mockedClient;

    @Mock
    private PredixEventMapper mockedMapper;

    @BeforeMethod
    public void setup() throws AuditException, EventHubClientException {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPredixEventProcess() throws AuditException, EventHubClientException {
        AuditEvent mockAuditEvent = Mockito.mock(AuditEvent.class);
        Mockito.doReturn("http://acs.com/v1/policy-evaluation/test123").when(mockAuditEvent).getRequestUri();
        AuditEventV2 mockPredixEvent = Mockito.mock(AuditEventV2.class);
        Mockito.when(this.mockedMapper.map(anyObject())).thenReturn(mockPredixEvent);

        Assert.assertTrue(this.eventProcessor.process(mockAuditEvent));
        Mockito.verify(this.mockedClient).audit(mockPredixEvent);
    }

    @Test
    public void testControllerExclusions() {
        AuditEvent auditEvent = Mockito.mock(AuditEvent.class);
        Mockito.doReturn("http://acs.com/v1/connector/test123").when(auditEvent).getRequestUri();
        Assert.assertTrue(this.eventProcessor.isExclusion(auditEvent));
    }

    @Test
    public void testIncludedApis() {
        AuditEvent mockAuditEvent = Mockito.mock(AuditEvent.class);
        Mockito.doReturn("http://acs.com/v1/policy-evaluation/test123").when(mockAuditEvent).getRequestUri();
        Assert.assertFalse(this.eventProcessor.isExclusion(mockAuditEvent));
    }

}
