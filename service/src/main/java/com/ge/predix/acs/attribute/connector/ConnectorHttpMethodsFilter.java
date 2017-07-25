package com.ge.predix.acs.attribute.connector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.security.AbstractHttpMethodsFilter;

@Component
public class ConnectorHttpMethodsFilter extends AbstractHttpMethodsFilter {

    private static final String CHANGE_GET_RESOURCE_CONNECTOR_URI_REGEX = "\\A/v1/connector/resource/??\\Z";
    private static final String CHANGE_GET_SUBJECT_CONNECTOR_URI_REGEX = "\\A/v1/connector/subject/??\\Z";

    private static Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods() {
        Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods = new LinkedHashMap<>();
        uriPatternsAndAllowedHttpMethods.put(CHANGE_GET_RESOURCE_CONNECTOR_URI_REGEX,
                new HashSet<>(Arrays.asList(HttpMethod.PUT, HttpMethod.GET, HttpMethod.DELETE, HttpMethod.HEAD)));
        uriPatternsAndAllowedHttpMethods.put(CHANGE_GET_SUBJECT_CONNECTOR_URI_REGEX,
                new HashSet<>(Arrays.asList(HttpMethod.PUT, HttpMethod.GET, HttpMethod.DELETE, HttpMethod.HEAD)));
        return uriPatternsAndAllowedHttpMethods;
    }

    public ConnectorHttpMethodsFilter() {
        super(uriPatternsAndAllowedHttpMethods());
    }
}
