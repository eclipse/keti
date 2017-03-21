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
package com.ge.predix.acs.zone.management.dao;

import java.io.IOException;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.acs.privilege.management.dao.ResourceEntity;
import com.ge.predix.acs.privilege.management.dao.SubjectEntity;
import com.ge.predix.acs.rest.AttributeConnector;
import com.ge.predix.acs.service.policy.admin.dao.PolicySetEntity;

@Entity
@Table(
        name = "authorization_zone",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }),
                @UniqueConstraint(columnNames = { "subdomain" }) })
public class ZoneEntity {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "subdomain", nullable = false, unique = true)
    private String subdomain;

    @OneToMany(
            mappedBy = "zone",
            cascade = { CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE },
            fetch = FetchType.LAZY)
    private Set<SubjectEntity> subjects;

    @OneToMany(
            mappedBy = "zone",
            cascade = { CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE },
            fetch = FetchType.LAZY)
    private Set<ResourceEntity> resources;

    @OneToMany(
            mappedBy = "zone",
            cascade = { CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE },
            fetch = FetchType.LAZY)
    private Set<PolicySetEntity> policySets;

    @Column(name = "resource_attribute_connector_json", nullable = true)
    private String resourceConnectorJson;

    @Column(name = "subject_attribute_connector_json", nullable = true)
    private String subjectConnectorJson;

    private AttributeConnector cachedResourceConnector;

    private AttributeConnector cachedSubjectConnector;

    public ZoneEntity() {
    }

    public ZoneEntity(final Long id) {
        this.id = id;
    }

    public ZoneEntity(final Long id, final String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return this.id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getSubdomain() {
        return this.subdomain;
    }

    public void setSubdomain(final String subdomain) {
        this.subdomain = subdomain;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.name).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ZoneEntity) {
            ZoneEntity other = (ZoneEntity) obj;
            return new EqualsBuilder().append(this.getName(), other.getName()).isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return "ZoneEntity [id=" + this.id + ", name=" + this.name + ", description=" + this.description
                + ", subdomain=" + this.subdomain + "]";
    }

    public AttributeConnector getResourceAttributeConnector() {
        if (null == this.cachedResourceConnector) {
            this.cachedResourceConnector = connectorFromJson(this.resourceConnectorJson);
        }
        return this.cachedResourceConnector;
    }

    public void setResourceAttributeConnector(final AttributeConnector connector) {
        this.resourceConnectorJson = jsonFromConnector(connector);
        this.cachedResourceConnector = connector;
    }

    public AttributeConnector getSubjectAttributeConnector() {
        if (null == this.cachedSubjectConnector) {
            this.cachedSubjectConnector = connectorFromJson(this.subjectConnectorJson);
        }
        return this.cachedSubjectConnector;
    }

    public void setSubjectAttributeConnector(final AttributeConnector connector) {
        this.subjectConnectorJson = jsonFromConnector(connector);
        this.cachedSubjectConnector = connector;
    }
    
    private String jsonFromConnector(final AttributeConnector connector) {
        if (null == connector) {
            return null;
        }

        String connectorJson;
        try {
            connectorJson = OBJECT_MAPPER.writeValueAsString(connector);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return connectorJson;
    }

    private AttributeConnector connectorFromJson(final String connectorJson) {
        if (null == connectorJson) {
            return null;
        }

        AttributeConnector connector;
        try {
            connector = OBJECT_MAPPER.readValue(connectorJson, AttributeConnector.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return connector;
    }
}
