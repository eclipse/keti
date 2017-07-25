package com.ge.predix.acs.zone.management;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.security.AbstractHttpMethodsFilter;

@Component
public class ZoneHttpMethodsFilter extends AbstractHttpMethodsFilter {

    private static final String ZONE_MANAGEMENT_URI_REGEX = "\\A/v1/zone/[^/]+?/??\\Z";

    private static Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods() {
        Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods = new LinkedHashMap<>();
        uriPatternsAndAllowedHttpMethods.put(ZONE_MANAGEMENT_URI_REGEX,
                new HashSet<>(Arrays.asList(HttpMethod.PUT, HttpMethod.GET, HttpMethod.DELETE, HttpMethod.HEAD)));
        return uriPatternsAndAllowedHttpMethods;
    }

    public ZoneHttpMethodsFilter() {
        super(uriPatternsAndAllowedHttpMethods());
    }
}
