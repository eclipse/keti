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

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.test.TestConfig
import org.eclipse.keti.test.utils.ACSITSetUpFactory
import org.eclipse.keti.test.utils.ACS_RESOURCE_API_PATH
import org.eclipse.keti.test.utils.ACS_SUBJECT_API_PATH
import org.eclipse.keti.test.utils.PREDIX_ZONE_ID
import org.eclipse.keti.test.utils.PrivilegeHelper
import org.eclipse.keti.test.utils.ZoneFactory
import org.eclipse.keti.test.utils.httpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URLEncoder
import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet
import javax.security.auth.Subject

@ContextConfiguration("classpath:integration-test-spring-context.xml")
// @Test(dependsOnGroups = { "acsHealthCheck.*" })
@Test
class PrivilegeManagementAccessControlServiceIT : AbstractTestNGSpringContextTests() {

    private var acsZone1Name: String? = null

    @Value("\${zone1UaaUrl}/oauth/token")
    private lateinit var primaryZoneIssuerId: String

    private var acsZone3Name: String? = null

    @Autowired
    private lateinit var zoneFactory: ZoneFactory

    @Autowired
    private lateinit var privilegeHelper: PrivilegeHelper

    @Autowired
    private lateinit var acsitSetUpFactory: ACSITSetUpFactory

    private var acsUrl: String? = null
    private var zone1Headers: HttpHeaders? = null
    private var zone3Headers: HttpHeaders? = null
    private var acsAdminRestTemplate: OAuth2RestTemplate? = null

    val subjectProvider: Array<Array<Any?>>
        @DataProvider(name = "subjectProvider")
        get() = arrayOf<Array<Any?>>(
            arrayOf(MARISSA_V1),
            arrayOf(JOE_V1),
            arrayOf(PETE_V1),
            arrayOf(JLO_V1),
            arrayOf(BOB_V1)
        )

    // empty subjectIdentifier
    val invalidSubjectsPost: Array<Array<Any?>>
        @DataProvider(name = "invalidSubjectPostProvider")
        get() = arrayOf(arrayOf(BaseSubject(null), acsUrl))

    // non empty resourceIdentifier
    val resourcesPost: Array<Array<Any?>>
        @DataProvider(name = "resourcePostProvider")
        get() = arrayOf(arrayOf(BaseResource("/sites/sanramon"), acsUrl))

    // empty resourceIdentifier
    val invalidResourcesPost: Array<Array<Any?>>
        @DataProvider(name = "invalidResourcePostProvider")
        get() = arrayOf(arrayOf(BaseResource(null), acsUrl))

    // non empty subjectIdentifier
    val subjectsPost: Array<Array<Any?>>
        @DataProvider(name = "subjectPostProvider")
        get() = arrayOf(arrayOf(MARISSA_V1, acsUrl))

    val acsEndpoint: Array<Array<Any?>>
        @DataProvider(name = "endpointProvider")
        get() = arrayOf<Array<Any?>>(arrayOf(acsUrl))

    @BeforeClass
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun setup() {
        TestConfig.setupForEclipse() // Starts ACS when running the test in eclipse.
        this.acsitSetUpFactory.setUp()
        this.acsUrl = this.acsitSetUpFactory.acsUrl
        this.zone1Headers = this.acsitSetUpFactory.zone1Headers
        this.zone3Headers = this.acsitSetUpFactory.zone3Headers
        this.acsAdminRestTemplate = this.acsitSetUpFactory.acsZonesAdminRestTemplate
        this.acsZone1Name = this.acsitSetUpFactory.zone1.subdomain
        this.acsZone3Name = this.acsitSetUpFactory.acsZone3Name
    }

    fun testBatchCreateSubjectsEmptyList() {
        val subjects = ArrayList<BaseSubject>()
        try {
            this.acsAdminRestTemplate!!.postForEntity(
                this.acsUrl!! + ACS_SUBJECT_API_PATH,
                HttpEntity<List<BaseSubject>>(subjects, this.zone1Headers), Array<BaseSubject>::class.java
            )
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.UNPROCESSABLE_ENTITY)
            return
        }

        Assert.fail("Expected unprocessable entity http client error.")
    }

    @Test
    fun testBatchSubjectsDataConstraintViolationSubjectIdentifier() {
        val subjects = ArrayList<BaseSubject>()
        subjects.add(this.privilegeHelper.createSubject("marissa"))
        subjects.add(this.privilegeHelper.createSubject("marissa"))

        try {
            this.acsAdminRestTemplate!!.postForEntity(
                this.acsUrl!! + ACS_SUBJECT_API_PATH,
                HttpEntity<List<BaseSubject>>(subjects, this.zone1Headers), ResponseEntity::class.java
            )
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.UNPROCESSABLE_ENTITY)
            return
        }

        this.acsAdminRestTemplate!!
            .exchange(
                this.acsUrl + ACS_SUBJECT_API_PATH + "/marissa", HttpMethod.DELETE,
                HttpEntity<Any>(this.zone1Headers), ResponseEntity::class.java
            )
        Assert.fail("Expected unprocessable entity http client error.")
    }

    fun testCreateSubjectWithMalformedJSON() {
        try {
            val badSubject = "{\"subject\": bad-subject-form\"}"
            val headers = httpHeaders()
            headers.add("Content-type", "application/json")
            headers.add(PREDIX_ZONE_ID, this.acsZone1Name)
            val httpEntity = HttpEntity(badSubject, headers)
            this.acsAdminRestTemplate!!
                .put(this.acsUrl + ACS_SUBJECT_API_PATH + "/bad-subject-form", httpEntity)
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.BAD_REQUEST)
            return
        }

        this.acsAdminRestTemplate!!
            .exchange(
                this.acsUrl + ACS_SUBJECT_API_PATH + "/bad-subject-form", HttpMethod.DELETE,
                HttpEntity<Any>(this.zone1Headers), ResponseEntity::class.java
            )
        Assert.fail("testCreateSubjectWithMalformedJSON should have failed!")
    }

    fun testCreateBatchSubjectsWithMalformedJSON() {
        try {
            val badSubject =
                "{\"subject\":{\"name\" : \"good-subject-brittany\"}," + "{\"subject\": bad-subject-sarah\"}"
            val headers = httpHeaders()
            headers.add("Content-type", "application/json")
            headers.add(PREDIX_ZONE_ID, this.acsZone1Name)
            val httpEntity = HttpEntity(badSubject, headers)
            this.acsAdminRestTemplate!!
                .postForEntity(
                    this.acsUrl!! + ACS_SUBJECT_API_PATH,
                    httpEntity,
                    Array<Subject>::class.java
                )
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.BAD_REQUEST)
            return
        }

        this.acsAdminRestTemplate!!
            .exchange(
                this.acsUrl + ACS_SUBJECT_API_PATH + "/bad-subject-sarah", HttpMethod.DELETE,
                HttpEntity<Any>(this.zone1Headers), ResponseEntity::class.java
            )
        this.acsAdminRestTemplate!!
            .exchange(
                this.acsUrl + ACS_SUBJECT_API_PATH + "/good-subject-brittany",
                HttpMethod.DELETE, HttpEntity<Any>(this.zone1Headers), ResponseEntity::class.java
            )
        Assert.fail("testCreateBatchSubjectsWithMalformedJSON should have failed!")
    }

    fun testCreateResourceWithMalformedJSON() {
        try {
            val badResource = "{\"resource\": bad-resource-form\"}"
            val headers = httpHeaders()
            headers.add("Content-type", "application/json")
            headers.add(PREDIX_ZONE_ID, this.acsZone1Name)
            val httpEntity = HttpEntity(badResource, headers)
            this.acsAdminRestTemplate!!
                .put(this.acsUrl + ACS_RESOURCE_API_PATH + "/bad-resource-form", httpEntity)
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.BAD_REQUEST)
            return
        }

        this.acsAdminRestTemplate!!
            .exchange(
                this.acsUrl + ACS_RESOURCE_API_PATH + "/bad-resource-form", HttpMethod.DELETE,
                HttpEntity<Any>(this.zone1Headers), ResponseEntity::class.java
            )
        Assert.fail("testCreateResourceWithMalformedJSON should have failed!")
    }

    fun testCreateBatchResourcesWithMalformedJSON() {
        try {
            val badResource =
                "{\"resource\":{\"name\" : \"Site\", \"uriTemplate\" : " + "\"/secured-by-value/sites/{site_id}\"},{\"resource\": bad-resource-form\"}"
            val headers = httpHeaders()
            headers.add("Content-type", "application/json")
            headers.add(PREDIX_ZONE_ID, this.acsZone1Name)
            val httpEntity = HttpEntity(badResource, headers)
            this.acsAdminRestTemplate!!.postForEntity(
                this.acsUrl!! + ACS_RESOURCE_API_PATH, httpEntity,
                Array<BaseResource>::class.java
            )
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.BAD_REQUEST)
            return
        }

        this.acsAdminRestTemplate!!
            .exchange(
                this.acsUrl + ACS_RESOURCE_API_PATH + "/bad-resource-form", HttpMethod.DELETE,
                HttpEntity<Any>(this.zone1Headers), ResponseEntity::class.java
            )
        this.acsAdminRestTemplate!!
            .exchange(
                this.acsUrl + ACS_RESOURCE_API_PATH + "/Site", HttpMethod.DELETE,
                HttpEntity<Any>(this.zone1Headers), ResponseEntity::class.java
            )
        Assert.fail("testCreateBatchResourcesWithMalformedJSON should have failed!")
    }

    fun testBatchCreateResourcesEmptyList() {
        val resources = ArrayList<BaseResource>()
        try {
            this.acsAdminRestTemplate!!.postForEntity(
                this.acsUrl!! + ACS_RESOURCE_API_PATH,
                HttpEntity<List<BaseResource>>(resources, this.zone1Headers), Array<BaseResource>::class.java
            )
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.UNPROCESSABLE_ENTITY)
            return
        }

        Assert.fail("Expected unprocessable entity http client error.")
    }

    fun testResourceUpdateAttributes() {
        val resource1 = this.privilegeHelper.createResource("marissa")
        val resource2 = this.privilegeHelper.createResource("marissa")
        val attributes = HashSet<Attribute>()
        val attribute = Attribute()
        attribute.name = "site"
        attribute.issuer = "http://attributes.net"
        attribute.value = "sanfrancisco"
        attributes.add(attribute)
        resource2.attributes = attributes

        this.acsAdminRestTemplate!!.put(
            this.acsUrl + ACS_RESOURCE_API_PATH + "/marissa",
            HttpEntity(resource1, this.zone1Headers)
        )
        this.acsAdminRestTemplate!!.put(
            this.acsUrl + ACS_RESOURCE_API_PATH + "/marissa",
            HttpEntity(resource2, this.zone1Headers)
        )
        val response = acsAdminRestTemplate!!
            .exchange(
                this.acsUrl + ACS_RESOURCE_API_PATH + "/marissa", HttpMethod.GET,
                HttpEntity<Any>(this.zone1Headers), BaseResource::class.java
            )
        Assert.assertEquals(response.body, resource2)
        this.acsAdminRestTemplate!!
            .exchange(
                this.acsUrl + ACS_RESOURCE_API_PATH + "/marissa", HttpMethod.DELETE,
                HttpEntity<Any>(this.zone1Headers), ResponseEntity::class.java
            )
    }

    @Test
    fun testBatchResourcesDataConstraintViolationResourceIdentifier() {
        val resources = ArrayList<BaseResource>()
        resources.add(this.privilegeHelper.createResource("dupResourceIdentifier"))
        resources.add(this.privilegeHelper.createResource("dupResourceIdentifier"))

        try {
            // This POST causes a data constraint violation on the service bcos
            // of duplicate
            // resource_identifiers which returns a HTTP 422 error.
            this.acsAdminRestTemplate!!.postForEntity(
                this.acsUrl!! + ACS_RESOURCE_API_PATH,
                HttpEntity<List<BaseResource>>(resources, this.zone1Headers), ResponseEntity::class.java
            )
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.UNPROCESSABLE_ENTITY)
            return
        }

        this.acsAdminRestTemplate!!
            .exchange(
                this.acsUrl + ACS_RESOURCE_API_PATH + "/marissa", HttpMethod.DELETE,
                HttpEntity<Any>(this.zone1Headers), ResponseEntity::class.java
            )
        Assert
            .fail("Expected unprocessable entity http client error on post for 2 resources with duplicate resource" + "identifiers.")
    }

    @Test(dataProvider = "subjectProvider")
    @Throws(UnsupportedEncodingException::class)
    fun testPutGetDeleteSubject(subject: BaseSubject) {
        val responseEntity: ResponseEntity<BaseSubject>
        try {
            this.privilegeHelper.putSubject(
                this.acsAdminRestTemplate!!, subject, this.acsUrl, this.zone1Headers!!,
                this.privilegeHelper.defaultAttribute
            )
        } catch (e: HttpClientErrorException) {
            Assert.fail("Unable to create subject.")
        }

        val encodedSubjectIdentifier = URLEncoder.encode(subject.subjectIdentifier!!, "UTF-8")
        val uri = URI.create(this.acsUrl + ACS_SUBJECT_API_PATH + encodedSubjectIdentifier)
        try {
            responseEntity = this.acsAdminRestTemplate!!
                .exchange(uri, HttpMethod.GET, HttpEntity<Any>(this.zone1Headers), BaseSubject::class.java)
            Assert.assertEquals(responseEntity.statusCode, HttpStatus.OK)
        } catch (e: HttpClientErrorException) {
            Assert.fail("Unable to get subject.")
        }

        try {
            this.acsAdminRestTemplate!!
                .exchange(uri, HttpMethod.DELETE, HttpEntity<Any>(this.zone1Headers), ResponseEntity::class.java)
            this.acsAdminRestTemplate!!
                .exchange(uri, HttpMethod.GET, HttpEntity<Any>(this.zone1Headers), BaseSubject::class.java)
            Assert.fail("Subject " + subject.subjectIdentifier + " was not properly deleted")
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.NOT_FOUND, "Subject was not deleted.")
        }
    }

    // To test cascade delete for postgres, comment out delete-db-service and delete executions. Run integration with
    // -PCloud. Bind db to pgPhpAdmin and browse the db to ensure all entries with zone 'test-zone-dev3' as a foreign
    // key are deleted respectively.
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun testPutSubjectDeleteZone() {

        this.zoneFactory.createTestZone(
            this.acsAdminRestTemplate!!, this.acsZone3Name!!,
            listOf(this.primaryZoneIssuerId)
        )

        val responseEntity: ResponseEntity<BaseSubject>
        try {
            this.privilegeHelper.putSubject(
                this.acsAdminRestTemplate!!, MARISSA_V1, this.acsUrl, this.zone3Headers!!,
                this.privilegeHelper.defaultAttribute
            )
        } catch (e: HttpClientErrorException) {
            Assert.fail("Unable to create subject.", e)
        }

        try {
            responseEntity = this.acsAdminRestTemplate!!
                .exchange(
                    this.acsUrl + ACS_SUBJECT_API_PATH + MARISSA_V1.subjectIdentifier,
                    HttpMethod.GET, HttpEntity<Any>(this.zone3Headers), BaseSubject::class.java
                )
            Assert.assertEquals(responseEntity.statusCode, HttpStatus.OK)
        } catch (e: HttpClientErrorException) {
            Assert.fail("Unable to get subject.", e)
        }

        try {
            this.zoneFactory.deleteZone(this.acsAdminRestTemplate!!, this.acsZone3Name!!)
            this.acsAdminRestTemplate!!
                .exchange(
                    this.acsUrl + ACS_SUBJECT_API_PATH + MARISSA_V1.subjectIdentifier,
                    HttpMethod.GET, HttpEntity<Any>(this.zone3Headers), BaseSubject::class.java
                )
            Assert.fail("Zone '" + this.acsZone3Name + "' was not properly deleted.")
        } catch (e: HttpServerErrorException) {
            // This following lines to be uncommented once TokenService returns the right exception instead of a
            // 500 - Defect url https://rally1.rallydev.com/#/30377833713d/detail/defect/42793900179
            // catch (OAuth2Exception e) {
            // Assert.assertTrue(e.getSummary().contains(HttpStatus.FORBIDDEN.toString()),
            // "Zone deletion did not produce the expected HTTP status code. Failed with: " + e);
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            Assert.fail("Failed with unexpected exception.", e)
        }
    }

    fun testPutSubjectMismatchURI() {
        try {
            val subjectIdentifier = "marcia"
            val subjectUri = URI.create(
                this.acsUrl + ACS_SUBJECT_API_PATH + URLEncoder
                    .encode(subjectIdentifier, "UTF-8")
            )
            this.acsAdminRestTemplate!!.put(subjectUri, HttpEntity(BOB_V1, this.zone1Headers))
            Assert.fail("Subject $subjectIdentifier was not supposed to be created")
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.UNPROCESSABLE_ENTITY)
            return
        } catch (e: Exception) {
            Assert.fail("Unable to create subject.")
        }

        Assert.fail("Expected Unprocessible Entity status code in testPutSubjectMismatchURIV1")
    }

    @Test(dataProvider = "invalidSubjectPostProvider")
    fun testPostSubjectNegativeCases(
        subject: BaseSubject,
        endpoint: String
    ) {
        try {

            this.privilegeHelper
                .postMultipleSubjects(this.acsAdminRestTemplate!!, endpoint, this.zone1Headers!!, subject)
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.UNPROCESSABLE_ENTITY)
            return
        } catch (e: Exception) {
            Assert.fail("Unable to create subject.")
        }

        Assert.fail("Expected Unprocessible Entity status code in testPostSubjectNegativeCases")
    }

    @Test(dataProvider = "subjectPostProvider")
    fun testPostSubjectPostiveCases(
        subject: BaseSubject,
        endpoint: String
    ) {
        try {
            val responseEntity = this.privilegeHelper
                .postMultipleSubjects(this.acsAdminRestTemplate!!, endpoint, this.zone1Headers!!, subject)
            Assert.assertEquals(responseEntity.statusCode, HttpStatus.NO_CONTENT)
        } catch (e: Exception) {
            Assert.fail("Unable to create subject.")
        }
    }

    @Test(dataProvider = "subjectPostProvider")
    fun testPostSubjectsUpdateAttributes(
        subject: BaseSubject,
        endpoint: String
    ) {
        // This test was added to test that the graph repo behaves transactionally.
        try {
            val subject2 = BaseSubject(BOB_V1.subjectIdentifier!!)
            subject2.attributes = HashSet(
                Arrays.asList(this.privilegeHelper.defaultAttribute)
            )
            subject.attributes = HashSet(
                Arrays.asList(this.privilegeHelper.defaultAttribute)
            )
            val responseEntity = this.privilegeHelper
                .postSubjects(this.acsAdminRestTemplate!!, endpoint, this.zone1Headers!!, subject, subject2)
            Assert.assertEquals(responseEntity.statusCode, HttpStatus.NO_CONTENT)
            subject2.attributes = HashSet(
                Arrays.asList(this.privilegeHelper.alternateAttribute)
            )
            subject.attributes = HashSet(
                Arrays.asList(this.privilegeHelper.alternateAttribute)
            )
            this.privilegeHelper
                .postSubjects(this.acsAdminRestTemplate!!, endpoint, this.zone1Headers!!, subject, subject2)
            var encodedSubjectIdentifier = URLEncoder.encode(subject.subjectIdentifier!!, "UTF-8")
            var uri = URI.create(this.acsUrl + ACS_SUBJECT_API_PATH + encodedSubjectIdentifier)
            var forEntity = this.acsAdminRestTemplate!!
                .exchange(uri, HttpMethod.GET, HttpEntity<Any>(this.zone1Headers), BaseSubject::class.java)
            Assert.assertTrue(
                forEntity.body.attributes!!.contains(this.privilegeHelper.alternateAttribute)
            )
            encodedSubjectIdentifier = URLEncoder.encode(subject2.subjectIdentifier!!, "UTF-8")
            uri = URI.create(this.acsUrl + ACS_SUBJECT_API_PATH + encodedSubjectIdentifier)
            forEntity = this.acsAdminRestTemplate!!
                .exchange(uri, HttpMethod.GET, HttpEntity<Any>(this.zone1Headers), BaseSubject::class.java)
            Assert.assertTrue(
                forEntity.body.attributes!!.contains(this.privilegeHelper.alternateAttribute)
            )

            encodedSubjectIdentifier = URLEncoder.encode(subject.subjectIdentifier!!, "UTF-8")
            uri = URI.create(this.acsUrl + ACS_SUBJECT_API_PATH + encodedSubjectIdentifier)
            forEntity = this.acsAdminRestTemplate!!
                .exchange(uri, HttpMethod.GET, HttpEntity<Any>(this.zone1Headers), BaseSubject::class.java)
            Assert.assertTrue(
                forEntity.body.attributes!!.contains(this.privilegeHelper.alternateAttribute)
            )
            encodedSubjectIdentifier = URLEncoder.encode(subject2.subjectIdentifier!!, "UTF-8")
            uri = URI.create(this.acsUrl + ACS_SUBJECT_API_PATH + encodedSubjectIdentifier)
            forEntity = this.acsAdminRestTemplate!!
                .exchange(uri, HttpMethod.GET, HttpEntity<Any>(this.zone1Headers), BaseSubject::class.java)
            Assert.assertTrue(
                forEntity.body.attributes!!.contains(this.privilegeHelper.alternateAttribute)
            )
        } catch (e: Exception) {
            Assert.fail("Unable to create subject.")
        }
    }

    @Test(dataProvider = "resourcePostProvider")
    fun testPostResourcePostiveCases(
        resource: BaseResource,
        endpoint: String
    ) {
        try {
            val responseEntity = this.privilegeHelper
                .postResources(this.acsAdminRestTemplate!!, endpoint, this.zone1Headers!!, resource)
            Assert.assertEquals(responseEntity.statusCode, HttpStatus.NO_CONTENT)
        } catch (e: Exception) {
            Assert.fail("Unable to create resource.")
        }
    }

    @Test(dataProvider = "invalidResourcePostProvider")
    fun testPostResourceNegativeCases(
        resource: BaseResource,
        endpoint: String
    ) {
        try {
            this.privilegeHelper.postResources(this.acsAdminRestTemplate!!, endpoint, this.zone1Headers!!, resource)
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.UNPROCESSABLE_ENTITY)
            return
        } catch (e: Exception) {
            Assert.fail("Unable to create resource.")
        }

        Assert.fail("Expected UnprocessibleEntity status code in testPostResourceNegativeCases")
    }

    @Throws(Exception::class)
    fun testPutGetDeleteResource() {
        try {
            this.privilegeHelper.putResource(
                this.acsAdminRestTemplate!!, SANRAMON, this.acsUrl!!, this.zone1Headers!!,
                this.privilegeHelper.defaultAttribute
            )
        } catch (e: Exception) {
            Assert.fail("Unable to create resource. " + e.message)
        }

        val resourceUri = URI.create(
            this.acsUrl + ACS_RESOURCE_API_PATH + URLEncoder
                .encode(SANRAMON.resourceIdentifier!!, "UTF-8")
        )
        try {
            this.acsAdminRestTemplate!!
                .exchange(resourceUri, HttpMethod.GET, HttpEntity<Any>(this.zone1Headers), BaseResource::class.java)
        } catch (e: HttpClientErrorException) {
            Assert.fail("Unable to get resource.")
        }

        try {
            this.acsAdminRestTemplate!!.exchange(
                resourceUri, HttpMethod.DELETE, HttpEntity<Any>(this.zone1Headers),
                ResponseEntity::class.java
            )
        } catch (e: HttpClientErrorException) {
            Assert.fail("Unable to delete resource.")
        }

        // properly delete
        try {
            this.acsAdminRestTemplate!!.exchange(
                resourceUri, HttpMethod.DELETE, HttpEntity<Any>(this.zone1Headers),
                ResponseEntity::class.java
            )
            this.acsAdminRestTemplate!!
                .exchange(resourceUri, HttpMethod.GET, HttpEntity<Any>(this.zone1Headers), BaseResource::class.java)
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.NOT_FOUND)
        }
    }

    @Throws(Exception::class)
    fun testUpdateResourceURIMismatch() {
        try {
            this.privilegeHelper.putResource(
                this.acsAdminRestTemplate!!, SANRAMON, this.acsUrl!!, this.zone1Headers!!,
                this.privilegeHelper.defaultAttribute
            )
            val resourceUri = URI.create(
                this.acsUrl + ACS_RESOURCE_API_PATH + URLEncoder
                    .encode("/different/resource", "UTF-8")
            )
            this.acsAdminRestTemplate!!.put(resourceUri, HttpEntity(SANRAMON, this.zone1Headers))
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.UNPROCESSABLE_ENTITY)
            return
        }

        Assert.fail("Expected Unprocessible Entity status code in testUpdateResourceURIMismatchV1")
    }

    @AfterMethod
    @Throws(Exception::class)
    fun cleanup() {
        this.privilegeHelper.deleteResources(this.acsAdminRestTemplate!!, this.acsUrl!!, this.zone1Headers!!)
        this.privilegeHelper.deleteSubjects(this.acsAdminRestTemplate!!, this.acsUrl!!, this.zone1Headers!!)
    }

    @AfterClass
    fun destroy() {
        this.acsitSetUpFactory.destroy()
    }
}
