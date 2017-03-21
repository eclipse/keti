package com.ge.predix.acs.policy.evaluation.cache;

import static com.ge.predix.acs.policy.evaluation.cache.AbstractPolicyEvaluationCacheTest.ZONE_NAME;
import static com.ge.predix.acs.policy.evaluation.cache.AbstractPolicyEvaluationCacheTest.mockPermitResult;
import static com.ge.predix.acs.testutils.XFiles.AGENT_MULDER;
import static com.ge.predix.acs.testutils.XFiles.XFILES_ID;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ge.predix.acs.attribute.connector.management.AttributeConnectorServiceImpl;
import com.ge.predix.acs.attribute.readers.AttributeReaderFactory;
import com.ge.predix.acs.attribute.readers.PrivilegeServiceResourceAttributeReader;
import com.ge.predix.acs.attribute.readers.PrivilegeServiceSubjectAttributeReader;
import com.ge.predix.acs.config.InMemoryDataSourceConfig;
import com.ge.predix.acs.privilege.management.PrivilegeManagementServiceImpl;
import com.ge.predix.acs.privilege.management.dao.ResourceEntity;
import com.ge.predix.acs.privilege.management.dao.ResourceRepositoryProxy;
import com.ge.predix.acs.privilege.management.dao.SubjectEntity;
import com.ge.predix.acs.privilege.management.dao.SubjectRepositoryProxy;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.resolver.SpringSecurityZoneResolver;
import com.ge.predix.acs.zone.resolver.ZoneResolver;

@ActiveProfiles(profiles = { "h2", "simple-cache" })
@TestPropertySource("classpath:application.properties")
@ContextConfiguration(
        classes = { DefaultPolicyEvaluationCacheCircuitBreakerTest.Config.class,
                HystrixPolicyEvaluationCacheCircuitBreaker.class, SpringSecurityZoneResolver.class,
                AttributeConnectorServiceImpl.class, InMemoryDataSourceConfig.class, AttributeReaderFactory.class,
                PrivilegeServiceResourceAttributeReader.class, PrivilegeManagementServiceImpl.class,
                SubjectRepositoryProxy.class, ResourceRepositoryProxy.class,
                PrivilegeServiceSubjectAttributeReader.class })
public class DefaultPolicyEvaluationCacheCircuitBreakerTest extends AbstractTestNGSpringContextTests {
    public static final String ACTION_GET = "GET";

    @Autowired
    private PolicyEvaluationCacheCircuitBreaker policyEvaluationCacheCircuitBreaker;
    @Autowired
    @InjectMocks
    private InMemoryPolicyEvaluationCache cache;
    @Mock
    private ZoneResolver zoneResolver;
    private final MockBrokenPolicyEvaluationCache brokenCache = new MockBrokenPolicyEvaluationCache();

    @BeforeClass
    public void setup() {
        this.policyEvaluationCacheCircuitBreaker.setCachingEnabled(true);
        MockitoAnnotations.initMocks(this);
        Mockito.when(zoneResolver.getZoneEntityOrFail()).thenReturn(Mockito.mock(ZoneEntity.class));
        ReflectionTestUtils.setField(this.cache, "connectorService", Mockito.mock(AttributeConnectorServiceImpl.class));
    }

    @BeforeMethod
    public void setupMethod() {
        closeCircuit();
    }

    /**
     * Tests that the Hystrix circuit breaker will make sure that a cache get or set is always returns a result even
     * if the underlying cache implementation fails.
     */
    @Test
    public void testGetAndSetPolicyEvaluationCache() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationResult result = mockPermitResult();
        this.policyEvaluationCacheCircuitBreaker.set(key, result);
        assertNotNull(this.policyEvaluationCacheCircuitBreaker.get(key));
        this.cache.reset();

        openCircuit();

        this.policyEvaluationCacheCircuitBreaker.set(key, result);
        // Verifies that the cache get works even though the underlying cache implementation fails.
        assertNull(this.policyEvaluationCacheCircuitBreaker.get(key));
        // Verifies that there was no result in the working cache implementation.
        assertNull(this.cache.get(key));
    }

    /**
     * Tests that the Hystrix circuit breaker will make sure that storing resource translations is always successful.
     */
    @Test
    public void testResourceTranslationPolicyEvaluationCache() {
        this.policyEvaluationCacheCircuitBreaker.setResourceTranslation(ZONE_NAME, XFILES_ID, "/v1/x-files");
        this.policyEvaluationCacheCircuitBreaker.setResourceTranslations(ZONE_NAME,
                new HashSet<>(Arrays.asList(new String[] { XFILES_ID })), "/v1/x-files");
        openCircuit();
        this.policyEvaluationCacheCircuitBreaker.setResourceTranslation(ZONE_NAME, XFILES_ID, "/v1/x-files");
        this.policyEvaluationCacheCircuitBreaker.setResourceTranslations(ZONE_NAME,
                new HashSet<>(Arrays.asList(new String[] { XFILES_ID })), "/v1/x-files");
        // Success happens if the calls above do not throw an exception.
    }

    /**
     * Tests that a policy set reset will always fail if the underlying cache implementation fails.
     */
    @Test
    public void testCircuitBreakerResetForPolicySet() {
        this.policyEvaluationCacheCircuitBreaker.resetForPolicySet(ZONE_NAME, "default");
        openCircuit();
        try {
            this.policyEvaluationCacheCircuitBreaker.resetForPolicySet(ZONE_NAME, "default");
        } catch (IllegalStateException ex) {
            // Success
            return;
        }
        fail("Test did not throw expected exception.");
    }

    /**
     * Tests that a subject reset will always fail if the underlying cache implementation fails.
     */
    @Test
    public void testCircuitBreakerResetForSubject() {
        this.policyEvaluationCacheCircuitBreaker.resetForSubject(ZONE_NAME, AGENT_MULDER);
        openCircuit();
        try {
            this.policyEvaluationCacheCircuitBreaker.resetForSubject(ZONE_NAME, AGENT_MULDER);
        } catch (IllegalStateException ex) {
            // Success
            return;
        }
        fail("Test did not throw expected exception.");
    }

    /**
     * Tests that multiple subject resets will always fail if the underlying cache implementation fails.
     */
    @Test
    public void testCircuitBreakerResetForSubjects() {
        this.policyEvaluationCacheCircuitBreaker.resetForSubjects(ZONE_NAME, Arrays.asList(new SubjectEntity[] {}));
        openCircuit();
        try {
            this.policyEvaluationCacheCircuitBreaker.resetForSubjects(ZONE_NAME, Arrays.asList(new SubjectEntity[] {}));
        } catch (IllegalStateException ex) {
            // Success
            return;
        }
        fail("Test did not throw expected exception.");
    }

    /**
     * Tests that a resource reset will always fail if the underlying cache implementation fails.
     */
    @Test
    public void testCircuitBreakerResetForResource() {
        this.policyEvaluationCacheCircuitBreaker.resetForResource(ZONE_NAME, XFILES_ID);
        openCircuit();
        try {
            this.policyEvaluationCacheCircuitBreaker.resetForResource(ZONE_NAME, XFILES_ID);
        } catch (IllegalStateException ex) {
            // Success
            return;
        }
        fail("Test did not throw expected exception.");
    }

    /**
     * Tests that multiple resource resets will always fail if the underlying cache implementation fails.
     */
    @Test
    public void testCircuitBreakerResetForResources() {
        this.policyEvaluationCacheCircuitBreaker.resetForResources(ZONE_NAME, Arrays.asList(new ResourceEntity[] {}));
        openCircuit();
        try {
            this.policyEvaluationCacheCircuitBreaker.resetForResources(ZONE_NAME,
                    Arrays.asList(new ResourceEntity[] {}));
        } catch (IllegalStateException ex) {
            // Success
            return;
        }
        fail("Test did not throw expected exception.");
    }

    public void closeCircuit() {
        this.policyEvaluationCacheCircuitBreaker.setCacheImpl(this.cache);
    }

    public void openCircuit() {
        this.policyEvaluationCacheCircuitBreaker.setCacheImpl(this.brokenCache);
    }

    @ComponentScan(value = { "com.ge.predix.acs.policy.evaluation.cache" })
    @Configuration
    @EnableAspectJAutoProxy
    @EnableCircuitBreaker
    public static class Config {

        @Bean
        public PropertySourcesPlaceholderConfigurer properties() throws Exception {
            final PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
            Properties properties = new Properties();
            pspc.setProperties(properties);
            return pspc;
        }
    }
}
