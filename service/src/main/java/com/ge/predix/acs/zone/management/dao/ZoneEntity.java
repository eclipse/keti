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

import com.ge.predix.acs.privilege.management.dao.ResourceEntity;
import com.ge.predix.acs.privilege.management.dao.SubjectEntity;
import com.ge.predix.acs.service.policy.admin.dao.PolicySetEntity;

@Entity
@Table(
        name = "authorization_zone",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }),
                @UniqueConstraint(columnNames = { "subdomain" }) })
public class ZoneEntity {

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
}
