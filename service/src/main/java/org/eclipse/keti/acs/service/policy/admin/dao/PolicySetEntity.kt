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

package org.eclipse.keti.acs.service.policy.admin.dao

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
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
 * @author acs-engineers@ge.com
 */
@Entity
@Table(
    name = "policy_set",
    uniqueConstraints = [(UniqueConstraint(columnNames = arrayOf("authorization_zone_id", "policy_set_id")))]
)
class PolicySetEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(optional = false)
    @JoinColumn(name = "authorization_zone_id", referencedColumnName = "id", nullable = false, updatable = false)
    private var zone: ZoneEntity? = null

    /** ID unique per client-id and issuer combination.  */
    @Column(name = "policy_set_id", nullable = false)
    var policySetId: String? = null

    /** Clob representing the JSON policy set.  */
    @Column(name = "policy_set_json", columnDefinition = "CLOB NOT NULL")
    var policySetJson: String? = null

    constructor(zone: ZoneEntity?, policySetId: String?, policySetJson: String?) : super() {
        this.zone = zone
        this.policySetId = policySetId
        this.policySetJson = policySetJson
    }

    constructor() : super()

    override fun toString(): String {
        return ("PolicySetEntity [id=" + this.id + ", zone=" + this.zone + ", policySetId=" + this.policySetId
                + ", policySetJson=" + this.policySetJson + "]")
    }

    override fun hashCode(): Int {
        return HashCodeBuilder().append(this.zone).append(this.policySetId).toHashCode()
    }

    override fun equals(other: Any?): Boolean {

        if (other is PolicySetEntity) {
            val that = other as PolicySetEntity?
            return EqualsBuilder().append(this.zone, that?.zone).append(this.policySetId, that?.policySetId)
                .isEquals
        }
        return false
    }
}
