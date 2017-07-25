package com.ge.predix.acs.privilege.management;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.security.AbstractHttpMethodsFilter;

@Component
public class SubjectHttpMethodsFilter extends AbstractHttpMethodsFilter {

    private static final String UPDATE_SUBJECT_URI_REGEX = "\\A/v1/subject/[^/]+?/??\\Z";
    private static final String CREATE_GET_SUBJECT_URI_REGEX = "\\A/v1/subject/??\\Z";

    private static Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods() {
        Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods = new LinkedHashMap<>();
        uriPatternsAndAllowedHttpMethods.put(UPDATE_SUBJECT_URI_REGEX,
                new HashSet<>(Arrays.asList(HttpMethod.PUT, HttpMethod.GET, HttpMethod.DELETE, HttpMethod.HEAD)));
        uriPatternsAndAllowedHttpMethods.put(CREATE_GET_SUBJECT_URI_REGEX,
                new HashSet<>(Arrays.asList(HttpMethod.POST, HttpMethod.GET, HttpMethod.HEAD)));
        return uriPatternsAndAllowedHttpMethods;
    }

    public SubjectHttpMethodsFilter() {
        super(uriPatternsAndAllowedHttpMethods());
    }
}
