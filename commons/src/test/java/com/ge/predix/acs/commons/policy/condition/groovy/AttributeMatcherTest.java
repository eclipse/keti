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

package com.ge.predix.acs.commons.policy.condition.groovy;

import static com.ge.predix.acs.commons.attribute.Attribute.attribute;
import static com.ge.predix.acs.commons.attribute.ScopeType.scope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.ge.predix.acs.commons.policy.condition.ConditionAssertionFailedException;
import com.ge.predix.acs.commons.policy.condition.ResourceHandler;
import com.ge.predix.acs.commons.policy.condition.SubjectHandler;
import com.ge.predix.acs.model.Attribute;

/**
 *
 * @author 212319607
 */
@SuppressWarnings({ "nls", "javadoc" })
public class AttributeMatcherTest {
    /**
     * match.any(resource.attributes("issuer", "site"), subject.attributes("issuer", "site")) match.any(resource,
     * subject).on("issuer", "name").
     */
    @Test
    public void testMatchSourceEmpty() {
        AttributeMatcher matcher = new AttributeMatcher();
        Set<String> resourceAttributes = new HashSet<>();
        resourceAttributes.add("San Ramon");
        resourceAttributes.add("Boston");
        Assert.assertFalse(matcher.any(new HashSet<String>(), resourceAttributes));
    }

    @Test
    public void testMatchSourceNull() {
        AttributeMatcher matcher = new AttributeMatcher();
        Set<String> resourceAttributes = new HashSet<>();
        resourceAttributes.add("San Ramon");
        resourceAttributes.add("Boston");
        Assert.assertFalse(matcher.any(null, resourceAttributes));
    }

    @Test
    public void testMatchTargetEmpty() {
        AttributeMatcher matcher = new AttributeMatcher();
        Set<String> resourceAttributes = new HashSet<>();
        resourceAttributes.add("San Ramon");
        resourceAttributes.add("Boston");
        Assert.assertFalse(matcher.any(resourceAttributes, new HashSet<String>()));
    }

    @Test
    public void testMatchTargetNull() {
        AttributeMatcher matcher = new AttributeMatcher();
        Set<String> resourceAttributes = new HashSet<>();
        resourceAttributes.add("San Ramon");
        resourceAttributes.add("Boston");
        Assert.assertFalse(matcher.any(resourceAttributes, null));
    }

    @Test
    public void testMatchSourceAndTargetEmpty() {
        AttributeMatcher matcher = new AttributeMatcher();
        Assert.assertFalse(matcher.any(new HashSet<String>(), new HashSet<String>()));
    }

    @Test
    public void testMatchOneMatch() {
        AttributeMatcher matcher = new AttributeMatcher();
        Set<String> subjectAttributes = new HashSet<>();
        subjectAttributes.add("San Ramon");
        subjectAttributes.add("New York");
        Set<String> resourceAttributes = new HashSet<>();
        resourceAttributes.add("San Ramon");
        resourceAttributes.add("Boston");
        Assert.assertTrue(matcher.any(subjectAttributes, resourceAttributes));
    }

    @Test
    public void testMultipleMatches() {
        AttributeMatcher matcher = new AttributeMatcher();
        Set<String> subjectAttributes = new HashSet<>();
        subjectAttributes.add("San Ramon");
        subjectAttributes.add("New York");
        subjectAttributes.add("ORD");
        Set<String> resourceAttributes = new HashSet<>();
        resourceAttributes.add("San Ramon");
        resourceAttributes.add("Boston");
        resourceAttributes.add("ORD");
        Assert.assertTrue(matcher.any(subjectAttributes, resourceAttributes));
    }

    @Test
    public void testNoMatches() {
        AttributeMatcher matcher = new AttributeMatcher();
        Set<String> subjectAttributes = new HashSet<>();
        subjectAttributes.add("San Ramon");
        subjectAttributes.add("New York");
        subjectAttributes.add("ORD");
        Set<String> resourceAttributes = new HashSet<>();
        resourceAttributes.add("Ramon");
        resourceAttributes.add("Boston");
        resourceAttributes.add("SanFrancisco");
        Assert.assertFalse(matcher.any(subjectAttributes, resourceAttributes));
    }

    /**
     * match.single(resource.attributes("issuer", "site"), "mysite")).
     */
    @Test
    public void testMatchSiteWithConstantValuePositive() {
        AttributeMatcher matcher = new AttributeMatcher();
        Set<String> subjectAttributes = new HashSet<>();
        subjectAttributes.add("San Ramon");
        subjectAttributes.add("New York");
        subjectAttributes.add("ORD");
        Assert.assertTrue(matcher.single(subjectAttributes, "New York"));
    }

    /**
     * match.single(resource.attributes("issuer", "site"), "mysite")).
     */
    @Test
    public void testMatchSiteWithConstantValueNegative() {
        AttributeMatcher matcher = new AttributeMatcher();
        Set<String> subjectAttributes = new HashSet<>();
        subjectAttributes.add("San Ramon");
        subjectAttributes.add("New York");
        subjectAttributes.add("ORD");
        Assert.assertFalse(matcher.single(subjectAttributes, "LA"));
    }

    @Test
    public void testMatchSubjectAttributeWithScope() throws ConditionAssertionFailedException {

        Attribute acsGroup = new Attribute("https://acs.attributes.int", "group", "acs");
        List<Attribute> scopes = new ArrayList<Attribute>(Arrays.asList(acsGroup));

        AttributeMatcher request = new AttributeMatcher();
        Set<Attribute> subjectAttributeSet = new HashSet<Attribute>();

        Attribute roleAttr = new Attribute("https://acs.attributes.int", "role", "analyst", scopes);
        subjectAttributeSet.add(roleAttr);
        SubjectHandler subject = new SubjectHandler(subjectAttributeSet);
        request.setSubjectHandler(subject);

        Set<Attribute> resourceAttributeSet = new HashSet<Attribute>();
        resourceAttributeSet.add(acsGroup);
        String resourceURI = "/asset/1234";
        String resourceURLTemplate = "/asset/{asset_id}";
        ResourceHandler resource = new ResourceHandler(resourceAttributeSet, resourceURI, resourceURLTemplate);
        request.setResourceHandler(resource);

        request.subject().has(attribute("https://acs.attributes.int", "role", "analyst")
                .forAny(scope(request.resource(), "https://acs.attributes.int", "group")));

        // Abbreviated form.
        request.subject().has(attribute("https://acs.attributes.int", "role", "analyst").forAny(request.resource(),
                "https://acs.attributes.int", "group"));

        subject.has(attribute("https://acs.attributes.int", "role", "analyst").forAny(resource,
                "https://acs.attributes.int", "group"));
    }

    @Test
    public void testMatchSubjectAttributeWithScopeWithMultipleScopes() throws ConditionAssertionFailedException {

        Attribute acsGroup = new Attribute("https://acs.attributes.int", "group", "acs");
        List<Attribute> scopes = new ArrayList<Attribute>(Arrays.asList(acsGroup));

        AttributeMatcher request = new AttributeMatcher();
        Set<Attribute> subjectAttributeSet = new HashSet<Attribute>();

        Attribute roleAttr = new Attribute("https://acs.attributes.int", "role", "analyst", scopes);
        subjectAttributeSet.add(roleAttr);
        SubjectHandler subject = new SubjectHandler(subjectAttributeSet);
        request.setSubjectHandler(subject);

        Set<Attribute> resourceAttributeSet = new HashSet<Attribute>();
        resourceAttributeSet.add(new Attribute("https://acs.attributes.int", "group", "uaa"));
        resourceAttributeSet.add(acsGroup);
        resourceAttributeSet.add(new Attribute("https://acs.attributes.int", "group", "abs"));
        String resourceURI = "/asset/1234";
        String resourceURLTemplate = "/asset/{asset_id}";
        ResourceHandler resource = new ResourceHandler(resourceAttributeSet, resourceURI, resourceURLTemplate);
        request.setResourceHandler(resource);

        subject.has(attribute("https://acs.attributes.int", "role", "analyst").forAny(resource,
                "https://acs.attributes.int", "group"));
    }

    /**
     * The condition is looking for a scoped subject attribute but the subject attribute is not scoped.
     *
     * @throws ConditionAssertionFailedException
     */
    @Test(expectedExceptions = { ConditionAssertionFailedException.class })
    public void testMatchSubjectAttributeWithScopeWithSubjectAttributeNotScoped()
            throws ConditionAssertionFailedException {

        Attribute acsGroup = new Attribute("https://acs.attributes.int", "group", "acs");

        AttributeMatcher request = new AttributeMatcher();
        Set<Attribute> subjectAttributeSet = new HashSet<Attribute>();

        // Create role attr with no scopes, assign to subject
        Attribute roleAttr = new Attribute("https://acs.attributes.int", "role", "analyst");
        subjectAttributeSet.add(roleAttr);
        SubjectHandler subjectHandler = new SubjectHandler(subjectAttributeSet);
        request.setSubjectHandler(subjectHandler);

        Set<Attribute> resourceAttributeSet = new HashSet<Attribute>();
        resourceAttributeSet.add(acsGroup);
        String resourceURI = "/asset/1234";
        String resourceURLTemplate = "/asset/{asset_id}";
        ResourceHandler resourceHandler = new ResourceHandler(resourceAttributeSet, resourceURI, resourceURLTemplate);
        request.setResourceHandler(resourceHandler);

        try {
            request.subject().has(attribute("https://acs.attributes.int", "role", "analyst").forAny(request.resource(),
                    "https://acs.attributes.int", "group"));
        } catch (ConditionAssertionFailedException e) {
            Assert.assertEquals(e.getMessage(),
                    "Subject Attribute [type=AttributeType [issuer=https://acs.attributes.int, name=role],"
                            + " value=analyst] is not scoped.");
            throw e;
        }
    }

    /**
     * The condition is looking for a scope that is not the same as the subject attribute's scope.
     */
    @Test(expectedExceptions = { ConditionAssertionFailedException.class })
    public void testMatchSubjectAttributeWithScopeWithScopeCriteriaMismatch() throws ConditionAssertionFailedException {

        Attribute acsGroup = new Attribute("https://acs.attributes.int", "group", "acs");
        List<Attribute> scopes = new ArrayList<Attribute>(Arrays.asList(acsGroup));

        AttributeMatcher request = new AttributeMatcher();
        Set<Attribute> subjectAttributeSet = new HashSet<Attribute>();

        Attribute roleAttr = new Attribute("https://acs.attributes.int", "role", "analyst", scopes);
        subjectAttributeSet.add(roleAttr);
        SubjectHandler subjectHandler = new SubjectHandler(subjectAttributeSet);
        request.setSubjectHandler(subjectHandler);

        Set<Attribute> resourceAttributeSet = new HashSet<Attribute>();
        resourceAttributeSet.add(acsGroup);
        String resourceURI = "/asset/1234";
        String resourceURLTemplate = "/asset/{asset_id}";
        ResourceHandler resourceHandler = new ResourceHandler(resourceAttributeSet, resourceURI, resourceURLTemplate);
        request.setResourceHandler(resourceHandler);

        try {
            request.subject().has(attribute("https://acs.attributes.int", "role", "analyst").forAny(request.resource(),
                    "https://acs.attributes.int", "site"));
        } catch (ConditionAssertionFailedException e) {
            System.out.println(e);
            Assert.assertEquals(e.getMessage(),
                    "Subject Attribute [type=AttributeType "
                            + "[issuer=https://acs.attributes.int, name=role], value=analyst] does not have "
                            + "ScopeType [resourceHandler=Resource, attributeType=AttributeType "
                            + "[issuer=https://acs.attributes.int, name=site]].");
            throw e;
        }
    }

    /**
     * The condition is looking for a scope that is not in the resource.
     *
     * @throws ConditionAssertionFailedException
     */
    @Test(expectedExceptions = { ConditionAssertionFailedException.class })
    public void testMatchSubjectAttributeWithScopeWithScopeNotInResource() throws ConditionAssertionFailedException {

        Attribute acsGroup = new Attribute("https://acs.attributes.int", "group", "acs");
        List<Attribute> scopes = new ArrayList<Attribute>(Arrays.asList(acsGroup));

        AttributeMatcher request = new AttributeMatcher();
        Set<Attribute> subjectAttributeSet = new HashSet<Attribute>();

        Attribute roleAttr = new Attribute("https://acs.attributes.int", "role", "analyst", scopes);
        subjectAttributeSet.add(roleAttr);
        SubjectHandler subjectHandler = new SubjectHandler(subjectAttributeSet);
        request.setSubjectHandler(subjectHandler);

        String resourceURI = "/asset/1234";
        String resourceURLTemplate = "/asset/{asset_id}";
        // Create resource with no attributes
        ResourceHandler resourceHandler = new ResourceHandler(new HashSet<Attribute>(), resourceURI,
                resourceURLTemplate);
        request.setResourceHandler(resourceHandler);

        try {
            request.subject().has(attribute("https://acs.attributes.int", "role", "analyst").forAny(request.resource(),
                    "https://acs.attributes.int", "group"));
        } catch (ConditionAssertionFailedException e) {
            Assert.assertEquals(e.getMessage(),
                    "Failed to match ScopedAttributeCriteria "
                            + "[attribute=Attribute [type=AttributeType [issuer=https://acs.attributes.int, name=role],"
                            + " value=analyst], scopeType=ScopeType [resourceHandler=Resource,"
                            + " attributeType=AttributeType [issuer=https://acs.attributes.int, name=group]]].");
            throw e;
        }
    }

    @Test
    public void testMatchSubjectHasAttribute() throws ConditionAssertionFailedException {

        AttributeMatcher request = new AttributeMatcher();
        Set<Attribute> subjectAttributeSet = new HashSet<Attribute>();

        Attribute roleAttr = new Attribute("https://acs.attributes.int", "role", "analyst");
        subjectAttributeSet.add(roleAttr);
        SubjectHandler subjectHandler = new SubjectHandler(subjectAttributeSet);
        request.setSubjectHandler(subjectHandler);

        String resourceURI = "/asset/1234";
        String resourceURLTemplate = "/asset/{asset_id}";
        ResourceHandler resourceHandler = new ResourceHandler(new HashSet<Attribute>(), resourceURI,
                resourceURLTemplate);
        request.setResourceHandler(resourceHandler);

        request.subject().has(attribute("https://acs.attributes.int", "role", "analyst"));
    }

    @Test(expectedExceptions = { ConditionAssertionFailedException.class })
    public void testMatchSubjectHasAttributeWhenItDoesnt() throws ConditionAssertionFailedException {

        AttributeMatcher request = new AttributeMatcher();
        Set<Attribute> subjectAttributeSet = new HashSet<Attribute>();

        Attribute roleAttr = new Attribute("https://acs.attributes.int", "role", "analyst");
        subjectAttributeSet.add(roleAttr);
        SubjectHandler subjectHandler = new SubjectHandler(subjectAttributeSet);
        request.setSubjectHandler(subjectHandler);

        String resourceURI = "/asset/1234";
        String resourceURLTemplate = "/asset/{asset_id}";
        ResourceHandler resourceHandler = new ResourceHandler(new HashSet<Attribute>(), resourceURI,
                resourceURLTemplate);
        request.setResourceHandler(resourceHandler);

        try {
            request.subject().has(attribute("https://acs.attributes.int", "role", "guardian"));
        } catch (final ConditionAssertionFailedException e) {
            Assert.assertEquals(e.getMessage(),
                    "Subject does not have Attribute [type=AttributeType [issuer=https://acs.attributes.int,"
                            + " name=role], value=guardian].");
            throw e;
        }
    }

    @Test
    public void testMatchSubjectAndResourceHaveSame() throws ConditionAssertionFailedException {

        AttributeMatcher request = new AttributeMatcher();

        Attribute groupAttr = new Attribute("https://acs.attributes.int", "group", "acs");
        Set<Attribute> subjectAttributeSet = new HashSet<Attribute>(Arrays.asList(new Attribute[] { groupAttr }));

        SubjectHandler subject = new SubjectHandler(subjectAttributeSet);
        request.setSubjectHandler(subject);

        Set<Attribute> resourceAttributeSet = new HashSet<Attribute>(Arrays.asList(new Attribute[] { groupAttr }));
        String resourceURI = "/asset/1234";
        String resourceURLTemplate = "/asset/{asset_id}";
        ResourceHandler resource = new ResourceHandler(resourceAttributeSet, resourceURI, resourceURLTemplate);
        request.setResourceHandler(resource);

        request.subject().and(request.resource()).haveSame("https://acs.attributes.int", "group");

        subject.and(resource).haveSame("https://acs.attributes.int", "group");
    }

    @Test(expectedExceptions = { ConditionAssertionFailedException.class })
    public void testMatchSubjectAndResourceHaveSameWhenTheyDont() throws ConditionAssertionFailedException {

        AttributeMatcher request = new AttributeMatcher();

        Attribute groupAttr = new Attribute("https://acs.attributes.int", "group", "acs");
        Set<Attribute> subjectAttributeSet = new HashSet<Attribute>(Arrays.asList(new Attribute[] { groupAttr }));

        SubjectHandler subject = new SubjectHandler(subjectAttributeSet);
        request.setSubjectHandler(subject);

        Attribute groupAttr2 = new Attribute("https://acs.attributes.int", "group", "uaa");
        Set<Attribute> resourceAttributeSet = new HashSet<Attribute>(Arrays.asList(new Attribute[] { groupAttr2 }));
        String resourceURI = "/asset/1234";
        String resourceURLTemplate = "/asset/{asset_id}";
        ResourceHandler resource = new ResourceHandler(resourceAttributeSet, resourceURI, resourceURLTemplate);
        request.setResourceHandler(resource);

        try {
            subject.and(resource).haveSame("https://acs.attributes.int", "group");
        } catch (final ConditionAssertionFailedException e) {
            Assert.assertEquals(e.getMessage(), "No intersection exists between Subject and Resource on "
                    + "AttributeType [issuer=https://acs.attributes.int, name=group].");
            throw e;
        }
    }

    @Test(expectedExceptions = { ConditionAssertionFailedException.class })
    public void testMatchSubjectAndResourceHaveSameWhenAttributeMissing() throws ConditionAssertionFailedException {

        AttributeMatcher request = new AttributeMatcher();

        Set<Attribute> subjectAttributeSet = new HashSet<Attribute>(Arrays.asList(new Attribute[] {}));

        SubjectHandler subject = new SubjectHandler(subjectAttributeSet);
        request.setSubjectHandler(subject);

        Set<Attribute> resourceAttributeSet = new HashSet<Attribute>(Arrays.asList(new Attribute[] {}));
        String resourceURI = "/asset/1234";
        String resourceURLTemplate = "/asset/{asset_id}";
        ResourceHandler resource = new ResourceHandler(resourceAttributeSet, resourceURI, resourceURLTemplate);
        request.setResourceHandler(resource);

        try {
            subject.and(resource).haveSame("https://acs.attributes.int", "group");
        } catch (final ConditionAssertionFailedException e) {
            Assert.assertEquals(e.getMessage(),
                    "Subject does not have AttributeType [issuer=https://acs.attributes.int, name=group].");
            throw e;
        }
    }
}
