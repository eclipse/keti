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

package com.ge.predix.acs.privilege.management.dao;

import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.PARENT_EDGE_LABEL;
import static com.ge.predix.acs.privilege.management.dao.GraphSubjectRepository.SUBJECT_ID_KEY;
import static com.ge.predix.acs.testutils.XFiles.AGENT_MULDER;
import static com.ge.predix.acs.testutils.XFiles.AGENT_SCULLY;
import static com.ge.predix.acs.testutils.XFiles.FBI;
import static com.ge.predix.acs.testutils.XFiles.FBI_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.MULDERS_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.PENTAGON_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.SECRET_CLASSIFICATION;
import static com.ge.predix.acs.testutils.XFiles.SECRET_GROUP;
import static com.ge.predix.acs.testutils.XFiles.SECRET_GROUP_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.SITE_BASEMENT;
import static com.ge.predix.acs.testutils.XFiles.SITE_PENTAGON;
import static com.ge.predix.acs.testutils.XFiles.SPECIAL_AGENTS_GROUP;
import static com.ge.predix.acs.testutils.XFiles.SPECIAL_AGENTS_GROUP_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.TOP_SECRET_CLASSIFICATION;
import static com.ge.predix.acs.testutils.XFiles.TOP_SECRET_GROUP;
import static com.ge.predix.acs.testutils.XFiles.TOP_SECRET_GROUP_ATTRIBUTES;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ge.predix.acs.config.GraphConfig;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.Parent;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.thinkaurelius.titan.core.TitanException;
import com.thinkaurelius.titan.core.TitanFactory;

public class GraphSubjectRepositoryTest {
    private static final JsonUtils JSON_UTILS = new JsonUtils();
    private static final ZoneEntity TEST_ZONE_1 = new ZoneEntity(1L, "testzone1");
    private static final ZoneEntity TEST_ZONE_2 = new ZoneEntity(2L, "testzone2");
    private static final int CONCURRENT_TEST_THREAD_COUNT = 3;
    private static final int CONCURRENT_TEST_INVOCATIONS = 20;

    private GraphSubjectRepository subjectRepository;
    private GraphTraversalSource graphTraversalSource;
    private Random randomGenerator = new Random();

    @BeforeClass
    public void setup() throws Exception {
        this.subjectRepository = new GraphSubjectRepository();
        Graph graph = TitanFactory.build().set("storage.backend", "inmemory").open();
        GraphConfig.createSchemaElements(graph);
        this.graphTraversalSource = graph.traversal();
        this.subjectRepository.setGraphTraversal(this.graphTraversalSource);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testGetByZoneAndSubjectIdentifier() {
        SubjectEntity subjectEntityForZone1 = persistRandomSubjectToZone1AndAssert();
        SubjectEntity subjectEntityForZone2 = persistRandomSubjectToZone2AndAssert();

        SubjectEntity actualSubjectForZone1 = this.subjectRepository
                .getByZoneAndSubjectIdentifier(TEST_ZONE_1, subjectEntityForZone1.getSubjectIdentifier());
        SubjectEntity actualSubjectForZone2 = this.subjectRepository
                .getByZoneAndSubjectIdentifier(TEST_ZONE_2, subjectEntityForZone2.getSubjectIdentifier());
        assertThat(actualSubjectForZone1, equalTo(subjectEntityForZone1));
        assertThat(actualSubjectForZone2, equalTo(subjectEntityForZone2));
        this.subjectRepository.delete(subjectEntityForZone1);
        this.subjectRepository.delete(subjectEntityForZone2);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testGetByZoneAndSubjectIdentifierAndScopes() {
        SubjectEntity expectedSubject = persistScopedHierarchy(AGENT_MULDER + getRandomNumber(), SITE_BASEMENT);
        String subjectIdentifier = expectedSubject.getSubjectIdentifier();

        HashSet<Attribute> expectedAttributes = new HashSet<>(
                Arrays.asList(SECRET_CLASSIFICATION, TOP_SECRET_CLASSIFICATION, SITE_BASEMENT));
        expectedSubject.setAttributes(expectedAttributes);
        expectedSubject.setAttributesAsJson(JSON_UTILS.serialize(expectedAttributes));

        SubjectEntity actualSubject = this.subjectRepository
                .getSubjectWithInheritedAttributesForScopes(TEST_ZONE_1, subjectIdentifier,
                        new HashSet<>(Collections.singletonList(SITE_BASEMENT)));
        assertThat(actualSubject, equalTo(expectedSubject));

        expectedAttributes = new HashSet<>(Arrays.asList(SECRET_CLASSIFICATION, SITE_BASEMENT));
        expectedSubject.setAttributes(expectedAttributes);
        expectedSubject.setAttributesAsJson(JSON_UTILS.serialize(expectedAttributes));
        actualSubject = this.subjectRepository
                .getSubjectWithInheritedAttributesForScopes(TEST_ZONE_1, subjectIdentifier,
                        new HashSet<>(Collections.singletonList(SITE_PENTAGON)));
        assertThat(actualSubject, equalTo(expectedSubject));
        GraphResourceRepositoryTest
                .deleteTwoLevelEntityAndParents(expectedSubject, TEST_ZONE_1, this.subjectRepository);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testParentAndChildSameAttribute() {
        SubjectEntity agentScully = new SubjectEntity(TEST_ZONE_1, AGENT_SCULLY + getRandomNumber());
        agentScully.setAttributes(MULDERS_ATTRIBUTES);
        agentScully.setAttributesAsJson(JSON_UTILS.serialize(agentScully.getAttributes()));
        saveWithRetry(agentScully, 3);
        SubjectEntity agentMulder = new SubjectEntity(TEST_ZONE_1, AGENT_MULDER + getRandomNumber());
        agentMulder.setAttributes(MULDERS_ATTRIBUTES);
        agentMulder.setAttributesAsJson(JSON_UTILS.serialize(agentMulder.getAttributes()));
        agentMulder
                .setParents(new HashSet<>(Collections.singletonList(new Parent(agentScully.getSubjectIdentifier()))));
        saveWithRetry(agentMulder, 3);
        SubjectEntity actualAgentMulder = this.subjectRepository
                .getSubjectWithInheritedAttributesForScopes(TEST_ZONE_1, agentMulder.getSubjectIdentifier(), null);
        assertThat(actualAgentMulder.getAttributesAsJson(),
                equalTo("[{\"issuer\":\"acs.example.org\",\"name\":\"site\",\"value\":\"basement\"}]"));
        GraphResourceRepositoryTest.deleteTwoLevelEntityAndParents(agentScully, TEST_ZONE_1, this.subjectRepository);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testParentAndChildAttributeSameNameDifferentValues() {
        SubjectEntity agentScully = new SubjectEntity(TEST_ZONE_1, AGENT_SCULLY + getRandomNumber());
        agentScully.setAttributes(PENTAGON_ATTRIBUTES);
        agentScully.setAttributesAsJson(JSON_UTILS.serialize(agentScully.getAttributes()));
        saveWithRetry(agentScully, 3);
        SubjectEntity agentMulder = new SubjectEntity(TEST_ZONE_1, AGENT_MULDER + getRandomNumber());
        agentMulder.setAttributes(MULDERS_ATTRIBUTES);
        agentMulder.setAttributesAsJson(JSON_UTILS.serialize(agentMulder.getAttributes()));
        agentMulder
                .setParents(new HashSet<>(Collections.singletonList(new Parent(agentScully.getSubjectIdentifier()))));
        saveWithRetry(agentMulder, 3);
        SubjectEntity actualAgentMulder = this.subjectRepository
                .getSubjectWithInheritedAttributesForScopes(TEST_ZONE_1, agentMulder.getSubjectIdentifier(), null);
        assertThat(actualAgentMulder.getAttributesAsJson(),
                equalTo("[{\"issuer\":\"acs.example.org\",\"name\":\"site\",\"value\":\"basement\"},"
                        + "{\"issuer\":\"acs.example.org\",\"name\":\"site\",\"value\":\"pentagon\"}]"));
        GraphResourceRepositoryTest.deleteTwoLevelEntityAndParents(agentScully, TEST_ZONE_1, this.subjectRepository);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testSave() {
        SubjectEntity subjectEntity = persistRandomSubjectToZone1AndAssert();
        String subjectId = subjectEntity.getSubjectIdentifier();
        GraphTraversal<Vertex, Vertex> traversal = this.graphTraversalSource.V().has(SUBJECT_ID_KEY, subjectId);
        assertThat(traversal.hasNext(), equalTo(true));
        assertThat(traversal.next().property(SUBJECT_ID_KEY).value(), equalTo(subjectId));
        this.subjectRepository.delete(subjectEntity);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testSaveWithNoAttributes() {
        SubjectEntity subject = new SubjectEntity(TEST_ZONE_1, AGENT_SCULLY + getRandomNumber());
        saveWithRetry(subject, 3);
        assertThat(this.subjectRepository.getByZoneAndSubjectIdentifier(TEST_ZONE_1, subject.getSubjectIdentifier()),
                equalTo(subject));
        this.subjectRepository.delete(subject);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testSaveScopes() {
        SubjectEntity subject = persistScopedHierarchy(AGENT_MULDER + getRandomNumber(), SITE_BASEMENT);
        assertThat(IteratorUtils.count(this.graphTraversalSource.V(subject.getId()).outE(PARENT_EDGE_LABEL)),
                equalTo(2L));

        // Persist again (i.e. update) and make sure vertex and edge count are stable.
        this.subjectRepository.save(subject);
        assertThat(IteratorUtils.count(this.graphTraversalSource.V(subject.getId()).outE(PARENT_EDGE_LABEL)),
                equalTo(2L));

        Parent parent = null;
        for (Parent tempParent : this.subjectRepository.findOne(subject.getId()).getParents()) {
            if (tempParent.getIdentifier().contains(TOP_SECRET_GROUP)) {
                parent = tempParent;
            }
        }
        assertThat(parent, notNullValue());
        assertThat("Expected scope not found on subject.", parent.getScopes().contains(SITE_BASEMENT));
        GraphResourceRepositoryTest.deleteTwoLevelEntityAndParents(subject, TEST_ZONE_1, this.subjectRepository);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testGetSubjectEntityAndDescendantsIds() {

        SubjectEntity fbi = persistSubjectToZoneAndAssert(TEST_ZONE_1, FBI + getRandomNumber(), FBI_ATTRIBUTES);

        SubjectEntity specialAgentsGroup = persistSubjectWithParentsToZoneAndAssert(TEST_ZONE_1,
                SPECIAL_AGENTS_GROUP + getRandomNumber(), SPECIAL_AGENTS_GROUP_ATTRIBUTES,
                new HashSet<>(Collections.singletonList(new Parent(fbi.getSubjectIdentifier()))));

        SubjectEntity topSecretGroup = persistSubjectToZoneAndAssert(TEST_ZONE_1, TOP_SECRET_GROUP + getRandomNumber(),
                TOP_SECRET_GROUP_ATTRIBUTES);

        SubjectEntity agentMulder = persistSubjectWithParentsToZoneAndAssert(TEST_ZONE_1,
                AGENT_MULDER + getRandomNumber(), MULDERS_ATTRIBUTES, new HashSet<>(
                        Arrays.asList(new Parent(specialAgentsGroup.getSubjectIdentifier()),
                                new Parent(topSecretGroup.getSubjectIdentifier()))));

        Set<String> descendantsIds = this.subjectRepository.getSubjectEntityAndDescendantsIds(fbi);
        assertThat(descendantsIds, hasSize(3));
        assertThat(descendantsIds, hasItems(fbi.getSubjectIdentifier(), specialAgentsGroup.getSubjectIdentifier(),
                agentMulder.getSubjectIdentifier()));

        descendantsIds = this.subjectRepository.getSubjectEntityAndDescendantsIds(specialAgentsGroup);
        assertThat(descendantsIds, hasSize(2));
        assertThat(descendantsIds,
                hasItems(specialAgentsGroup.getSubjectIdentifier(), agentMulder.getSubjectIdentifier()));

        descendantsIds = this.subjectRepository.getSubjectEntityAndDescendantsIds(topSecretGroup);
        assertThat(descendantsIds, hasSize(2));
        assertThat(descendantsIds, hasItems(topSecretGroup.getSubjectIdentifier(), agentMulder.getSubjectIdentifier()));

        descendantsIds = this.subjectRepository.getSubjectEntityAndDescendantsIds(agentMulder);
        assertThat(descendantsIds, hasSize(1));
        assertThat(descendantsIds, hasItems(agentMulder.getSubjectIdentifier()));

        descendantsIds = this.subjectRepository.getSubjectEntityAndDescendantsIds(null);
        assertThat(descendantsIds, empty());

        descendantsIds = this.subjectRepository
                .getSubjectEntityAndDescendantsIds(new SubjectEntity(TEST_ZONE_1, "/nonexistent-subject"));
        assertThat(descendantsIds, empty());
        GraphResourceRepositoryTest.deleteTwoLevelEntityAndParents(agentMulder, TEST_ZONE_1, this.subjectRepository);
    }

    private SubjectEntity persistRandomSubjectToZone1AndAssert() {
        return persistSubjectToZoneAndAssert(TEST_ZONE_1, AGENT_SCULLY + getRandomNumber(), Collections.emptySet());
    }

    private SubjectEntity persistRandomSubjectToZone2AndAssert() {
        return persistSubjectToZoneAndAssert(TEST_ZONE_2, AGENT_SCULLY + getRandomNumber(), Collections.emptySet());
    }

    private SubjectEntity persistRandomTopSecretGroupAndAssert() {
        return persistSubjectToZoneAndAssert(TEST_ZONE_1, TOP_SECRET_GROUP + getRandomNumber(),
                TOP_SECRET_GROUP_ATTRIBUTES);
    }

    private SubjectEntity persistRandomSecretGroupAndAssert() {
        return persistSubjectToZoneAndAssert(TEST_ZONE_1, SECRET_GROUP + getRandomNumber(), SECRET_GROUP_ATTRIBUTES);
    }

    private int getRandomNumber() {
        return randomGenerator.nextInt(10000000);
    }

    private SubjectEntity persistSubjectToZoneAndAssert(final ZoneEntity zone, final String subjectIdentifier,
            final Set<Attribute> attributes) {
        SubjectEntity subject = new SubjectEntity(zone, subjectIdentifier);
        subject.setAttributes(attributes);
        subject.setAttributesAsJson(JSON_UTILS.serialize(subject.getAttributes()));
        SubjectEntity subjectEntity = saveWithRetry(subject, 3);
        assertThat(this.subjectRepository.findOne(subjectEntity.getId()), equalTo(subject));
        return subjectEntity;
    }

    private SubjectEntity persistSubjectWithParentsToZoneAndAssert(final ZoneEntity zoneEntity,
            final String subjectIdentifier, final Set<Attribute> attributes, final Set<Parent> parents) {
        SubjectEntity subject = new SubjectEntity(zoneEntity, subjectIdentifier);
        subject.setAttributes(attributes);
        subject.setAttributesAsJson(JSON_UTILS.serialize(subject.getAttributes()));
        subject.setParents(parents);
        SubjectEntity subjectEntity = saveWithRetry(subject, 3);
        assertThat(this.subjectRepository.findOne(subjectEntity.getId()), equalTo(subject));
        return subjectEntity;
    }

    private SubjectEntity persistScopedHierarchy(final String subjectIdentifier, final Attribute scope) {
        SubjectEntity secretGroup = persistRandomSecretGroupAndAssert();
        SubjectEntity topSecretGroup = persistRandomTopSecretGroupAndAssert();

        SubjectEntity agentMulder = new SubjectEntity(TEST_ZONE_1, subjectIdentifier);
        agentMulder.setAttributes(MULDERS_ATTRIBUTES);
        agentMulder.setAttributesAsJson(JSON_UTILS.serialize(agentMulder.getAttributes()));
        agentMulder.setParents(new HashSet<>(Arrays.asList(
                new Parent(topSecretGroup.getSubjectIdentifier(), new HashSet<>(Collections.singletonList(scope))),
                new Parent(secretGroup.getSubjectIdentifier()))));
        return this.subjectRepository.save(agentMulder);
    }

    private SubjectEntity saveWithRetry(final SubjectEntity subject, final int retryCount) throws TitanException {
        return GraphResourceRepositoryTest.saveWithRetry(this.subjectRepository, subject, retryCount);
    }

}
