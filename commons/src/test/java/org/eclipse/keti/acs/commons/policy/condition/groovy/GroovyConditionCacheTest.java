/*******************************************************************************
 * Copyright 2018 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package org.eclipse.keti.acs.commons.policy.condition.groovy;

import org.eclipse.keti.acs.commons.policy.condition.ConditionScript;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GroovyConditionCacheTest {

    @Test
    public void testPutGetAndRemove() {
        GroovyConditionCache cache = new InMemoryGroovyConditionCache();
        ConditionScript compiledScript = Mockito.mock(ConditionScript.class);
        String testScript = "1 == 1";
        cache.put(testScript, compiledScript);
        Assert.assertEquals(cache.get(testScript), compiledScript);
        cache.remove(testScript);
        Assert.assertNull(cache.get(testScript));
    }

    @Test
    public void testPutGetAndRemoveIfDisabled() {
        GroovyConditionCache cache = new NonCachingGroovyConditionCache();
        ConditionScript compiledScript = Mockito.mock(ConditionScript.class);
        String testScript = "1 == 1";
        cache.put(testScript, compiledScript);
        Assert.assertNull(cache.get(testScript));
        cache.remove(testScript);
        Assert.assertNull(cache.get(testScript));
    }
}
