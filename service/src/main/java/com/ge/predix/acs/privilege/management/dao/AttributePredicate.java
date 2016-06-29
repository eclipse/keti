package com.ge.predix.acs.privilege.management.dao;

import java.util.Set;
import java.util.function.BiPredicate;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.utils.JsonUtils;

public final class AttributePredicate {
    private static final JsonUtils JSON_UTILS = new JsonUtils();

    private AttributePredicate() {
        // Prevents instantiation.
    }

    static BiPredicate<String, Set<Attribute>> elementOf() {
        return new BiPredicate<String, Set<Attribute>>() {
            @Override
            public boolean test(final String t, final Set<Attribute> u) {
                Attribute element = JSON_UTILS.deserialize(t, Attribute.class);
                return u.contains(element);
            }
        };
    }
}
