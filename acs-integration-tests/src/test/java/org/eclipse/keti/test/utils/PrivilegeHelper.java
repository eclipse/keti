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

package org.eclipse.keti.test.utils;

import static org.eclipse.keti.test.utils.ACSTestUtil.ACS_VERSION;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.eclipse.keti.acs.model.Attribute;
import org.eclipse.keti.acs.rest.BaseResource;
import org.eclipse.keti.acs.rest.BaseSubject;

@Component
public class PrivilegeHelper {

    private static final String ALTERNATE_ATTRIBUTE_VALUE = "sanfrancisco";
    private static final String DEFAULT_ATTRIBUTE_VALUE = "sanramon";
    private static final String DEFAULT_ATTRIBUTE_NAME = "site";

    private static final String DEFAULT_ORG_ATTRIBUTE_NAME = "org";
    private static final String DEFAULT_ORG_ATTRIBUTE_VALUE = "alliance";
    private static final String ALTERNATE_ORG_ATTRIBUTE_VALUE = "syndicate";

    public static final String ACS_SUBJECT_API_PATH = ACS_VERSION + "/subject/";
    public static final String ACS_RESOURCE_API_PATH = ACS_VERSION + "/resource/";

    public static final String DEFAULT_SUBJECT_ID = "any";
    public static final String DEFAULT_SUBJECT_IDENTIFIER = "any";
    public static final String DEFAULT_RESOURCE_IDENTIFIER = "any";
    public static final String DEFAULT_ATTRIBUTE_ISSUER = "https://acs.attributes.int";

    @Autowired
    private ZoneFactory zoneFactory;

    public BaseSubject putSubject(final OAuth2RestTemplate acsTemplate, final BaseSubject subject,
            final String endpoint, final HttpHeaders headers, final Attribute... attributes)
            throws UnsupportedEncodingException {

        subject.setAttributes(new HashSet<>(Arrays.asList(attributes)));
        URI subjectUri = URI
                .create(endpoint + ACS_SUBJECT_API_PATH + URLEncoder.encode(subject.getSubjectIdentifier(), "UTF-8"));
        acsTemplate.put(subjectUri, new HttpEntity<>(subject, headers));
        return subject;
    }

    public void putSubject(final OAuth2RestTemplate restTemplate, final String subjectIdentifier,
            final HttpHeaders headers, final Attribute... attributes) throws UnsupportedEncodingException {

        BaseSubject subject = new BaseSubject(subjectIdentifier);
        // no header needed, because it uses zone specific url
        putSubject(restTemplate, subject, this.zoneFactory.getAcsBaseURL(), headers, attributes);
    }

    public ResponseEntity<Object> postMultipleSubjects(final OAuth2RestTemplate acsTemplate, final String endpoint,
            final HttpHeaders headers, final BaseSubject... subjects) {
        Attribute site = getDefaultAttribute();
        Set<Attribute> attributes = new HashSet<>();
        attributes.add(site);

        List<BaseSubject> subjectsArray = new ArrayList<>();
        for (BaseSubject s : subjects) {
            s.setAttributes(attributes);
            subjectsArray.add(s);
        }
        URI subjectUri = URI.create(endpoint + ACS_SUBJECT_API_PATH);

        ResponseEntity<Object> responseEntity = acsTemplate.postForEntity(subjectUri,
                new HttpEntity<>(subjectsArray, headers), Object.class);

        return responseEntity;
    }

    public ResponseEntity<Object> postSubjectsWithDefaultAttributes(final OAuth2RestTemplate acsTemplate,
            final String endpoint, final HttpHeaders headers, final BaseSubject... subjects) {
        Attribute site = getDefaultAttribute();
        Set<Attribute> attributes = new HashSet<>();
        attributes.add(site);

        for (BaseSubject s : subjects) {
            s.setAttributes(attributes);
        }
        return postSubjects(acsTemplate, endpoint, headers, subjects);
    }

    public ResponseEntity<Object> postSubjects(final OAuth2RestTemplate acsTemplate, final String endpoint,
            final HttpHeaders headers, final BaseSubject... subjects) {
        List<BaseSubject> subjectsArray = new ArrayList<>();
        for (BaseSubject s : subjects) {
            subjectsArray.add(s);
        }
        URI subjectUri = URI.create(endpoint + ACS_SUBJECT_API_PATH);
        ResponseEntity<Object> responseEntity = acsTemplate.postForEntity(subjectUri,
                new HttpEntity<>(subjectsArray, headers), Object.class);
        return responseEntity;
    }

    public BaseResource putResource(final OAuth2RestTemplate acsTemplate, final BaseResource resource,
            final String endpoint, final HttpHeaders headers, final Attribute... attributes) throws Exception {

        resource.setAttributes(new HashSet<>(Arrays.asList(attributes)));

        String value = URLEncoder.encode(resource.getResourceIdentifier(), "UTF-8");

        URI uri = new URI(endpoint + ACS_RESOURCE_API_PATH + value);
        acsTemplate.put(uri, new HttpEntity<>(resource, headers));
        return resource;
    }

    public ResponseEntity<Object> postResources(final OAuth2RestTemplate acsTemplate, final String endpoint,
            final BaseResource... resources) {

        Attribute site = getDefaultAttribute();
        Set<Attribute> attributes = new HashSet<>();
        attributes.add(site);

        List<BaseResource> resourcesArray = new ArrayList<>();
        for (BaseResource r : resources) {
            r.setAttributes(attributes);
            resourcesArray.add(r);
        }
        URI resourceUri = URI.create(endpoint + ACS_RESOURCE_API_PATH);

        ResponseEntity<Object> responseEntity = acsTemplate.postForEntity(resourceUri, resourcesArray, Object.class);

        return responseEntity;
    }

    public ResponseEntity<Object> postResourcesWithDefaultSiteAttribute(final OAuth2RestTemplate acsTemplate,
            final String endpoint, final HttpHeaders headers, final BaseResource... resources) {

        Attribute site = getDefaultAttribute();
        Set<Attribute> attributes = new HashSet<>();
        attributes.add(site);

        for (BaseResource r : resources) {
            r.setAttributes(attributes);
        }
        return postResources(acsTemplate, endpoint, headers, resources);
    }

    public ResponseEntity<Object> postResources(final OAuth2RestTemplate acsTemplate, final String endpoint,
            final HttpHeaders headers, final BaseResource... resources) {

        List<BaseResource> resourcesArray = new ArrayList<>();
        for (BaseResource r : resources) {
            resourcesArray.add(r);
        }
        URI resourceUri = URI.create(endpoint + ACS_RESOURCE_API_PATH);

        ResponseEntity<Object> responseEntity = acsTemplate.postForEntity(resourceUri,
                new HttpEntity<>(resourcesArray, headers), Object.class);

        return responseEntity;
    }

    public void deleteSubject(final RestTemplate restTemplate, final String acsUrl, final String subjectId)
            throws Exception {
        deleteSubject(restTemplate, acsUrl, subjectId, null);
    }

    public void deleteSubject(final RestTemplate restTemplate, final String acsUrl, final String subjectId,
            final HttpHeaders headers) throws Exception {
        if (subjectId != null) {
            URI subjectUri = URI.create(acsUrl + ACS_SUBJECT_API_PATH + URLEncoder.encode(subjectId, "UTF-8"));
            restTemplate.exchange(subjectUri, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        }
    }

    public void deleteResource(final RestTemplate restTemplate, final String acsUrl, final String resourceId)
            throws Exception {
        deleteResource(restTemplate, acsUrl, resourceId, null);
    }

    public void deleteResource(final RestTemplate restTemplate, final String acsUrl, final String resourceId,
            final HttpHeaders headers) throws Exception {
        if (resourceId != null) {
            URI resourceUri = URI.create(acsUrl + ACS_RESOURCE_API_PATH + URLEncoder.encode(resourceId, "UTF-8"));
            restTemplate.exchange(resourceUri, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        }
    }

    public BaseSubject createSubject(final String subjectIdentifier) {
        BaseSubject subject = new BaseSubject();
        subject.setSubjectIdentifier(subjectIdentifier);
        Set<Attribute> attributes = new HashSet<>();
        attributes.add(getDefaultAttribute());
        subject.setAttributes(attributes);
        return subject;
    }

    public BaseResource createResource(final String resourceIdentifier) {
        BaseResource resource = new BaseResource();
        resource.setResourceIdentifier(resourceIdentifier);
        Set<Attribute> attributes = new HashSet<>();
        attributes.add(getDefaultAttribute());
        resource.setAttributes(attributes);
        return resource;
    }

    public Attribute getDefaultAttribute() {
        Attribute site = new Attribute();
        site.setIssuer(DEFAULT_ATTRIBUTE_ISSUER);
        site.setName(DEFAULT_ATTRIBUTE_NAME);
        site.setValue(DEFAULT_ATTRIBUTE_VALUE);
        return site;
    }

    public Attribute getAlternateAttribute() {
        Attribute site = new Attribute();
        site.setIssuer(DEFAULT_ATTRIBUTE_ISSUER);
        site.setName(DEFAULT_ATTRIBUTE_NAME);
        site.setValue(ALTERNATE_ATTRIBUTE_VALUE);
        return site;
    }

    public Attribute getDefaultOrgAttribute() {
        Attribute site = new Attribute();
        site.setIssuer(DEFAULT_ATTRIBUTE_ISSUER);
        site.setName(DEFAULT_ORG_ATTRIBUTE_NAME);
        site.setValue(DEFAULT_ORG_ATTRIBUTE_VALUE);
        return site;
    }

    public Attribute getAlternateOrgAttribute() {
        Attribute site = new Attribute();
        site.setIssuer(DEFAULT_ATTRIBUTE_ISSUER);
        site.setName(DEFAULT_ORG_ATTRIBUTE_NAME);
        site.setValue(ALTERNATE_ORG_ATTRIBUTE_VALUE);
        return site;
    }

    public void deleteSubjects(final RestTemplate restTemplate, final String acsUrl, final HttpHeaders headers)
            throws Exception {
        BaseSubject[] subjects = listSubjects(restTemplate, acsUrl, headers);
        for (BaseSubject subject : subjects) {
            deleteSubject(restTemplate, acsUrl, subject.getSubjectIdentifier(), headers);
        }
    }

    public BaseSubject[] listSubjects(final RestTemplate restTemplate, final String acsUrl, final HttpHeaders headers) {
        URI uri = URI.create(acsUrl + ACS_SUBJECT_API_PATH);
        ResponseEntity<BaseSubject[]> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers),
                BaseSubject[].class);
        return response.getBody();
    }

    public void deleteResources(final RestTemplate restTemplate, final String acsUrl, final HttpHeaders headers)
            throws Exception {
        BaseResource[] resources = listResources(restTemplate, acsUrl, headers);
        for (BaseResource resource : resources) {
            deleteResource(restTemplate, acsUrl, resource.getResourceIdentifier(), headers);
        }
    }

    public BaseResource[] listResources(final RestTemplate restTemplate, final String acsEndpoint,
            final HttpHeaders headers) {
        URI uri = URI.create(acsEndpoint + ACS_RESOURCE_API_PATH);
        ResponseEntity<BaseResource[]> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers),
                BaseResource[].class);
        return response.getBody();
    }
}
