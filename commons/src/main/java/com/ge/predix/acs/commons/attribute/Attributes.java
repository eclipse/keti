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

package com.ge.predix.acs.commons.attribute;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author 212314537
 */
@SuppressWarnings("nls")
public class Attributes {
    private final Set<Attribute> attributeSet;
    private final Set<AttributeType> attributeTypeSet;

    /**
     * Default constructor.
     */
    public Attributes() {
        this.attributeSet = new HashSet<>();
        this.attributeTypeSet = new HashSet<>();
    }

    /**
     * @param attributeSet
     *            set of attributes.
     */
    public Attributes(final Set<Attribute> attributeSet) {
        super();

        if (null == attributeSet) {
            throw new IllegalArgumentException("A Null attribute set is not allowed.");
        }

        this.attributeSet = attributeSet;
        this.attributeTypeSet = new HashSet<>();
        for (Attribute attribute : this.attributeSet) {
            this.attributeTypeSet.add(attribute.getType());
        }
    }

    /**
     * @param attributeType
     *            the attribute type to search for
     * @return true if the attributes contain the specified attribute type
     */
    public boolean hasAttributeType(final AttributeType attributeType) {
        return this.attributeTypeSet.contains(attributeType);
    }

    /**
     * @param attributeTypeSetCriteria
     *            set of attribute types to search for
     * @return true if the attributes contain the specified attribute type
     */
    public boolean hasAttributeTypes(final Set<AttributeType> attributeTypeSetCriteria) {
        return this.attributeTypeSet.containsAll(attributeTypeSetCriteria);
    }

    /**
     * @param attribute
     *            the attribute to search for
     * @return true if the attributes contain the specified attribute
     */
    public boolean hasAttribute(final Attribute attribute) {
        return this.attributeSet.contains(attribute);
    }

    /**
     * @param attributeSetCriteria
     *            attributes to look for
     * @return true if all attributes in the criteria are present
     */
    public boolean hasAttributes(final Set<Attribute> attributeSetCriteria) {
        return this.attributeSet.containsAll(attributeSetCriteria);
    }

    /**
     * @param attributeSetToMerge
     *            set of attributes to merge with current attributes
     * @return true if the attributes are changed false otherwise
     */
    public boolean addAll(final Set<Attribute> attributeSetToMerge) {
        boolean hasChanged = this.attributeSet.addAll(attributeSetToMerge);
        if (hasChanged) {
            for (Attribute attribute : attributeSetToMerge) {
                this.attributeTypeSet.add(attribute.getType());
            }
        }
        return hasChanged;
    }
}
