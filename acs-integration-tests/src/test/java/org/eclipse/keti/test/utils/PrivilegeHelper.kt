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

package org.eclipse.keti.test.utils

import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URLEncoder
import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet

private const val ALTERNATE_ATTRIBUTE_VALUE = "sanfrancisco"
private const val DEFAULT_ATTRIBUTE_VALUE = "sanramon"
private const val DEFAULT_ATTRIBUTE_NAME = "site"
private const val DEFAULT_ORG_ATTRIBUTE_NAME = "org"
private const val DEFAULT_ORG_ATTRIBUTE_VALUE = "alliance"
private const val ALTERNATE_ORG_ATTRIBUTE_VALUE = "syndicate"

const val ACS_SUBJECT_API_PATH = "$ACS_VERSION/subject/"
const val ACS_RESOURCE_API_PATH = "$ACS_VERSION/resource/"
const val DEFAULT_SUBJECT_ID = "any"
const val DEFAULT_SUBJECT_IDENTIFIER = "any"
const val DEFAULT_RESOURCE_IDENTIFIER = "any"
const val DEFAULT_ATTRIBUTE_ISSUER = "https://acs.attributes.int"

@Component
class PrivilegeHelper {

    @Autowired
    private lateinit var zoneFactory: ZoneFactory

    val defaultAttribute: Attribute
        get() {
            val site = Attribute()
            site.issuer = DEFAULT_ATTRIBUTE_ISSUER
            site.name = DEFAULT_ATTRIBUTE_NAME
            site.value = DEFAULT_ATTRIBUTE_VALUE
            return site
        }

    val alternateAttribute: Attribute
        get() {
            val site = Attribute()
            site.issuer = DEFAULT_ATTRIBUTE_ISSUER
            site.name = DEFAULT_ATTRIBUTE_NAME
            site.value = ALTERNATE_ATTRIBUTE_VALUE
            return site
        }

    val defaultOrgAttribute: Attribute
        get() {
            val site = Attribute()
            site.issuer = DEFAULT_ATTRIBUTE_ISSUER
            site.name = DEFAULT_ORG_ATTRIBUTE_NAME
            site.value = DEFAULT_ORG_ATTRIBUTE_VALUE
            return site
        }

    val alternateOrgAttribute: Attribute
        get() {
            val site = Attribute()
            site.issuer = DEFAULT_ATTRIBUTE_ISSUER
            site.name = DEFAULT_ORG_ATTRIBUTE_NAME
            site.value = ALTERNATE_ORG_ATTRIBUTE_VALUE
            return site
        }

    @Throws(UnsupportedEncodingException::class)
    fun putSubject(
        acsTemplate: OAuth2RestTemplate,
        subject: BaseSubject,
        endpoint: String?,
        headers: HttpHeaders,
        vararg attributes: Attribute
    ): BaseSubject {

        subject.attributes = HashSet(Arrays.asList(*attributes))
        val subjectUri = URI
            .create(endpoint + ACS_SUBJECT_API_PATH + URLEncoder.encode(subject.subjectIdentifier!!, "UTF-8"))
        acsTemplate.put(subjectUri, HttpEntity(subject, headers))
        return subject
    }

    @Throws(UnsupportedEncodingException::class)
    fun putSubject(
        restTemplate: OAuth2RestTemplate,
        subjectIdentifier: String,
        headers: HttpHeaders,
        vararg attributes: Attribute
    ) {

        val subject = BaseSubject(subjectIdentifier)
        // no header needed, because it uses zone specific url
        putSubject(restTemplate, subject, this.zoneFactory.acsBaseURL, headers, *attributes)
    }

    fun postMultipleSubjects(
        acsTemplate: OAuth2RestTemplate,
        endpoint: String,
        headers: HttpHeaders,
        vararg subjects: BaseSubject
    ): ResponseEntity<Any> {
        val site = defaultAttribute
        val attributes = HashSet<Attribute>()
        attributes.add(site)

        val subjectsArray = ArrayList<BaseSubject>()
        for (s in subjects) {
            s.attributes = attributes
            subjectsArray.add(s)
        }
        val subjectUri = URI.create(endpoint + ACS_SUBJECT_API_PATH)

        return acsTemplate.postForEntity(
            subjectUri,
            HttpEntity<List<BaseSubject>>(subjectsArray, headers), Any::class.java
        )
    }

    fun postSubjectsWithDefaultAttributes(
        acsTemplate: OAuth2RestTemplate,
        endpoint: String,
        headers: HttpHeaders,
        vararg subjects: BaseSubject
    ): ResponseEntity<Any> {
        val site = defaultAttribute
        val attributes = HashSet<Attribute>()
        attributes.add(site)

        for (s in subjects) {
            s.attributes = attributes
        }
        return postSubjects(acsTemplate, endpoint, headers, *subjects)
    }

    fun postSubjects(
        acsTemplate: OAuth2RestTemplate,
        endpoint: String,
        headers: HttpHeaders,
        vararg subjects: BaseSubject
    ): ResponseEntity<Any> {
        val subjectsArray = ArrayList<BaseSubject>()
        for (s in subjects) {
            subjectsArray.add(s)
        }
        val subjectUri = URI.create(endpoint + ACS_SUBJECT_API_PATH)
        return acsTemplate.postForEntity(
            subjectUri,
            HttpEntity<List<BaseSubject>>(subjectsArray, headers), Any::class.java
        )
    }

    @Throws(Exception::class)
    fun putResource(
        acsTemplate: OAuth2RestTemplate,
        resource: BaseResource,
        endpoint: String,
        headers: HttpHeaders,
        vararg attributes: Attribute
    ): BaseResource {

        resource.attributes = HashSet(Arrays.asList(*attributes))

        val value = URLEncoder.encode(resource.resourceIdentifier!!, "UTF-8")

        val uri = URI(endpoint + ACS_RESOURCE_API_PATH + value)
        acsTemplate.put(uri, HttpEntity(resource, headers))
        return resource
    }

    fun postResources(
        acsTemplate: OAuth2RestTemplate,
        endpoint: String,
        vararg resources: BaseResource
    ): ResponseEntity<Any> {

        val site = defaultAttribute
        val attributes = HashSet<Attribute>()
        attributes.add(site)

        val resourcesArray = ArrayList<BaseResource>()
        for (r in resources) {
            r.attributes = attributes
            resourcesArray.add(r)
        }
        val resourceUri = URI.create(endpoint + ACS_RESOURCE_API_PATH)

        return acsTemplate.postForEntity(resourceUri, resourcesArray, Any::class.java)
    }

    fun postResourcesWithDefaultSiteAttribute(
        acsTemplate: OAuth2RestTemplate,
        endpoint: String,
        headers: HttpHeaders,
        vararg resources: BaseResource
    ): ResponseEntity<Any> {

        val site = defaultAttribute
        val attributes = HashSet<Attribute>()
        attributes.add(site)

        for (r in resources) {
            r.attributes = attributes
        }
        return postResources(acsTemplate, endpoint, headers, *resources)
    }

    fun postResources(
        acsTemplate: OAuth2RestTemplate,
        endpoint: String,
        headers: HttpHeaders,
        vararg resources: BaseResource
    ): ResponseEntity<Any> {

        val resourcesArray = ArrayList<BaseResource>()
        for (r in resources) {
            resourcesArray.add(r)
        }
        val resourceUri = URI.create(endpoint + ACS_RESOURCE_API_PATH)

        return acsTemplate.postForEntity(
            resourceUri,
            HttpEntity<List<BaseResource>>(resourcesArray, headers), Any::class.java
        )
    }

    @Throws(Exception::class)
    @JvmOverloads
    fun deleteSubject(
        restTemplate: RestTemplate,
        acsUrl: String,
        subjectId: String?,
        headers: HttpHeaders? = null
    ) {
        if (subjectId != null) {
            val subjectUri = URI.create(acsUrl + ACS_SUBJECT_API_PATH + URLEncoder.encode(subjectId, "UTF-8"))
            restTemplate.exchange(subjectUri, HttpMethod.DELETE, HttpEntity<Any>(headers), String::class.java)
        }
    }

    @Throws(Exception::class)
    @JvmOverloads
    fun deleteResource(
        restTemplate: RestTemplate,
        acsUrl: String,
        resourceId: String?,
        headers: HttpHeaders? = null
    ) {
        if (resourceId != null) {
            val resourceUri = URI.create(acsUrl + ACS_RESOURCE_API_PATH + URLEncoder.encode(resourceId, "UTF-8"))
            restTemplate.exchange(resourceUri, HttpMethod.DELETE, HttpEntity<Any>(headers), String::class.java)
        }
    }

    fun createSubject(subjectIdentifier: String): BaseSubject {
        val subject = BaseSubject()
        subject.subjectIdentifier = subjectIdentifier
        val attributes = HashSet<Attribute>()
        attributes.add(defaultAttribute)
        subject.attributes = attributes
        return subject
    }

    fun createResource(resourceIdentifier: String): BaseResource {
        val resource = BaseResource()
        resource.resourceIdentifier = resourceIdentifier
        val attributes = HashSet<Attribute>()
        attributes.add(defaultAttribute)
        resource.attributes = attributes
        return resource
    }

    @Throws(Exception::class)
    fun deleteSubjects(
        restTemplate: RestTemplate,
        acsUrl: String,
        headers: HttpHeaders
    ) {
        val subjects = listSubjects(restTemplate, acsUrl, headers)
        for (subject in subjects) {
            deleteSubject(restTemplate, acsUrl, subject.subjectIdentifier, headers)
        }
    }

    fun listSubjects(
        restTemplate: RestTemplate,
        acsUrl: String,
        headers: HttpHeaders
    ): Array<BaseSubject> {
        val uri = URI.create(acsUrl + ACS_SUBJECT_API_PATH)
        val response = restTemplate.exchange(
            uri, HttpMethod.GET, HttpEntity<Any>(headers),
            Array<BaseSubject>::class.java
        )
        return response.body
    }

    @Throws(Exception::class)
    fun deleteResources(
        restTemplate: RestTemplate,
        acsUrl: String,
        headers: HttpHeaders
    ) {
        val resources = listResources(restTemplate, acsUrl, headers)
        for (resource in resources) {
            deleteResource(restTemplate, acsUrl, resource.resourceIdentifier, headers)
        }
    }

    fun listResources(
        restTemplate: RestTemplate,
        acsEndpoint: String,
        headers: HttpHeaders
    ): Array<BaseResource> {
        val uri = URI.create(acsEndpoint + ACS_RESOURCE_API_PATH)
        val response = restTemplate.exchange(
            uri, HttpMethod.GET, HttpEntity<Any>(headers),
            Array<BaseResource>::class.java
        )
        return response.body
    }
}
