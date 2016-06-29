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

package com.ge.predix.acs.privilege.management;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.testng.annotations.Test;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.utils.JsonUtils;

/**
 *
 * @author 212360328
 */
public class BeanUtilTests {

    @Test
    public void test() {
        JsonUtils ju = new JsonUtils();
        Attribute a1 = new Attribute("acs", "role", "admin");
        Set<Attribute> s = new HashSet<>();
        s.add(a1);
        String serialize = ju.serialize(s);
        System.out.println(serialize);

        @SuppressWarnings({ "unchecked" })
        Set<Attribute> deserialize = ju.deserialize(serialize, Set.class, Attribute.class);
        Iterator<Attribute> iterator = deserialize.iterator();
        Attribute next = iterator.next();
        System.out.println(next);
    }
}
