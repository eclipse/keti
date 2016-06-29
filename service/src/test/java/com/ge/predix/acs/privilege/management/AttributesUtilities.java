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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.ge.predix.acs.model.Attribute;

/**
 * @author 212360328
 */
public class AttributesUtilities {

    /**
     * Converts a list of attributes into a Set.
     *
     * @param attrs
     *            dynamic list of attributes
     * @return a set of the attributes
     */
    public Set<Attribute> getSetOfAttributes(final Attribute... attrs) {
        Set<Attribute> attributes = new HashSet<>();
        attributes.addAll(Arrays.asList(attrs));
        return attributes;
    }
}
