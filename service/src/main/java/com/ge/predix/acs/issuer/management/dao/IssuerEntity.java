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

package com.ge.predix.acs.issuer.management.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 *
 * This class is no longer used except in migration logic.
 *
 */
@Entity
@Table(
        name = "issuer",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "issuer_id" }),
                @UniqueConstraint(columnNames = { "issuer_check_token_url" }) })
@Deprecated
public class IssuerEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "issuer_id", nullable = false)
    private String issuerId;

    @Column(name = "issuer_check_token_url", nullable = false)
    private String issuerCheckTokenUrl;

    public IssuerEntity() {
    }

    public IssuerEntity(final String issuerId, final String issuerCheckTokenUrl) {
        super();
        this.issuerId = issuerId;
        this.issuerCheckTokenUrl = issuerCheckTokenUrl;
    }

    public long getId() {
        return this.id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    /**
     * This is the canonical identifier for the issuer, which is available in a OAUTH token.
     *
     * @return
     */
    public String getIssuerId() {
        return this.issuerId;
    }

    public void setIssuerId(final String issuerId) {
        this.issuerId = issuerId;
    }

    /**
     * URL provided by this issuer to validate OAUTH tokens.
     *
     * @return
     */
    public String getIssuerCheckTokenUrl() {
        return this.issuerCheckTokenUrl;
    }

    public void setIssuerCheckTokenUrl(final String issuerCheckTokenUrl) {
        this.issuerCheckTokenUrl = issuerCheckTokenUrl;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.issuerId).append(this.issuerCheckTokenUrl).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof IssuerEntity) {
            final IssuerEntity other = (IssuerEntity) obj;
            return new EqualsBuilder().append(this.issuerId, other.issuerId)
                    .append(this.issuerCheckTokenUrl, other.issuerCheckTokenUrl).isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return "IssuerEntity [id=" + this.id + ", issuerId=" + this.issuerId + ", issuerCheckTokenUrl="
                + this.issuerCheckTokenUrl + "]";
    }

}
