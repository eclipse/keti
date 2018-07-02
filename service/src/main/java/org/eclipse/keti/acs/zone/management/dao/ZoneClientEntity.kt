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

package org.eclipse.keti.acs.zone.management.dao

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder
import org.eclipse.keti.acs.issuer.management.dao.IssuerEntity
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

/**
 *
 *
 * This class is no longer used except in migration logic.
 *
 */
@Entity
@Table(
    name = "authorization_zone_client",
    uniqueConstraints = [(UniqueConstraint(
        columnNames = arrayOf(
            "issuer_id",
            "client_id",
            "authorization_zone_id"
        )
    ))]
)
@Deprecated("")
class ZoneClientEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(optional = false)
    @JoinColumn(name = "issuer_id", referencedColumnName = "id", nullable = false, updatable = false)
    var issuer: IssuerEntity? = null

    @Column(name = "client_id", nullable = false)
    var clientId: String? = null

    @ManyToOne(optional = false)
    @JoinColumn(name = "authorization_zone_id")
    var zone: ZoneEntity? = null

    override fun hashCode(): Int {
        return HashCodeBuilder().append(this.clientId).append(this.issuer).append(this.zone).toHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is ZoneClientEntity) {
            val that = other as ZoneClientEntity?
            return EqualsBuilder().append(this.clientId, that?.clientId).append(this.issuer, that?.issuer)
                .append(this.zone, that?.zone).isEquals
        }
        return false
    }

    override fun toString(): String {
        return ("ZoneClientEntity [id=" + this.id + ", issuer=" + this.issuer + ", clientId=" + this.clientId
                + ", zoneName=" + this.zone!!.name
                /*
                 * Note: do not iterate the zone object, or will cause stackoverflow error
                 */
                + "]")
    }
}
