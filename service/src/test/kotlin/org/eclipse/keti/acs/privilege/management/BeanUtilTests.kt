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

package org.eclipse.keti.acs.privilege.management

import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.utils.JsonUtils
import org.testng.annotations.Test
import java.util.HashSet

/**
 *
 * @author acs-engineers@ge.com
 */
class BeanUtilTests {

    @Test
    fun test() {
        val ju = JsonUtils()
        val a1 = Attribute("acs", "role", "admin")
        val s = HashSet<Attribute>()
        s.add(a1)
        val serialize = ju.serialize<Set<Attribute>>(s)
        println(serialize)

        val deserialize = ju.deserialize(serialize!!, Set::class.java as Class<Set<Attribute>>, Attribute::class.java)
        val iterator = deserialize!!.iterator()
        val next = iterator.next()
        println(next)
    }
}
