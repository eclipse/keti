package com.ge.predix.acs.service.policy.matcher;

import java.util.List;
import java.util.Set;

import com.ge.predix.acs.service.policy.evaluation.MatchedPolicy;

public class MatchResult {

    private final List<MatchedPolicy> matchedPolicies;
    private final Set<String> resolvedResourceUris;

    public MatchResult(final List<MatchedPolicy> matchedPolicies, final Set<String> resolvedResourceUris) {
        this.matchedPolicies = matchedPolicies;
        this.resolvedResourceUris = resolvedResourceUris;
    }

    public List<MatchedPolicy> getMatchedPolicies() {
        return this.matchedPolicies;
    }

    public Set<String> getResolvedResourceUris() {
        return this.resolvedResourceUris;
    }
}
