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

package org.eclipse.keti.acs.utils

import com.fasterxml.jackson.databind.JsonNode
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.PolicySet
import org.testng.Assert
import org.testng.annotations.Test

import java.util.Arrays

private const val STUDENT_AGE = 15

class JsonUtilsTest {
    private val jsonUtils = JsonUtils()

    @Test
    fun testDeserialize() {
        val student = this.jsonUtils.deserialize(
            "{\"id\":\"S00001\",\"age\":15,\"name\":\"Joe\",\"courses\":[\"Math\",\"Arts\"]}", Student::class.java
        )
        assertStudent(student)
    }

    @Test
    fun testDeserializeCollectionOfTypedItems() {
        val studentsAsJson =
            "[{\"issuer\":\"acs\",\"name\":\"role\",\"value\":\"analyst\"}," + "{\"issuer\":\"acs\",\"name\":\"role\",\"value\":\"admin\"}]"

        val deserialize =
            this.jsonUtils.deserialize(studentsAsJson, Set::class.java as Class<Set<Attribute>>, Attribute::class.java)

        Assert.assertNotNull(deserialize)
        Assert.assertEquals(deserialize!!.size, 2)

        val iterator = deserialize.iterator()
        val value1 = iterator.next().value
        val value2 = iterator.next().value
        Assert.assertTrue(value1 == "analyst" || value1 == "admin")
        Assert.assertTrue(value2 == "analyst" || value2 == "admin")
    }

    @Test
    fun testDeserializeFromFile() {
        val student = this.jsonUtils.deserializeFromFile("student.json", Student::class.java)
        assertStudent(student)
    }

    @Test
    fun testDeserializeFromNotFoundFile() {
        Assert.assertNull(this.jsonUtils.deserializeFromFile("student-no-found.json", Student::class.java))
    }

    @Test
    fun testReadJsonNodeFromFile() {
        val studentJsonNode = this.jsonUtils.readJsonNodeFromFile("student.json")
        assertStudentJsonNode(studentJsonNode)
    }

    @Test
    fun testReadJsonNodeFromObject() {
        val studentJsonNode = this.jsonUtils.readJsonNodeFromObject(createStudent())
        assertStudentJsonNode(studentJsonNode)
    }

    @Test
    fun testSerialize() {
        val student = createStudent()
        val serializedStudent = this.jsonUtils.serialize(student)
        Assert.assertNotNull(serializedStudent)
    }

    @Test
    fun testDoNotSerializaNullProperties() {
        val ps = PolicySet()
        val serializedObject = this.jsonUtils.serialize(ps)
        Assert.assertNotNull(serializedObject)
        Assert.assertFalse(serializedObject!!.contains("null"))
    }

    private fun assertStudent(student: Student?) {
        Assert.assertNotNull(student)
        Assert.assertEquals(student!!.id, "S00001")
        Assert.assertEquals(student.name, "Joe")
        Assert.assertEquals(student.age, STUDENT_AGE)
        Assert.assertEquals(student.courses!!.size, 2)
        Assert.assertEquals(student.courses!![0], "Math")
        Assert.assertEquals(student.courses!![1], "Arts")
    }

    private fun assertStudentJsonNode(studentJsonNode: JsonNode?) {
        Assert.assertNotNull(studentJsonNode)
        Assert.assertEquals(studentJsonNode!!.findValuesAsText("id")[0], "S00001")
        Assert.assertEquals(studentJsonNode.findValuesAsText("name")[0], "Joe")
        Assert.assertEquals(studentJsonNode.findValuesAsText("age")[0], "15")
        Assert.assertEquals(studentJsonNode.findValue("courses").get(0).asText(), "Math")
        Assert.assertEquals(studentJsonNode.findValue("courses").get(1).asText(), "Arts")
    }

    private fun createStudent(): Student {
        val s = Student()
        s.age = STUDENT_AGE
        s.name = "Joe"
        s.id = "S00001"
        s.courses = Arrays.asList("Math", "Arts")
        return s
    }

    private class Student internal constructor() {
        var id: String? = null
        var age: Int = 0
        var name: String? = null
        var courses: List<String>? = null
    }
}
