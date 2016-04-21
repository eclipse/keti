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
package com.ge.predix.acs.service.policy.admin;

import static org.mockito.Mockito.mock;

import java.util.List;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ge.predix.acs.SpringSecurityPolicyContextResolver;
import com.ge.predix.acs.commons.policy.condition.groovy.GroovyConditionCache;
import com.ge.predix.acs.commons.policy.condition.groovy.GroovyConditionShell;
import com.ge.predix.acs.config.InMemoryDataSourceConfig;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.model.PolicySet;
import com.ge.predix.acs.policy.evaluation.cache.HystrixPolicyEvaluationCacheCircuitBreaker;
import com.ge.predix.acs.policy.evaluation.cache.InMemoryPolicyEvaluationCache;
import com.ge.predix.acs.service.InvalidACSRequestException;
import com.ge.predix.acs.service.policy.validation.PolicySetValidatorImpl;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.management.dao.ZoneRepository;
import com.ge.predix.acs.zone.resolver.SpringSecurityZoneResolver;
import com.ge.predix.acs.zone.resolver.ZoneResolver;

@Test
@ActiveProfiles(profiles = { "h2", "public", "simple-cache" })
@ContextConfiguration(
        classes = { HystrixPolicyEvaluationCacheCircuitBreaker.class, InMemoryPolicyEvaluationCache.class,
                InMemoryDataSourceConfig.class, PolicyManagementServiceImpl.class,
                SpringSecurityPolicyContextResolver.class, PolicySetValidatorImpl.class, GroovyConditionShell.class,
                SpringSecurityZoneResolver.class, GroovyConditionCache.class })
public class PolicyManagementServiceTest extends AbstractTransactionalTestNGSpringContextTests {

    private static final String SUBDOMAIN1 = "tenant1";
    private static final String SUBDOMAIN2 = "tenant2";
    private static final String DEFAULT_SUBDOMAIN = "defaultTenant";

    @Autowired
    @InjectMocks
    private PolicyManagementServiceImpl policyService;

    @Autowired
    private ZoneRepository zoneRepository;

    @Mock
    private final ZoneResolver mockZoneResolver = mock(ZoneResolver.class);

    private final JsonUtils jsonUtils = new JsonUtils();

    private final ZoneEntity zone1 = this.createZone("zone1", SUBDOMAIN1, "description for Zone1");
    private final ZoneEntity zone2 = this.createZone("zone2", SUBDOMAIN2, "description for Zone2");
    private final ZoneEntity defaultZone = this.createZone("defaultZone", DEFAULT_SUBDOMAIN,
            "description for defaultZone");

    @BeforeClass
    public void beforeClass() {
        this.zoneRepository.save(this.zone1);
        this.zoneRepository.save(this.zone2);
        this.zoneRepository.save(this.defaultZone);
    }

    @AfterClass
    public void afterClass() {

        this.zoneRepository.delete(this.defaultZone);
        this.zoneRepository.delete(this.zone1);
        this.zoneRepository.delete(this.zone2);
    }

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        initializeDefaultResolverBehavior();
    }

    public void testDeleteWhenPolicySetExists() {
        PolicySet policySet = this.jsonUtils.deserializeFromFile("set-with-1-policy.json", PolicySet.class);
        this.policyService.upsertPolicySet(policySet);
        this.policyService.deletePolicySet(policySet.getName());
        PolicySet retrievedPolicySet = this.policyService.getPolicySet(policySet.getName());
        Assert.assertNull(retrievedPolicySet);
    }

    public void testDeleteWhenPolicySetDoesNotExists() {
        this.policyService.deletePolicySet("policyId");
        Assert.assertTrue(true); // no exception throw means, the test passed
    }

    public void testDeleteWhenPolicySetIdIsNull() {
        this.policyService.deletePolicySet(null);
        Assert.assertTrue(true); // no exception throw means, the test passed
    }

    public void testCreatePolicySetPositive() {
        PolicySet policySet = this.jsonUtils.deserializeFromFile("set-with-1-policy.json", PolicySet.class);
        String policyName = policySet.getName();
        this.policyService.upsertPolicySet(policySet);
        PolicySet savedPolicySet = this.policyService.getPolicySet(policyName);
        Assert.assertNotNull(savedPolicySet);
        Assert.assertEquals(savedPolicySet.getName(), policyName);
        this.policyService.deletePolicySet(policyName);
        Assert.assertEquals(this.policyService.getAllPolicySets().size(), 0);
    }

    @Test
    public void testCreateApmPolicySetPositive() {
        PolicySet policySet = this.jsonUtils.deserializeFromFile("testApmPolicySetLoadsSuccessfully.json",
                PolicySet.class);
        String policyName = policySet.getName();
        this.policyService.upsertPolicySet(policySet);
        PolicySet savedPolicySet = this.policyService.getPolicySet(policyName);
        Assert.assertNotNull(savedPolicySet);
        Assert.assertEquals(savedPolicySet.getName(), policyName);
        this.policyService.deletePolicySet(policyName);
        Assert.assertEquals(this.policyService.getAllPolicySets().size(), 0);
    }

    public void testUpdatePolicySet() {
        PolicySet policySet = this.jsonUtils.deserializeFromFile("set-with-1-policy.json", PolicySet.class);

        this.policyService.upsertPolicySet(policySet);

        PolicySet retPolicySet = this.policyService.getPolicySet(policySet.getName());
        Assert.assertEquals(retPolicySet.getName(), policySet.getName());
        Assert.assertEquals(retPolicySet.getPolicies().size(), policySet.getPolicies().size());

        // Now we want to update
        policySet.getPolicies().get(0).setEffect(Effect.DENY);
        this.policyService.upsertPolicySet(policySet);

        // get the policy back
        retPolicySet = this.policyService.getPolicySet(policySet.getName());

        Assert.assertEquals(retPolicySet.getPolicies().get(0).getEffect(), Effect.DENY);

        this.policyService.deletePolicySet(policySet.getName());
        Assert.assertEquals(this.policyService.getAllPolicySets().size(), 0);

    }

    public void testCreateMultiplePolicySet() {
        PolicySet policySet = this.jsonUtils.deserializeFromFile("set-with-1-policy.json", PolicySet.class);
        PolicySet policySet2 = this.jsonUtils.deserializeFromFile("policy-set-with-one-policy-one-condition.json",
                PolicySet.class);

        this.policyService.upsertPolicySet(policySet);
        try {
            this.policyService.upsertPolicySet(policySet2);
            Assert.fail("Creation of 2nd policySet did not throw an exception");
        } catch (PolicyManagementException e) {
            // expected
        } finally {
            this.policyService.deletePolicySet(policySet.getName());
            Assert.assertEquals(this.policyService.getAllPolicySets().size(), 0);
        }
    }

    @Test(expectedExceptions = { PolicyManagementException.class })
    public void testCreatePolicySetWithInvalidConditions() {
        PolicySet policySet = this.jsonUtils.deserializeFromFile("policy-set-with-one-policy-invalid-condition.json",
                PolicySet.class);
        this.policyService.upsertPolicySet(policySet);
    }

    @Test(expectedExceptions = { PolicyManagementException.class })
    public void testCreatePolicySetWithInvalidJson() {
        PolicySet policySet = this.jsonUtils.deserializeFromFile("policyset/validator/test/missing-effect-policy.json",
                PolicySet.class);
        this.policyService.upsertPolicySet(policySet);
    }

    @Test(expectedExceptions = { PolicyManagementException.class })
    public void testCreatePolicySetWithMissingClientId() {
        Mockito.when(this.mockZoneResolver.getZoneEntityOrFail()).thenThrow(new InvalidACSRequestException());
        this.createSimplePolicySet();
    }

    @Test(expectedExceptions = { PolicyManagementException.class })
    public void testCreatePolicySetWithMissingIssuer() {
        Mockito.when(this.mockZoneResolver.getZoneEntityOrFail()).thenThrow(new InvalidACSRequestException());
        this.createSimplePolicySet();
    }

    public void testCreatePolicySetsForMultipleApplications() {
        PolicySet client1PolicySet = this.jsonUtils.deserializeFromFile("set-with-1-policy.json", PolicySet.class);
        PolicySet client2PolicySet = this.jsonUtils.deserializeFromFile("policy-set-with-one-policy-one-condition.json",
                PolicySet.class);

        Mockito.when(this.mockZoneResolver.getZoneEntityOrFail()).thenReturn(this.zone1);

        this.policyService.upsertPolicySet(client1PolicySet);
        Assert.assertEquals(this.policyService.getAllPolicySets().size(), 1);
        Assert.assertEquals(this.policyService.getAllPolicySets().get(0).getName(), client1PolicySet.getName());

        // Add and assert policyset for client2, with client1 policySet already
        // created

        Mockito.when(this.mockZoneResolver.getZoneEntityOrFail()).thenReturn(this.zone2);
        this.policyService.upsertPolicySet(client2PolicySet);
        Assert.assertEquals(this.policyService.getAllPolicySets().size(), 1);
        Assert.assertEquals(this.policyService.getAllPolicySets().get(0).getName(), client2PolicySet.getName());

        this.policyService.deletePolicySet(client2PolicySet.getName());

        // Cleanup PolicySet for client1
        Mockito.when(this.mockZoneResolver.getZoneEntityOrFail()).thenReturn(this.zone1);
        this.policyService.deletePolicySet(client1PolicySet.getName());
        Assert.assertEquals(this.policyService.getAllPolicySets().size(), 0);
    }

    // TODO Enable this test once the service is updated to use real zones
    @Test(enabled = false)
    public void testGetAllPolicySetAndReturnEmptyList() {
        Mockito.when(this.mockZoneResolver.getZoneEntityOrFail()).thenReturn(this.zone1);

        List<PolicySet> allPolicySets = this.policyService.getAllPolicySets();
        Assert.assertEquals(allPolicySets.size(), 0);
    }

    public void testCreatePolicySetsForMultipleZones() {
        PolicySet issuer1PolicySet = this.jsonUtils.deserializeFromFile("set-with-1-policy.json", PolicySet.class);
        PolicySet issuer2PolicySet = this.jsonUtils.deserializeFromFile("policy-set-with-one-policy-one-condition.json",
                PolicySet.class);

        Mockito.when(this.mockZoneResolver.getZoneEntityOrFail()).thenReturn(this.zone1);
        this.policyService.upsertPolicySet(issuer1PolicySet);

        Mockito.when(this.mockZoneResolver.getZoneEntityOrFail()).thenReturn(this.zone2);
        this.policyService.upsertPolicySet(issuer2PolicySet);

        Assert.assertEquals(this.policyService.getAllPolicySets().size(), 1);

        this.policyService.deletePolicySet(issuer2PolicySet.getName());
        // need this to delete issuer1PolicySet properly (policy-set-id and
        // zone_id are used to find the row)
        Mockito.when(this.mockZoneResolver.getZoneEntityOrFail()).thenReturn(this.zone1);
        this.policyService.deletePolicySet(issuer1PolicySet.getName());
        Assert.assertEquals(this.policyService.getAllPolicySets().size(), 0);

    }

    private void createSimplePolicySet() {
        PolicySet policySet = this.jsonUtils.deserializeFromFile("set-with-1-policy.json", PolicySet.class);
        this.policyService.upsertPolicySet(policySet);
    }

    private void initializeDefaultResolverBehavior() {
        Mockito.when(this.mockZoneResolver.getZoneEntityOrFail()).thenReturn(this.defaultZone);
    }

    private ZoneEntity createZone(final String name, final String subdomain, final String description) {
        ZoneEntity zone = new ZoneEntity();
        zone.setName(name);
        zone.setSubdomain(subdomain);
        zone.setDescription(description);
        return zone;
    }

}
