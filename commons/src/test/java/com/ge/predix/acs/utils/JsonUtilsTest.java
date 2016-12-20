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
package com.ge.predix.acs.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.PolicySet;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class JsonUtilsTest {

    private static final int STUDENT_AGE = 15;
    private final JsonUtils jsonUtils = new JsonUtils();

    @Test
    public void testDeserialize() {
        Student student = this.jsonUtils.deserialize(
                "{\"id\":\"S00001\",\"age\":15,\"name\":\"Joe\",\"courses\":[\"Math\",\"Arts\"]}", Student.class);
        assertStudent(student);
    }

    @Test
    public void testDeserializeCollectionOfTypedItems() {
        String studentsAsJson = "[{\"issuer\":\"acs\",\"name\":\"role\",\"value\":\"analyst\"},"
                + "{\"issuer\":\"acs\",\"name\":\"role\",\"value\":\"admin\"}]";

        @SuppressWarnings("unchecked")
        Set<Attribute> deserialize = this.jsonUtils.deserialize(studentsAsJson, Set.class, Attribute.class);

        Assert.assertNotNull(deserialize);
        Assert.assertEquals(deserialize.size(), 2);

        Iterator<Attribute> iterator = deserialize.iterator();
        String value1 = iterator.next().getValue();
        String value2 = iterator.next().getValue();
        Assert.assertTrue(value1.equals("analyst") || value1.equals("admin"));
        Assert.assertTrue(value2.equals("analyst") || value2.equals("admin"));
    }

    @Test
    public void testDeserializeFromFile() {
        Student student = this.jsonUtils.deserializeFromFile("student.json", Student.class);
        assertStudent(student);
    }

    @Test
    public void testDeserializeFromNotFoundFile() {
        Assert.assertNull(this.jsonUtils.deserializeFromFile("student-no-found.json", Student.class));
    }

    @Test
    public void testReadJsonNodeFromFile() {
        JsonNode studentJsonNode = this.jsonUtils.readJsonNodeFromFile("student.json");
        assertStudentJsonNode(studentJsonNode);
    }

    @Test
    public void testReadJsonNodeFromObject() {
        JsonNode studentJsonNode = this.jsonUtils.readJsonNodeFromObject(createStudent());
        assertStudentJsonNode(studentJsonNode);
    }

    @Test
    public void testSerialize() {
        Student student = createStudent();
        String serializedStudent = this.jsonUtils.serialize(student);
        Assert.assertNotNull(serializedStudent);
    }

    @Test
    public void testDoNotSerializaNullProperties() {
        PolicySet ps = new PolicySet();
        String serializedObject = this.jsonUtils.serialize(ps);
        Assert.assertNotNull(serializedObject);
        Assert.assertFalse(serializedObject.contains("null"));
    }

    private void assertStudent(final Student student) {
        Assert.assertNotNull(student);
        Assert.assertEquals(student.getId(), "S00001");
        Assert.assertEquals(student.getName(), "Joe");
        Assert.assertEquals(student.getAge(), STUDENT_AGE);
        Assert.assertEquals(student.getCourses().size(), 2);
        Assert.assertEquals(student.getCourses().get(0), "Math");
        Assert.assertEquals(student.getCourses().get(1), "Arts");
    }

    private void assertStudentJsonNode(final JsonNode studentJsonNode) {
        Assert.assertNotNull(studentJsonNode);
        Assert.assertEquals(studentJsonNode.findValuesAsText("id").get(0), "S00001");
        Assert.assertEquals(studentJsonNode.findValuesAsText("name").get(0), "Joe");
        Assert.assertEquals(studentJsonNode.findValuesAsText("age").get(0), "15");
        Assert.assertEquals(studentJsonNode.findValue("courses").get(0).asText(), "Math");
        Assert.assertEquals(studentJsonNode.findValue("courses").get(1).asText(), "Arts");
    }

    private Student createStudent() {
        Student s = new Student();
        s.setAge(STUDENT_AGE);
        s.setName("Joe");
        s.setId("S00001");
        s.setCourses(Arrays.asList("Math", "Arts"));
        return s;
    }

    private static class Student {
        private String id;
        private int age;
        private String name;
        private List<String> courses;

        Student() {
        }

        public String getId() {
            return this.id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public int getAge() {
            return this.age;
        }

        public void setAge(final int age) {
            this.age = age;
        }

        public String getName() {
            return this.name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public List<String> getCourses() {
            return this.courses;
        }

        public void setCourses(final List<String> courses) {
            this.courses = courses;
        }

    }
}
