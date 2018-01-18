/*******************************************************************************
 * Copyright 2017 General Electric Company
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

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.eclipse.keti.acs.commons.policy.condition.ConditionScript;

public class GroovyConditionCacheTest {

    @Test
    public void testPutGetAndRemove() {
        GroovyConditionCache cache = new GroovyConditionCache();
        ConditionScript compiledScript = Mockito.mock(ConditionScript.class);
        String testScript = "1 == 1";
        cache.put(testScript, compiledScript);
        Assert.assertEquals(cache.get(testScript), compiledScript);
        cache.remove(testScript);
        Assert.assertNull(cache.get(testScript));
    }

}
