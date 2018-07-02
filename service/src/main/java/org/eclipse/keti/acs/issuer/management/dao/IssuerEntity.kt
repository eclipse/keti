/*******************************************************************************
 * Copyright 2017 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.issuer.management.dao

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.UniqueConstraint

/**
 *
 * This class is no longer used except in migration logic.
 *
 */
@Entity
@Table(
    name = "issuer",
    uniqueConstraints = [(UniqueConstraint(columnNames = arrayOf("issuer_id"))), (UniqueConstraint(columnNames = arrayOf("issuer_check_token_url")))]
)
@Deprecated("")
class IssuerEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    /**
     * This is the canonical identifier for the issuer, which is available in a OAUTH token.
     *
     * @return
     */
    @Column(name = "issuer_id", nullable = false)
    var issuerId: String? = null

    /**
     * URL provided by this issuer to validate OAUTH tokens.
     *
     * @return
     */
    @Column(name = "issuer_check_token_url", nullable = false)
    private var issuerCheckTokenUrl: String? = null

    constructor()

    constructor(issuerId: String, issuerCheckTokenUrl: String) : super() {
        this.issuerId = issuerId
        this.issuerCheckTokenUrl = issuerCheckTokenUrl
    }

    override fun hashCode(): Int {
        return HashCodeBuilder().append(this.issuerId).append(this.issuerCheckTokenUrl).toHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is IssuerEntity) {
            val that = other as IssuerEntity?
            return EqualsBuilder().append(this.issuerId, that?.issuerId)
                .append(this.issuerCheckTokenUrl, that?.issuerCheckTokenUrl).isEquals
        }
        return false
    }

    override fun toString(): String {
        return ("IssuerEntity [id=" + this.id + ", issuerId=" + this.issuerId + ", issuerCheckTokenUrl="
                + this.issuerCheckTokenUrl + "]")
    }
}
