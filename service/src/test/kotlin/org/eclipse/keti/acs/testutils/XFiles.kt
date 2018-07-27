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

package org.eclipse.keti.acs.testutils

import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.rest.Parent
import java.util.Arrays
import java.util.Collections
import java.util.HashSet

private const val ISSUER_URI = "acs.example.org"
val SITE_BASEMENT = Attribute(ISSUER_URI, "site", "basement")
val SITE_PENTAGON = Attribute(ISSUER_URI, "site", "pentagon")
val SITE_QUANTICO = Attribute(ISSUER_URI, "site", "quantico")
val TOP_SECRET_CLASSIFICATION = Attribute(ISSUER_URI, "classification", "top secret")
val SECRET_CLASSIFICATION = Attribute(ISSUER_URI, "classification", "secret")
val SPECIAL_AGENTS_GROUP_ATTRIBUTE = Attribute(ISSUER_URI, "group", "special-agents")
val TYPE_MYTHARC = Attribute(ISSUER_URI, "type", "mytharc")
val TYPE_MONSTER_OF_THE_WEEK = Attribute(
    "acs.predix.ge.com", "type",
    "monster-of-the-week"
)

const val FBI = "fbi"
val FBI_ATTRIBUTES: MutableSet<Attribute> = Collections.unmodifiableSet(HashSet(Arrays.asList(SITE_QUANTICO)))

const val SPECIAL_AGENTS_GROUP = "special-agents"
val SPECIAL_AGENTS_GROUP_ATTRIBUTES: MutableSet<Attribute> =
    Collections.unmodifiableSet(HashSet(Arrays.asList(SPECIAL_AGENTS_GROUP_ATTRIBUTE)))

const val TOP_SECRET_GROUP = "top-secret-clearance"
val TOP_SECRET_GROUP_ATTRIBUTES: MutableSet<Attribute> =
    Collections.unmodifiableSet(HashSet(Arrays.asList(TOP_SECRET_CLASSIFICATION)))

const val SECRET_GROUP = "secret-clearance"
val SECRET_GROUP_ATTRIBUTES: MutableSet<Attribute> =
    Collections.unmodifiableSet(HashSet(Arrays.asList(SECRET_CLASSIFICATION)))

const val AGENT_MULDER = "mulder"
val MULDERS_ATTRIBUTES: MutableSet<Attribute> = Collections.unmodifiableSet(HashSet(Arrays.asList(SITE_BASEMENT)))

const val AGENT_SCULLY = "scully"

const val BASEMENT_SITE_ID = "/site/basement"
val BASEMENT_ATTRIBUTES: MutableSet<Attribute> = Collections.unmodifiableSet(HashSet(Arrays.asList(SITE_BASEMENT)))

const val PENTAGON_SITE_ID = "/site/pentagon"
val PENTAGON_ATTRIBUTES: MutableSet<Attribute> = Collections.unmodifiableSet(HashSet(Arrays.asList(SITE_PENTAGON)))

const val XFILES_ID = "/x-files"

const val ASCENSION_ID = "/x-files/ascension"
val ASCENSION_ATTRIBUTES: MutableSet<Attribute> =
    Collections.unmodifiableSet(HashSet(Arrays.asList(SPECIAL_AGENTS_GROUP_ATTRIBUTE, TYPE_MYTHARC)))

const val DRIVE_ID = "/x-files/drive"
val DRIVE_ATTRIBUTES: MutableSet<Attribute> =
    Collections.unmodifiableSet(HashSet(Arrays.asList(TYPE_MONSTER_OF_THE_WEEK)))

const val JOSECHUNG_ID = "/x-files/josechung"

const val EVIDENCE_SCULLYS_TESTIMONY_ID = "/evidence/scullys-testimony"
val SCULLYS_TESTIMONY_ATTRIBUTES: MutableSet<Attribute> =
    Collections.unmodifiableSet(HashSet(Arrays.asList(TOP_SECRET_CLASSIFICATION)))

const val EVIDENCE_IMPLANT_ID = "/evidence/implant"
val EVIDENCE_IMPLANT_ATTRIBUTES: MutableSet<Attribute> =
    Collections.unmodifiableSet(HashSet(Arrays.asList(TOP_SECRET_CLASSIFICATION)))

fun createSubjectHierarchy(): List<BaseSubject> {
    val fbi = BaseSubject(FBI)
    fbi.attributes = FBI_ATTRIBUTES

    val specialAgentsGroup = BaseSubject(SPECIAL_AGENTS_GROUP)
    specialAgentsGroup.attributes = SPECIAL_AGENTS_GROUP_ATTRIBUTES
    specialAgentsGroup
        .parents = HashSet(Arrays.asList(Parent(fbi.subjectIdentifier!!)))

    val topSecretGroup = BaseSubject(TOP_SECRET_GROUP)
    topSecretGroup.attributes = TOP_SECRET_GROUP_ATTRIBUTES

    val agentMulder = BaseSubject(AGENT_MULDER)
    agentMulder.attributes = MULDERS_ATTRIBUTES
    agentMulder.parents = HashSet(
        Arrays.asList(
            Parent(specialAgentsGroup.subjectIdentifier!!), Parent(topSecretGroup.subjectIdentifier!!)
        )
    )

    return Arrays.asList(fbi, specialAgentsGroup, topSecretGroup, agentMulder)
}

fun createTwoParentResourceHierarchy(): List<BaseResource> {
    val pentagon = BaseResource(PENTAGON_SITE_ID)
    pentagon.attributes = PENTAGON_ATTRIBUTES

    val ascension = BaseResource(ASCENSION_ID)
    ascension.attributes = ASCENSION_ATTRIBUTES

    val evidenceImplant = BaseResource(EVIDENCE_IMPLANT_ID)
    evidenceImplant.attributes = EVIDENCE_IMPLANT_ATTRIBUTES
    evidenceImplant.parents = HashSet(
        Arrays.asList(
            Parent(pentagon.resourceIdentifier!!), Parent(ascension.resourceIdentifier!!)
        )
    )

    return Arrays.asList(pentagon, ascension, evidenceImplant)
}

fun createTwoLevelResourceHierarchy(): List<BaseResource> {

    val basement = BaseResource(BASEMENT_SITE_ID)
    basement.attributes = BASEMENT_ATTRIBUTES

    val scullysTestimony = BaseResource(EVIDENCE_SCULLYS_TESTIMONY_ID)
    scullysTestimony.attributes = SCULLYS_TESTIMONY_ATTRIBUTES
    scullysTestimony.parents = HashSet(Arrays.asList(Parent(basement.resourceIdentifier!!)))

    return Arrays.asList(basement, scullysTestimony)
}

fun createThreeLevelResourceHierarchy(): List<BaseResource> {
    val basement = BaseResource(BASEMENT_SITE_ID)
    basement.attributes = BASEMENT_ATTRIBUTES

    val ascension = BaseResource(ASCENSION_ID)
    ascension.attributes = ASCENSION_ATTRIBUTES
    ascension.parents = HashSet(Arrays.asList(Parent(basement.resourceIdentifier!!)))

    val scullysTestimony = BaseResource(EVIDENCE_SCULLYS_TESTIMONY_ID)
    scullysTestimony.attributes = SCULLYS_TESTIMONY_ATTRIBUTES
    scullysTestimony.parents = HashSet(Arrays.asList(Parent(ascension.resourceIdentifier!!)))

    return Arrays.asList(basement, ascension, scullysTestimony)
}

fun createScopedSubjectHierarchy(): List<BaseSubject> {
    val fbi = BaseSubject(FBI)
    fbi.attributes = FBI_ATTRIBUTES

    val specialAgentsGroup = BaseSubject(SPECIAL_AGENTS_GROUP)
    specialAgentsGroup.attributes = SPECIAL_AGENTS_GROUP_ATTRIBUTES
    specialAgentsGroup
        .parents = HashSet(Arrays.asList(Parent(fbi.subjectIdentifier!!)))

    val topSecretGroup = BaseSubject(TOP_SECRET_GROUP)
    topSecretGroup.attributes = TOP_SECRET_GROUP_ATTRIBUTES

    val agentMulder = BaseSubject(AGENT_MULDER)
    agentMulder.attributes = MULDERS_ATTRIBUTES
    agentMulder.parents = HashSet(
        Arrays.asList(
            Parent(specialAgentsGroup.subjectIdentifier!!), Parent(
                topSecretGroup.subjectIdentifier!!,
                HashSet(Arrays.asList(SITE_BASEMENT))
            )
        )
    )
    return Arrays.asList(fbi, specialAgentsGroup, topSecretGroup, agentMulder)
}
