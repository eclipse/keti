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
 *******************************************************************************/

package com.ge.predix.acs.policy.evaluation.cache;

import static com.ge.predix.acs.testutils.XFiles.AGENT_MULDER;
import static com.ge.predix.acs.testutils.XFiles.XFILES_ID;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joda.time.DateTime;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.attribute.connector.management.AttributeConnectorService;
import com.ge.predix.acs.attribute.connector.management.AttributeConnectorServiceImpl;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.model.PolicySet;
import com.ge.predix.acs.privilege.management.dao.ResourceEntity;
import com.ge.predix.acs.privilege.management.dao.SubjectEntity;
import com.ge.predix.acs.rest.AttributeConnector;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public class AbstractPolicyEvaluationCacheTest {

    private static final String ZONE_NAME = "testzone1";
    private static final ZoneEntity ZONE_ENTITY = new ZoneEntity(1L, ZONE_NAME);
    private static final String ACTION_GET = "GET";
    private static final PolicySet POLICY_ONE = new PolicySet("policyOne");
    private static final PolicySet POLICY_TWO = new PolicySet("policyTwo");
    private static final LinkedHashSet<String> EVALUATION_ORDER_POLICYONE_POLICYTWO = Stream
            .of("policyOne", "policyTwo").collect(Collectors.toCollection(LinkedHashSet::new));
    private static final LinkedHashSet<String> EVALUATION_ORDER_POLICYTWO_POLICYONE = Stream
            .of("policyTwo", "policyOne").collect(Collectors.toCollection(LinkedHashSet::new));
    private static final LinkedHashSet<String> EVALUATION_ORDER_POLICYONE = Stream.of("policyOne")
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private final InMemoryPolicyEvaluationCache cache = new InMemoryPolicyEvaluationCache();

    @BeforeClass
    void beforeClass() {
        AttributeConnectorService connectorService = Mockito.mock(AttributeConnectorService.class);
        ReflectionTestUtils.setField(this.cache, "connectorService", connectorService);
    }

    @AfterMethod
    public void cleanupTest() {
        this.cache.reset();
    }

    @Test
    public void testGetWithCacheMissForPolicyEvaluation() {

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();
        assertNull(this.cache.get(key));
    }

    @Test
    public void testGetWithCacheMissForResource() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_ONE.getName());
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();
        PolicyEvaluationResult result = mockPermitResult();
        this.cache.set(key, result);
        this.cache.delete(AbstractPolicyEvaluationCache.resourceKey(ZONE_NAME, XFILES_ID));
        assertNull(this.cache.get(key));
    }

    @Test
    public void testGetWithCacheHit() {

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_ONE.getName());
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        request.setPolicySetsEvaluationOrder(EVALUATION_ORDER_POLICYONE);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = mockPermitResult();
        this.cache.set(key, result);

        PolicyEvaluationResult cachedResult = this.cache.get(key);
        assertEquals(cachedResult.getEffect(), result.getEffect());
    }

    @Test
    public void testGetWithPolicyInvalidation() throws Exception {

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_ONE.getName());
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        request.setPolicySetsEvaluationOrder(EVALUATION_ORDER_POLICYONE);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = mockPermitResult();
        this.cache.set(key, result);

        PolicyEvaluationResult cachedResult = this.cache.get(key);
        assertEquals(cachedResult.getEffect(), result.getEffect());

        Thread.sleep(1);
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_ONE.getName());
        assertNull(this.cache.get(key));
    }

    @Test
    public void testGetWithResetForMultiplePolicySets() throws Exception {

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_ONE.getName());
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_TWO.getName());
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        request.setPolicySetsEvaluationOrder(EVALUATION_ORDER_POLICYONE_POLICYTWO);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = mockPermitResult();
        this.cache.set(key, result);

        PolicyEvaluationResult cachedResult = this.cache.get(key);
        assertNotNull(cachedResult);
        assertEquals(cachedResult.getEffect(), result.getEffect());

        Thread.sleep(1);
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_TWO.getName());
        assertNull(this.cache.get(key));
    }

    @Test
    public void testGetWithPolicyEvaluationOrderChange() throws Exception {

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_ONE.getName());
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_TWO.getName());
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        request.setPolicySetsEvaluationOrder(EVALUATION_ORDER_POLICYONE_POLICYTWO);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = mockPermitResult();
        this.cache.set(key, result);

        PolicyEvaluationResult cachedResult = this.cache.get(key);
        assertNotNull(cachedResult);
        assertEquals(cachedResult.getEffect(), result.getEffect());

        Thread.sleep(1);

        request.setPolicySetsEvaluationOrder(EVALUATION_ORDER_POLICYTWO_POLICYONE);
        key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME).request(request).build();

        assertNull(this.cache.get(key));
    }

    @Test
    public void testGetWithResetForResource() throws Exception {

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_ONE.getName());
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_TWO.getName());
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = mockPermitResult();
        this.cache.set(key, result);

        PolicyEvaluationResult cachedResult = this.cache.get(key);
        assertEquals(cachedResult.getEffect(), result.getEffect());

        Thread.sleep(1);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        assertNull(this.cache.get(key));
    }

    @Test
    public void testGetWithResetForLongResource() throws Exception {

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier("/v1/x-files");
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = mockPermitResult();
        this.cache.set(key, result);

        PolicyEvaluationResult cachedResult = this.cache.get(key);
        assertEquals(cachedResult.getEffect(), result.getEffect());

        Thread.sleep(1);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        assertNull(this.cache.get(key));
    }

    @Test
    public void testGetWithResetForResources() throws Exception {

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = mockPermitResult();
        this.cache.set(key, result);

        PolicyEvaluationResult cachedResult = this.cache.get(key);
        assertEquals(cachedResult.getEffect(), result.getEffect());

        Thread.sleep(1);
        this.cache.resetForResources(ZONE_NAME, Collections.singletonList(new ResourceEntity(ZONE_ENTITY, XFILES_ID)));
        assertNull(this.cache.get(key));
    }

    @Test
    public void testGetWithResetForResolvedResources() throws Exception {

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        String resolvedResourceUri = "/resolved-resource";
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = mockPermitResult();
        result.setResolvedResourceUris(Collections.singleton(resolvedResourceUri));
        this.cache.set(key, result);

        PolicyEvaluationResult cachedResult = this.cache.get(key);
        assertEquals(cachedResult.getEffect(), result.getEffect());

        Thread.sleep(1);
        this.cache.resetForResources(ZONE_NAME,
                Collections.singletonList(new ResourceEntity(ZONE_ENTITY, resolvedResourceUri)));
        assertNull(this.cache.get(key));
    }

    @Test
    public void testGetWithResetForResourcesByIds() throws Exception {

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = mockPermitResult();
        this.cache.set(key, result);

        PolicyEvaluationResult cachedResult = this.cache.get(key);
        assertEquals(cachedResult.getEffect(), result.getEffect());

        Thread.sleep(1);
        this.cache.resetForResourcesByIds(ZONE_NAME, Collections.singleton(XFILES_ID));
        assertNull(this.cache.get(key));
    }

    @Test
    public void testGetWithResetForSubject() throws Exception {

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = mockPermitResult();
        this.cache.set(key, result);

        PolicyEvaluationResult cachedResult = this.cache.get(key);
        assertEquals(cachedResult.getEffect(), result.getEffect());

        Thread.sleep(1);
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        assertNull(this.cache.get(key));
    }

    @Test
    public void testGetWithResetForSubjectsByIds() throws Exception {

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = mockPermitResult();
        this.cache.set(key, result);

        PolicyEvaluationResult cachedResult = this.cache.get(key);
        assertEquals(cachedResult.getEffect(), result.getEffect());

        Thread.sleep(1);
        this.cache.resetForSubjectsByIds(ZONE_NAME, Collections.singleton(AGENT_MULDER));
        assertNull(this.cache.get(key));
    }

    @Test
    public void testGetWithResetForSubjects() throws Exception {

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = mockPermitResult();
        this.cache.set(key, result);

        PolicyEvaluationResult cachedResult = this.cache.get(key);
        assertEquals(cachedResult.getEffect(), result.getEffect());

        Thread.sleep(1);
        this.cache.resetForSubjects(ZONE_NAME, Collections.singletonList(new SubjectEntity(ZONE_ENTITY, AGENT_MULDER)));
        assertNull(this.cache.get(key));
    }

    @Test
    public void testGetWithReset() {

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER);
        this.cache.resetForResource(ZONE_NAME, XFILES_ID);
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = mockPermitResult();
        this.cache.set(key, result);

        PolicyEvaluationResult cachedResult = this.cache.get(key);
        assertEquals(cachedResult.getEffect(), result.getEffect());

        this.cache.reset();
        assertNull(this.cache.get(key));
    }

    @Test(dataProvider = "intervalProvider")
    public void testHaveConnectorIntervalsLapsed(final AttributeConnector resourceConnector,
            final AttributeConnector subjectConnector, final DateTime currentTime,
            final boolean haveConnectorCacheIntervalsLapsed) {
        AttributeConnectorService connectorService = Mockito.mock(AttributeConnectorServiceImpl.class);

        Mockito.doReturn(resourceConnector).when(connectorService).getResourceAttributeConnector();
        Mockito.doReturn(subjectConnector).when(connectorService).getSubjectAttributeConnector();
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_ONE.getName());

        boolean isResourceConnectorConfigured = resourceConnector != null;
        boolean isSubjectConnectorConfigured = subjectConnector != null;
        Mockito.doReturn(isResourceConnectorConfigured).when(connectorService).isResourceAttributeConnectorConfigured();
        Mockito.doReturn(isSubjectConnectorConfigured).when(connectorService).isSubjectAttributeConnectorConfigured();

        InMemoryPolicyEvaluationCache spiedCache = Mockito.spy(this.cache);
        ReflectionTestUtils.setField(spiedCache, "connectorService", connectorService);

        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        request.setPolicySetsEvaluationOrder(EVALUATION_ORDER_POLICYONE);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult expectedResult = mockPermitResult();
        spiedCache.set(key, expectedResult);

        PolicyEvaluationResult actualResult = spiedCache.get(key);
        Assert.assertEquals(actualResult.getEffect(), expectedResult.getEffect());
        Assert.assertEquals(actualResult.getResourceAttributes(), expectedResult.getResourceAttributes());
        Assert.assertEquals(actualResult.getSubjectAttributes(), expectedResult.getSubjectAttributes());

        Mockito.verify(spiedCache, Mockito.times(isResourceConnectorConfigured || isSubjectConnectorConfigured ? 1 : 2))
                .haveEntitiesChanged(Mockito.any(), Mockito.any());
        Mockito.verify(spiedCache, Mockito.times(isResourceConnectorConfigured || isSubjectConnectorConfigured ? 1 : 0))
                .haveConnectorCacheIntervalsLapsed(Mockito.any(), Mockito.any());
        Assert.assertEquals(this.cache.haveConnectorCacheIntervalsLapsed(connectorService, currentTime),
                haveConnectorCacheIntervalsLapsed);

    }

    private static PolicyEvaluationResult mockPermitResult() {
        PolicyEvaluationResult result = new PolicyEvaluationResult(Effect.PERMIT);
        result.setResolvedResourceUris(Collections.singleton(XFILES_ID));
        return result;
    }

    @DataProvider
    private Object[][] intervalProvider() {
        return new Object[][] { allConnectorsConfiguredNoneElapsed(), allConnectorsConfiguredOnlyResourceElapsed(),
                allConnectorsConfiguredOnlySubjectElapsed(), connectorsNotConfigured(),
                onlyResourceConnectorConfiguredAndElapsed(), onlySubjectConnectorConfiguredAndElapsed() };
    }

    private Object[] allConnectorsConfiguredNoneElapsed() {
        AttributeConnector resourceConnector = new AttributeConnector();
        AttributeConnector subjectConnector = new AttributeConnector();
        resourceConnector.setMaxCachedIntervalMinutes(1);
        subjectConnector.setMaxCachedIntervalMinutes(1);

        return new Object[] { resourceConnector, subjectConnector, DateTime.now(), false };
    }

    private Object[] allConnectorsConfiguredOnlyResourceElapsed() {
        AttributeConnector resourceConnector = new AttributeConnector();
        AttributeConnector subjectConnector = new AttributeConnector();
        resourceConnector.setMaxCachedIntervalMinutes(1);
        subjectConnector.setMaxCachedIntervalMinutes(4);

        return new Object[] { resourceConnector, subjectConnector, DateTime.now().minusMinutes(3), true };
    }

    private Object[] allConnectorsConfiguredOnlySubjectElapsed() {
        AttributeConnector resourceConnector = new AttributeConnector();
        AttributeConnector subjectConnector = new AttributeConnector();
        resourceConnector.setMaxCachedIntervalMinutes(4);
        subjectConnector.setMaxCachedIntervalMinutes(1);

        return new Object[] { resourceConnector, subjectConnector, DateTime.now().minusMinutes(3), true };
    }

    private Object[] onlyResourceConnectorConfiguredAndElapsed() {
        AttributeConnector resourceConnector = new AttributeConnector();
        resourceConnector.setMaxCachedIntervalMinutes(1);

        return new Object[] { resourceConnector, null, DateTime.now().minusMinutes(3), true };
    }

    private Object[] onlySubjectConnectorConfiguredAndElapsed() {
        AttributeConnector subjectConnector = new AttributeConnector();
        subjectConnector.setMaxCachedIntervalMinutes(1);

        return new Object[] { null, subjectConnector, DateTime.now().minusMinutes(3), true };
    }

    private Object[] connectorsNotConfigured() {
        return new Object[] { null, null, DateTime.now().minusMinutes(3), false };
    }

}
