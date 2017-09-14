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
 *******************************************************************************/

package com.ge.predix.acs.service.policy.evaluation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ge.predix.acs.model.Attribute;

/**
 * @author 212314537
 */
public class AttributesHandler {
    private final Map<String, String> attributeMap = new HashMap<>();

    /**
     * Default constructor.
     *
     * @param attributes
     *            attribute list
     */
    public AttributesHandler(final List<Attribute> attributes) {
        for (Attribute attributeValue : attributes) {
            this.attributeMap.put(getKey(attributeValue.getIssuer(), attributeValue.getName()),
                    attributeValue.getValue());
        }
    }

    /**
     * @param issuer
     * @param name
     * @return
     */
    private String getKey(final String issuer, final String name) {
        return issuer + name;
    }

    /**
     * Get an attribute value.
     *
     * @param issuer
     *            attribute issuer
     * @param name
     *            attribute name
     * @return attribute value
     */
    public String attributes(final String issuer, final String name) {
        return this.attributeMap.get(getKey(issuer, name));
    }

    /**
     * @param issuer
     *            claim issuer
     * @param name
     *            claim name
     * @param value
     *            claim value
     * @return true if has claim
     */
    public boolean hasAttribute(final String issuer, final String name, final String value) {
        return this.attributeMap.containsKey(getKey(issuer, name));
    }
}
