/*******************************************************************************
 * Copyright 2018 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.integration.test

import org.apache.commons.lang.StringUtils
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.rest.AttributeAdapterConnection
import org.eclipse.keti.acs.rest.AttributeConnector
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.test.utils.ACSITSetUpFactory
import org.eclipse.keti.test.utils.ACSTestUtil
import org.eclipse.keti.test.utils.PolicyHelper
import org.eclipse.keti.test.utils.ZoneFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.sleuth.DefaultSpanNamer
import org.springframework.cloud.sleuth.Span
import org.springframework.cloud.sleuth.TraceKeys
import org.springframework.cloud.sleuth.instrument.web.HttpTraceKeysInjector
import org.springframework.cloud.sleuth.instrument.web.ZipkinHttpSpanInjector
import org.springframework.cloud.sleuth.instrument.web.client.TraceRestTemplateInterceptor
import org.springframework.cloud.sleuth.log.NoOpSpanLogger
import org.springframework.cloud.sleuth.sampler.AlwaysSampler
import org.springframework.cloud.sleuth.trace.DefaultTracer
import org.springframework.cloud.sleuth.util.ArrayListSpanAccumulator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.web.client.RestTemplate
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.IOException
import java.net.URI
import java.util.HashSet
import java.util.Random

@Configuration
@ImportResource("classpath:integration-test-spring-context.xml")
internal class PolicyEvaluationWithAttributeConnectorITConfiguration {

    @Autowired
    private lateinit var acsitSetUpFactory: ACSITSetUpFactory

    @Autowired
    private lateinit var tracer: DefaultTracer

    private fun setRestTemplateInterceptor(restTemplate: RestTemplate) {
        restTemplate.interceptors = listOf<ClientHttpRequestInterceptor>(
            TraceRestTemplateInterceptor(
                this.tracer, ZipkinHttpSpanInjector(),
                HttpTraceKeysInjector(this.tracer, TraceKeys())
            )
        )
    }

    @Bean
    fun tracer(): DefaultTracer {
        return DefaultTracer(
            AlwaysSampler(), Random(), DefaultSpanNamer(), NoOpSpanLogger(),
            ArrayListSpanAccumulator(), TraceKeys()
        )
    }

    @Bean
    @Throws(IOException::class)
    fun acsAdminRestTemplate(): OAuth2RestTemplate {
        val acsAdminRestTemplate: OAuth2RestTemplate = this.acsitSetUpFactory.acsZonesAdminRestTemplate
        this.acsitSetUpFactory.setUp()
        setRestTemplateInterceptor(acsAdminRestTemplate)
        return acsAdminRestTemplate
    }

    @Bean
    fun acsitSetUpFactory(): ACSITSetUpFactory? {
        return this.acsitSetUpFactory
    }
}

private const val TEST_PART_ID = "part/03f95db1-4255-4265-a509-f7bca3e1fee4"

@ContextConfiguration(classes = [(PolicyEvaluationWithAttributeConnectorITConfiguration::class)])
class PolicyEvaluationWithAttributeConnectorIT : AbstractTestNGSpringContextTests() {

    @Value("\${ACS_UAA_URL}")
    private lateinit var acsUaaUrl: String

    @Value("\${ADAPTER_ENDPOINT:\${ASSET_ADAPTER_URL}}")
    private lateinit var adapterEndpoint: String

    @Value("\${ADAPTER_UAA_TOKEN_URL:\${ASSET_TOKEN_URL}}")
    private lateinit var adapterUaaTokenUrl: String

    @Value("\${ADAPTER_UAA_CLIENT_ID:\${ASSET_CLIENT_ID}}")
    private lateinit var adapterUaaClientId: String

    @Value("\${ADAPTER_UAA_CLIENT_SECRET:\${ASSET_CLIENT_SECRET}}")
    private lateinit var adapterUaaClientSecret: String

    @Autowired
    private val zoneFactory: ZoneFactory? = null

    @Autowired
    private val policyHelper: PolicyHelper? = null

    @Autowired
    private val acsAdminRestTemplate: OAuth2RestTemplate? = null

    @Autowired
    private val acsitSetUpFactory: ACSITSetUpFactory? = null

    @Autowired
    private val tracer: DefaultTracer? = null

    private var zone: Zone? = null

    private var resourceAttributeConnectorUrl: URI? = null

    @Throws(IOException::class)
    private fun zoneHeader(): HttpHeaders {
        val httpHeaders = ACSTestUtil.httpHeaders()
        httpHeaders.set(PolicyHelper.PREDIX_ZONE_ID, this.zone!!.subdomain)
        return httpHeaders
    }

    @Throws(IOException::class)
    private fun configureAttributeConnector(isActive: Boolean) {

        val adapters = listOf(
            AttributeAdapterConnection(
                this.adapterEndpoint, this.adapterUaaTokenUrl, this.adapterUaaClientId,
                this.adapterUaaClientSecret
            )
        )

        val attributeConnector = AttributeConnector()
        attributeConnector.isActive = isActive
        attributeConnector.adapters = HashSet(adapters)
        this.acsAdminRestTemplate!!.exchange(
            this.resourceAttributeConnectorUrl, HttpMethod.PUT,
            HttpEntity(attributeConnector, zoneHeader()), AttributeConnector::class.java
        )
    }

    @BeforeClass
    @Throws(IOException::class)
    internal fun beforeClass() {

        this.zone = this.acsitSetUpFactory!!.zone1
        this.resourceAttributeConnectorUrl = URI.create(this.zoneFactory!!.acsBaseURL + "/v1/connector/resource")
    }

    @Throws(IOException::class)
    private fun deconfigureAttributeConnector() {
        this.acsAdminRestTemplate!!
            .exchange(
                this.resourceAttributeConnectorUrl, HttpMethod.DELETE, HttpEntity<Any>(zoneHeader()),
                Void::class.java
            )
    }

    @AfterClass
    @Throws(IOException::class)
    internal fun afterClass() {
        this.acsitSetUpFactory!!.destroy()
    }

    @Test(dataProvider = "adapterStatusesAndResultingEffects")
    @Throws(Exception::class)
    fun testPolicyEvaluationWithAdapters(
        adapterActive: Boolean,
        expectedEffect: Effect,
        enableSleuthTracing: Boolean
    ) {
        val testPolicyName = this.policyHelper!!
            .setTestPolicy(
                this.acsAdminRestTemplate, zoneHeader(), this.zoneFactory!!.acsBaseURL,
                "src/test/resources/policy-set-with-one-policy-using-resource-attributes-from-asset-adapter" + ".json"
            )

        try {
            this.configureAttributeConnector(adapterActive)
            val policyEvaluationRequest = this.policyHelper
                .createEvalRequest("GET", "testSubject", TEST_PART_ID, null)

            if (enableSleuthTracing) {
                this.tracer!!.continueSpan(Span.builder().traceId(1L).spanId(2L).parent(3L).build())
            }

            val policyEvaluationResponse = this.acsAdminRestTemplate!!
                .postForEntity(
                    this.zoneFactory.acsBaseURL + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    HttpEntity(policyEvaluationRequest, zoneHeader()), PolicyEvaluationResult::class.java
                )
            Assert.assertEquals(policyEvaluationResponse.statusCode, HttpStatus.OK)

            val responseHeaders = policyEvaluationResponse.headers
            Assert.assertTrue(
                responseHeaders.containsKey(Span.TRACE_ID_NAME) && StringUtils.isNotEmpty(Span.TRACE_ID_NAME)
            )
            if (enableSleuthTracing) {
                Assert.assertEquals(Span.hexToId(responseHeaders[Span.TRACE_ID_NAME]?.get(0)), 1L)
            }

            val policyEvaluationResult = policyEvaluationResponse.body
            Assert.assertEquals(policyEvaluationResult.effect, expectedEffect)
        } finally {
            this.policyHelper
                .deletePolicySet(
                    this.acsAdminRestTemplate, this.zoneFactory.acsBaseURL, testPolicyName,
                    zoneHeader()
                )
            this.deconfigureAttributeConnector()
        }
    }

    @DataProvider
    private fun adapterStatusesAndResultingEffects(): Array<Array<Any?>> {
        return arrayOf<Array<Any?>>(
            arrayOf(true, Effect.PERMIT, true),
            arrayOf(true, Effect.PERMIT, false),
            arrayOf(false, Effect.NOT_APPLICABLE, true)
        )
    }
}
