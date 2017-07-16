package com.ge.predix.acs.service.policy.admin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.security.AbstractHttpMethodsFilter;

@Component
public class PolicyHttpMethodsFilter extends AbstractHttpMethodsFilter {

    private static final String UPDATE_POLICY_URI_REGEX = "\\A/v1/policy-set/[^/]+?/??\\Z";
    private static final String GET_POLICY_URI_REGEX = "\\A/v1/policy-set/??\\Z";

    private static Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods() {
        Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods = new LinkedHashMap<>();
        uriPatternsAndAllowedHttpMethods.put(UPDATE_POLICY_URI_REGEX,
                new HashSet<>(Arrays.asList(HttpMethod.PUT, HttpMethod.GET, HttpMethod.DELETE, HttpMethod.HEAD)));
        uriPatternsAndAllowedHttpMethods.put(GET_POLICY_URI_REGEX,
                new HashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.HEAD)));
        return uriPatternsAndAllowedHttpMethods;
    }

    public PolicyHttpMethodsFilter() {
        super(uriPatternsAndAllowedHttpMethods());
    }
}
