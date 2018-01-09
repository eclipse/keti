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

package com.ge.predix.acs.attribute.readers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.rest.BaseResource;

@Component
public class PrivilegeServiceResourceAttributeReader implements ResourceAttributeReader {
    @Autowired
    private PrivilegeManagementService privilegeManagementService;

    @Override
    public Set<Attribute> getAttributes(final String identifier) {
        Set<Attribute> resourceAttributes = Collections.emptySet();
        BaseResource resource =
            this.privilegeManagementService.getByResourceIdentifierWithInheritedAttributes(identifier);
        if (null != resource) {
            resourceAttributes = Collections.unmodifiableSet(new HashSet<>(resource.getAttributes()));
        }
        return resourceAttributes;
    }
}
