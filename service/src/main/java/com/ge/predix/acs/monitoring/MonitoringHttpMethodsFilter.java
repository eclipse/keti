package com.ge.predix.acs.monitoring;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.security.AbstractHttpMethodsFilter;

@Component
public class MonitoringHttpMethodsFilter extends AbstractHttpMethodsFilter {

    private static final String HEARTBEAT_URI_REGEX = "\\A/monitoring/heartbeat/??\\Z";

    private static Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods() {
        Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods = new LinkedHashMap<>();
        uriPatternsAndAllowedHttpMethods.put(HEARTBEAT_URI_REGEX,
                new HashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.HEAD)));
        return uriPatternsAndAllowedHttpMethods;
    }

    public MonitoringHttpMethodsFilter() {
        super(uriPatternsAndAllowedHttpMethods());
    }
}
