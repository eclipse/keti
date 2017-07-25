package com.ge.predix.acs.service.policy.evaluation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.security.AbstractHttpMethodsFilter;

@Component
public class EvaluationHttpMethodsFilter extends AbstractHttpMethodsFilter {

    private static final String EVALUATION_URI_REGEX = "\\A/v1/policy-evaluation/??\\Z";

    private static Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods() {
        Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods = new LinkedHashMap<>();
        uriPatternsAndAllowedHttpMethods.put(EVALUATION_URI_REGEX, Collections.singleton(HttpMethod.POST));
        return uriPatternsAndAllowedHttpMethods;
    }

    public EvaluationHttpMethodsFilter() {
        super(uriPatternsAndAllowedHttpMethods());
    }
}
