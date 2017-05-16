/*******************************************************************************
 * Copyright 2016 General Electric Company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.ge.predix.acs.policy.evaluation.cache;

import static com.ge.predix.acs.testutils.XFiles.AGENT_MULDER;
import static com.ge.predix.acs.testutils.XFiles.XFILES_ID;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;
import static org.testng.Assert.assertEquals;

import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;

public class InMemoryPolicyEvaluationCacheTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ZONE_NAME = "testzone1";
    public static final String ACTION_GET = "GET";
    private final InMemoryPolicyEvaluationCache cache = new InMemoryPolicyEvaluationCache();

    @AfterMethod
    public void cleanupTest() {
        this.cache.reset();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetPolicyEvalResult() throws Exception {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = new PolicyEvaluationResult(Effect.PERMIT);
        String value = OBJECT_MAPPER.writeValueAsString(result);
        this.cache.set(key.toDecisionKey(), value);

        Map<String, String> evalCache = (Map<String, String>) getInternalState(this.cache, "evalCache");
        assertEquals(evalCache.size(), 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetPolicySetChangedTimestamp() throws Exception {
        String key = AbstractPolicyEvaluationCache.policySetKey(ZONE_NAME, "testSetPolicyPolicySetChangedTimestamp");
        String value = OBJECT_MAPPER.writeValueAsString(new DateTime());
        this.cache.set(key, value);

        Map<String, String> evalCache = (Map<String, String>) getInternalState(this.cache, "evalCache");
        assertEquals(evalCache.size(), 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetPolicyResourceChangedTimestamp() throws Exception {
        String key = AbstractPolicyEvaluationCache.resourceKey(ZONE_NAME, XFILES_ID);
        String value = OBJECT_MAPPER.writeValueAsString(new DateTime());
        this.cache.set(key, value);

        Map<String, String> evalCache = (Map<String, String>) getInternalState(this.cache, "evalCache");
        assertEquals(evalCache.size(), 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetPolicySubjectChangedTimestamp() throws Exception {
        String key = AbstractPolicyEvaluationCache.subjectKey(ZONE_NAME, AGENT_MULDER);
        String value = OBJECT_MAPPER.writeValueAsString(new DateTime());
        this.cache.set(key, value);

        Map<String, String> evalCache = (Map<String, String>) getInternalState(this.cache, "evalCache");
        assertEquals(evalCache.size(), 1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSetUnsupportedKeyFormat() {
        this.cache.set("key", "");
    }
}
