package com.ge.predix.acs.audit;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditEnums.CategoryType;
import com.ge.predix.audit.sdk.message.AuditEnums.Classifier;
import com.ge.predix.audit.sdk.message.AuditEnums.EventType;
import com.ge.predix.audit.sdk.message.AuditEnums.PublisherType;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import com.ge.predix.audit.sdk.message.AuditEventV2.AuditEventV2Builder;

@Component
@Profile("predixAudit")
public class PredixEventMapper {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PredixEventMapper.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern REGEX = Pattern
            .compile("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)");

    public AuditEventV2 map(final AuditEvent auditEvent) {
        Map<String, String> auditPayload = new HashMap<>();
        auditPayload.put("httpMethod", auditEvent.getMethod());
        auditPayload.put("requestBody", auditEvent.getRequestBody());
        auditPayload.put("responseBody", auditEvent.getResponseBody());

        String payload = "";
        try {
            payload = OBJECT_MAPPER.writeValueAsString(auditPayload);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Unable to convert audit payload to json: {}" + auditPayload, e);
        }

        String correlationId = auditEvent.getCorrelationId();
        // Padding correlation ID with 0s if zipkin id is 64 bit. Audit requires a correlation ID of 128 bits, each
        // hex character is 4 bits so we must have a string of length 32 with only hex values.
        if (correlationId.length() == 16) {
            correlationId = "0000000000000000" + correlationId;
        }
        correlationId = REGEX.matcher(correlationId).replaceFirst("$1-$2-$3-$4-$5");

        AuditEventV2Builder auditEventBuilder = AuditEventV2.builder().timestamp(auditEvent.getTime().toEpochMilli())
                .correlationId(correlationId).tenantUuid(auditEvent.getZoneId())
                .publisherType(PublisherType.APP_SERVICE).categoryType(CategoryType.API_CALLS).payload(payload);

        if (isSuccessful(auditEvent)) {
            auditEventBuilder = auditEventBuilder.eventType(EventType.SUCCESS_API_REQUEST)
                    .classifier(Classifier.SUCCESS);
        } else {
            auditEventBuilder = auditEventBuilder.eventType(EventType.FAILURE_API_REQUEST)
                    .classifier(Classifier.FAILURE);
        }

        return auditEventBuilder.build();
    }

    private boolean isSuccessful(final AuditEvent auditEvent) {
        return HttpStatus.valueOf(auditEvent.getStatus()).is2xxSuccessful();
    }

}