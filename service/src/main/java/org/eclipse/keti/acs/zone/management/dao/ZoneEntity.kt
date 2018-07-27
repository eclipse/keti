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

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder
import org.eclipse.keti.acs.privilege.management.dao.ResourceEntity
import org.eclipse.keti.acs.privilege.management.dao.SubjectEntity
import org.eclipse.keti.acs.rest.AttributeConnector
import org.eclipse.keti.acs.service.policy.admin.dao.PolicySetEntity
import java.io.IOException
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.UniqueConstraint

private val OBJECT_MAPPER = ObjectMapper()

@Entity
@Table(
    name = "authorization_zone",
    uniqueConstraints = [
        UniqueConstraint(columnNames = arrayOf("name")),
        UniqueConstraint(columnNames = arrayOf("subdomain"))
    ]
)
class ZoneEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "name", nullable = false, unique = true)
    var name: String? = null

    @Column(name = "description", nullable = false)
    var description: String? = null

    @Column(name = "subdomain", nullable = false, unique = true)
    var subdomain: String? = null

    @OneToMany(
        mappedBy = "zone",
        cascade = [(CascadeType.MERGE), (CascadeType.REFRESH), (CascadeType.REMOVE)],
        fetch = FetchType.LAZY
    )
    private val subjects: Set<SubjectEntity>? = null

    @OneToMany(
        mappedBy = "zone",
        cascade = [(CascadeType.MERGE), (CascadeType.REFRESH), (CascadeType.REMOVE)],
        fetch = FetchType.LAZY
    )
    private val resources: Set<ResourceEntity>? = null

    @OneToMany(
        mappedBy = "zone",
        cascade = [(CascadeType.MERGE), (CascadeType.REFRESH), (CascadeType.REMOVE)],
        fetch = FetchType.LAZY
    )
    private val policySets: Set<PolicySetEntity>? = null

    @Column(name = "resource_attribute_connector_json", nullable = true)
    private var resourceConnectorJson: String? = null

    @Column(name = "subject_attribute_connector_json", nullable = true)
    private var subjectConnectorJson: String? = null

    private var cachedResourceConnector: AttributeConnector? = null

    private var cachedSubjectConnector: AttributeConnector? = null

    var resourceAttributeConnector: AttributeConnector?
        get() {
            if (null == this.cachedResourceConnector) {
                this.cachedResourceConnector = connectorFromJson(this.resourceConnectorJson)
            }
            return this.cachedResourceConnector
        }
        set(connector) {
            this.resourceConnectorJson = jsonFromConnector(connector)
            this.cachedResourceConnector = connector
        }

    var subjectAttributeConnector: AttributeConnector?
        get() {
            if (null == this.cachedSubjectConnector) {
                this.cachedSubjectConnector = connectorFromJson(this.subjectConnectorJson)
            }
            return this.cachedSubjectConnector
        }
        set(connector) {
            this.subjectConnectorJson = jsonFromConnector(connector)
            this.cachedSubjectConnector = connector
        }

    constructor()

    constructor(id: Long?) {
        this.id = id!!
    }

    constructor(id: Long?, name: String) {
        this.id = id!!
        this.name = name
    }

    override fun hashCode(): Int {
        return HashCodeBuilder().append(this.name).toHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is ZoneEntity) {
            val that = other as ZoneEntity?
            return EqualsBuilder().append(this.name, that?.name).isEquals
        }
        return false
    }

    override fun toString(): String {
        return ("ZoneEntity [id=" + this.id + ", name=" + this.name + ", description=" + this.description
                + ", subdomain=" + this.subdomain + "]")
    }

    private fun jsonFromConnector(connector: AttributeConnector?): String? {
        if (null == connector) {
            return null
        }

        val connectorJson: String
        try {
            connectorJson = OBJECT_MAPPER.writeValueAsString(connector)
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }

        return connectorJson
    }

    private fun connectorFromJson(connectorJson: String?): AttributeConnector? {
        if (null == connectorJson) {
            return null
        }

        val connector: AttributeConnector
        try {
            connector = OBJECT_MAPPER.readValue(connectorJson, AttributeConnector::class.java)
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }

        return connector
    }
}
