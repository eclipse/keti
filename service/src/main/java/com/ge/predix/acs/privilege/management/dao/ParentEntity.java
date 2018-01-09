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

package com.ge.predix.acs.privilege.management.dao;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;

public class ParentEntity {

    private ZonableEntity childEntity;
    private Set<Attribute> scopes;

    ParentEntity() {
        // Default constructor.
    }

    public ParentEntity(final ZonableEntity entity) {
        this.childEntity = entity;
    }

    public ParentEntity(final ZonableEntity entity, final Set<Attribute> scopes) {
        this.childEntity = entity;
        this.scopes = scopes;
    }

    public ZonableEntity getChildEntity() {
        return this.childEntity;
    }

    public void setChildEntity(final ZonableEntity entity) {
        this.childEntity = entity;
    }

    public Set<Attribute> getScopes() {
        return this.scopes;
    }

    public void setScopes(final Set<Attribute> scopes) {
        this.scopes = scopes;
    }

}
