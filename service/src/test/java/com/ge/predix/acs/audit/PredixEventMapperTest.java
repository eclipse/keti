package com.ge.predix.acs.audit;

import java.io.IOException;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.audit.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditEnums.CategoryType;
import com.ge.predix.audit.sdk.message.AuditEnums.Classifier;
import com.ge.predix.audit.sdk.message.AuditEnums.EventType;
import com.ge.predix.audit.sdk.message.AuditEnums.PublisherType;
import com.ge.predix.audit.sdk.message.AuditEventV2;

public class PredixEventMapperTest {

    // CHECKSTYLE:OFF: MagicNumber
    @Test(dataProvider = "predixMapperDataProvider")
    public void testPredixAuditEventMapper(final String requestBody, final String methodType, final int status,
            final String responseBody, final String zoneId, final String correlationId,
            final PublisherType publisherType, final CategoryType categoryType, final EventType eventType,
            final Classifier classifier) throws IOException {

        PredixEventMapper mapper = new PredixEventMapper();

        // setup mocked request and response
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(requestBody.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod(methodType);
        request.setRequestURI("not-used");
        response.setStatus(status);

        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        cachedResponse.getWriter().write(responseBody);
        cachedRequest.getReader().readLine();

        AuditEvent event = new AuditEvent(cachedRequest, cachedResponse, zoneId, correlationId);
        AuditEventV2 predixEvent = mapper.map(event);

        Assert.assertTrue(predixEvent.getPayload().contains(requestBody));
        Assert.assertTrue(predixEvent.getPayload().contains(methodType));
        Assert.assertTrue(predixEvent.getPayload().contains(responseBody));
        Assert.assertEquals(predixEvent.getPublisherType(), publisherType);
        Assert.assertEquals(predixEvent.getCategoryType(), categoryType);
        Assert.assertEquals(predixEvent.getEventType(), eventType);
        Assert.assertEquals(predixEvent.getClassifier(), classifier);
    }
    // CHECKSTYLE:ON: MagicNumber

    @DataProvider
    public Object[][] predixMapperDataProvider() {
        return new Object[][] {
                new Object[] { "request body", "POST", 401, "response body", "1234", "5678", PublisherType.APP_SERVICE,
                        CategoryType.API_CALLS, EventType.FAILURE_API_REQUEST, Classifier.FAILURE },
                { "request body", "PUT", 200, "response body", "9101112", "13141516", PublisherType.APP_SERVICE,
                        CategoryType.API_CALLS, EventType.SUCCESS_API_REQUEST, Classifier.SUCCESS } };
    }

}
