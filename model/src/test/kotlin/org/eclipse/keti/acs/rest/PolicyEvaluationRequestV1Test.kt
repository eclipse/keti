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

package org.eclipse.keti.acs.rest

import org.eclipse.keti.acs.model.Attribute
import org.testng.Assert
import org.testng.annotations.Test
import java.util.HashSet
import java.util.LinkedHashSet

val EVALUATION_ORDER_P1_P2 = LinkedHashSet<String?>(listOf("P1", "P2"))
val EVALUATION_ORDER_P2_P1 = LinkedHashSet<String?>(listOf("P2", "P1"))

class PolicyEvaluationRequestV1Test {

    @Test
    fun testEqualsNoAttributes() {
        val a = PolicyEvaluationRequestV1()
        a.subjectIdentifier = "subject"
        a.action = "GET"
        a.resourceIdentifier = "/resource"

        val b = PolicyEvaluationRequestV1()
        b.subjectIdentifier = "subject"
        b.action = "GET"
        b.resourceIdentifier = "/resource"
        Assert.assertEquals(a, b)
    }

    @Test
    fun testEqualsSameAttributesAndPolicySetsPriority() {
        val a = PolicyEvaluationRequestV1()
        a.subjectIdentifier = "subject"
        a.action = "GET"
        a.resourceIdentifier = "/resource"
        a.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role"), Attribute("issuer", "group")
            )
        )
        a.policySetsEvaluationOrder = EVALUATION_ORDER_P1_P2

        val b = PolicyEvaluationRequestV1()
        b.subjectIdentifier = "subject"
        b.action = "GET"
        b.resourceIdentifier = "/resource"
        b.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role"), Attribute("issuer", "group")
            )
        )
        b.policySetsEvaluationOrder = EVALUATION_ORDER_P1_P2
        Assert.assertEquals(a, b)
    }

    @Test
    fun testEqualsThisHasNoAttributes() {
        val a = PolicyEvaluationRequestV1()
        a.subjectIdentifier = "subject"
        a.action = "GET"
        a.resourceIdentifier = "/resource"

        val b = PolicyEvaluationRequestV1()
        b.subjectIdentifier = "subject"
        b.action = "GET"
        b.resourceIdentifier = "/resource"
        b.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role"), Attribute("issuer", "group")
            )
        )
        Assert.assertNotEquals(a, b)
    }

    @Test
    fun testEqualsThatHasNoAttributes() {
        val a = PolicyEvaluationRequestV1()
        a.subjectIdentifier = "subject"
        a.action = "GET"
        a.resourceIdentifier = "/resource"
        a.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role"), Attribute("issuer", "group")
            )
        )

        val b = PolicyEvaluationRequestV1()
        b.subjectIdentifier = "subject"
        b.action = "GET"
        b.resourceIdentifier = "/resource"
        Assert.assertNotEquals(a, b)
    }

    @Test
    fun testEqualsSizeAttributes() {
        val a = PolicyEvaluationRequestV1()
        a.subjectIdentifier = "subject"
        a.action = "GET"
        a.resourceIdentifier = "/resource"
        a.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role")
            )
        )

        val b = PolicyEvaluationRequestV1()
        b.subjectIdentifier = "subject"
        b.action = "GET"
        b.resourceIdentifier = "/resource"
        b.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role"), Attribute("issuer", "group")
            )
        )
        Assert.assertNotEquals(a, b)
    }

    @Test
    fun testEqualsDifferentAttributes() {
        val a = PolicyEvaluationRequestV1()
        a.subjectIdentifier = "subject"
        a.action = "GET"
        a.resourceIdentifier = "/resource"
        a.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role"), Attribute("issuer", "site")
            )
        )

        val b = PolicyEvaluationRequestV1()
        b.subjectIdentifier = "subject"
        b.action = "GET"
        b.resourceIdentifier = "/resource"
        b.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role"), Attribute("issuer", "group")
            )
        )
        Assert.assertNotEquals(a, b)
    }

    @Test
    fun testEqualsDifferentOrderPolicySetPriorities() {
        val a = PolicyEvaluationRequestV1()
        a.subjectIdentifier = "subject"
        a.action = "GET"
        a.resourceIdentifier = "/resource"
        a.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role"), Attribute("issuer", "site")
            )
        )
        a.policySetsEvaluationOrder = EVALUATION_ORDER_P1_P2

        val b = PolicyEvaluationRequestV1()
        b.subjectIdentifier = "subject"
        b.action = "GET"
        b.resourceIdentifier = "/resource"
        b.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role"), Attribute("issuer", "site")
            )
        )
        a.policySetsEvaluationOrder = EVALUATION_ORDER_P2_P1
        Assert.assertNotEquals(a, b)
    }

    @Test
    fun testHashCodeNoAttributes() {
        val a = PolicyEvaluationRequestV1()
        a.subjectIdentifier = "subject"
        a.action = "GET"
        a.resourceIdentifier = "/resource"

        val b = PolicyEvaluationRequestV1()
        b.subjectIdentifier = "subject"
        b.action = "GET"
        b.resourceIdentifier = "/resource"
        Assert.assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testHashCodeSameAttributesAndPolicySetsPriority() {
        val a = PolicyEvaluationRequestV1()
        a.subjectIdentifier = "subject"
        a.action = "GET"
        a.resourceIdentifier = "/resource"
        a.subjectAttributes = HashSet(
            listOf(Attribute("issuer", "role"), Attribute("issuer", "group"))
        )
        a.policySetsEvaluationOrder = EVALUATION_ORDER_P1_P2

        val b = PolicyEvaluationRequestV1()
        b.subjectIdentifier = "subject"
        b.action = "GET"
        b.resourceIdentifier = "/resource"
        b.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role"), Attribute("issuer", "group")
            )
        )
        b.policySetsEvaluationOrder = EVALUATION_ORDER_P1_P2
        Assert.assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testHashCodeThisHasNoAttributes() {
        val a = PolicyEvaluationRequestV1()
        a.subjectIdentifier = "subject"
        a.action = "GET"
        a.resourceIdentifier = "/resource"

        val b = PolicyEvaluationRequestV1()
        b.subjectIdentifier = "subject"
        b.action = "GET"
        b.resourceIdentifier = "/resource"
        b.subjectAttributes = HashSet(
            listOf(Attribute("issuer", "role"), Attribute("issuer", "group"))
        )
        Assert.assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testHashCodeThatHasNoAttributes() {
        val a = PolicyEvaluationRequestV1()
        a.subjectIdentifier = "subject"
        a.action = "GET"
        a.resourceIdentifier = "/resource"
        a.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role"), Attribute("issuer", "group")
            )
        )

        val b = PolicyEvaluationRequestV1()
        b.subjectIdentifier = "subject"
        b.action = "GET"
        b.resourceIdentifier = "/resource"
        Assert.assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testHashCodeSizeAttributes() {
        val a = PolicyEvaluationRequestV1()
        a.subjectIdentifier = "subject"
        a.action = "GET"
        a.resourceIdentifier = "/resource"
        a.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role")
            )
        )

        val b = PolicyEvaluationRequestV1()
        b.subjectIdentifier = "subject"
        b.action = "GET"
        b.resourceIdentifier = "/resource"
        b.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role"), Attribute("issuer", "group")
            )
        )
        Assert.assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testHashCodeDifferentOrderPolicySetsPriority() {
        val a = PolicyEvaluationRequestV1()
        a.subjectIdentifier = "subject"
        a.action = "GET"
        a.resourceIdentifier = "/resource"
        a.policySetsEvaluationOrder = EVALUATION_ORDER_P1_P2

        val b = PolicyEvaluationRequestV1()
        b.subjectIdentifier = "subject"
        b.action = "GET"
        b.resourceIdentifier = "/resource"
        b.policySetsEvaluationOrder = EVALUATION_ORDER_P2_P1
        Assert.assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testHashCodeDifferentAttributes() {
        val a = PolicyEvaluationRequestV1()
        a.subjectIdentifier = "subject"
        a.action = "GET"
        a.resourceIdentifier = "/resource"
        a.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role"), Attribute("issuer", "site")
            )
        )

        val b = PolicyEvaluationRequestV1()
        b.subjectIdentifier = "subject"
        b.action = "GET"
        b.resourceIdentifier = "/resource"
        b.subjectAttributes = HashSet(
            listOf(
                Attribute("issuer", "role"), Attribute("issuer", "group")
            )
        )
        Assert.assertNotEquals(a.hashCode(), b.hashCode())
    }
}
