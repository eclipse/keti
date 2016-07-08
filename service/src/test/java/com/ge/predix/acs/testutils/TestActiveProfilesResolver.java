package com.ge.predix.acs.testutils;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.context.ActiveProfilesResolver;

public class TestActiveProfilesResolver implements ActiveProfilesResolver {

    @Override
    public String[] resolve(final Class<?> arg0) {
        String envSpringProfilesActive = System.getenv("SPRING_PROFILES_ACTIVE");
        String[] profiles = new String[] { "h2", "public", "simple-cache", "titan" };
        if (StringUtils.isNotEmpty(envSpringProfilesActive)) {
            profiles = StringUtils.split(envSpringProfilesActive, ',');
        }
        System.out.println("SPRING_ACTIVE_PROFILES: " + StringUtils.join(profiles, ','));
        return profiles;
    }
}
