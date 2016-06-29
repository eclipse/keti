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

import com.ge.predix.acs.issuer.management.dao.IssuerEntity;

/**
 *
 *
 * This class is no longer used except in migration logic.
 *
 */
@Entity
@Table(
        name = "authorization_zone_client",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "issuer_id", "client_id", "authorization_zone_id" }) })
@Deprecated
public class ZoneClientEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "issuer_id", referencedColumnName = "id", nullable = false, updatable = false)
    private IssuerEntity issuer;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "authorization_zone_id")
    private ZoneEntity zone;

    public long getId() {
        return this.id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public IssuerEntity getIssuer() {
        return this.issuer;
    }

    public void setIssuer(final IssuerEntity issuer) {
        this.issuer = issuer;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public ZoneEntity getZone() {
        return this.zone;
    }

    public void setZone(final ZoneEntity zone) {
        this.zone = zone;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.clientId).append(this.issuer).append(this.zone).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ZoneClientEntity) {
            final ZoneClientEntity other = (ZoneClientEntity) obj;
            return new EqualsBuilder().append(this.clientId, other.clientId).append(this.issuer, other.issuer)
                    .append(this.zone, other.zone).isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return "ZoneClientEntity [id=" + this.id + ", issuer=" + this.issuer + ", clientId=" + this.clientId
                + ", zoneName=" + this.zone.getName()
                /*
                 * Note: do not iterate the zone object, or will cause stackoverflow error
                 */
                + "]";
    }

}
