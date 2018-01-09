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

package com.ge.predix.acs.service.policy.admin.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.ge.predix.acs.zone.management.dao.ZoneEntity;

/**
 *
 * @author acs-engineers@ge.com
 */
@SuppressWarnings({ "nls", "javadoc" })
@Entity
@Table(
        name = "policy_set",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "authorization_zone_id", "policy_set_id" }) })
public class PolicySetEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "authorization_zone_id", referencedColumnName = "id", nullable = false, updatable = false)
    private ZoneEntity zone;

    /** ID unique per client-id and issuer combination. */
    @Column(name = "policy_set_id", nullable = false)
    private String policySetId;

    /** Clob representing the JSON policy set. */
    @Column(name = "policy_set_json", columnDefinition = "CLOB NOT NULL")
    private String policySetJson;

    public PolicySetEntity(final ZoneEntity zone, final String policySetID, final String policySetJson) {
        super();
        this.zone = zone;
        this.policySetId = policySetID;
        this.policySetJson = policySetJson;
    }

    public PolicySetEntity() {
        super();
    }

    public long getId() {
        return this.id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getPolicySetID() {
        return this.policySetId;
    }

    public String getPolicySetJson() {
        return this.policySetJson;
    }

    @Override
    public String toString() {
        return "PolicySetEntity [id=" + this.id + ", zone=" + this.zone + ", policySetId=" + this.policySetId
                + ", policySetJson=" + this.policySetJson + "]";
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.zone).append(this.policySetId).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {

        if (obj instanceof PolicySetEntity) {
            final PolicySetEntity other = (PolicySetEntity) obj;
            return new EqualsBuilder().append(this.zone, other.zone).append(this.policySetId, other.policySetId)
                    .isEquals();
        }
        return false;
    }

}
