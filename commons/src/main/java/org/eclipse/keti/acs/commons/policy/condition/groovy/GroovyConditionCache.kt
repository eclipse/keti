package org.eclipse.keti.acs.commons.policy.condition.groovy;

import org.eclipse.keti.acs.commons.policy.condition.ConditionScript;

public interface GroovyConditionCache {

    ConditionScript get(String script);

    void put(String script, ConditionScript compiledScript);

    void remove(String script);
}
