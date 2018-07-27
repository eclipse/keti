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

package db.postgres

import org.eclipse.keti.acs.zone.management.dao.ZoneClientEntity
import org.flywaydb.core.api.migration.spring.SpringJdbcMigration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import java.util.HashSet

//Naming convention for the class name is being enforced by spring
class V2_0_1__InitializeIdentityZones : SpringJdbcMigration {

    @Throws(Exception::class)
    override fun migrate(jdbcTemplate: JdbcTemplate) {
        val acsAuthorizationZoneId = createDefaultAuthzZone(jdbcTemplate)
        /*
         * Let's find all the possible OAuth issuer_id and client_id combinations that exist in the old database
         * schema.
         */
        val existingOAuthClients = findExistingOAuthClients(jdbcTemplate)
        addOAuthClientsToDefaultAuthzZone(jdbcTemplate, acsAuthorizationZoneId, existingOAuthClients)
        dropConstraintsAndColumns(jdbcTemplate)
        removeDuplicateRows(jdbcTemplate, acsAuthorizationZoneId)
    }

    private fun createDefaultAuthzZone(jdbcTemplate: JdbcTemplate): Long {
        val insertZoneSql = "INSERT INTO authorization_zone (name, description, subdomain) " + "VALUES (?,?,?)"
        val holder = GeneratedKeyHolder()

        jdbcTemplate.update({ connection ->
            val ps = connection.prepareStatement(insertZoneSql, arrayOf("id"))
            ps.setString(1, "apm-migrated")
            ps.setString(2, "APM Migrated Zone from mvp1")
            ps.setString(3, "apm-migrated")
            ps
        }, holder)

        return holder.key.toLong()
    }

    private fun findExistingOAuthClients(jdbcTemplate: JdbcTemplate): Set<ZoneClientEntity> {
        val oauthClients = HashSet<ZoneClientEntity>()
        val subjectOAuthClients = jdbcTemplate
            .query("SELECT DISTINCT issuer_id, client_id FROM subject", ZoneClientRowMapper())
        oauthClients.addAll(subjectOAuthClients)

        val resourceOAuthClients = jdbcTemplate
            .query("SELECT DISTINCT issuer_id, client_id FROM resource", ZoneClientRowMapper())
        oauthClients.addAll(resourceOAuthClients)

        val policySetOAuthClients = jdbcTemplate
            .query("SELECT DISTINCT issuer_id, client_id FROM policy_set", ZoneClientRowMapper())
        oauthClients.addAll(policySetOAuthClients)
        return oauthClients
    }

    private fun addOAuthClientsToDefaultAuthzZone(
        jdbcTemplate: JdbcTemplate,
        acsAuthorizationZoneId: Long?,
        existingOAuthClients: Set<ZoneClientEntity>
    ) {

        for (oauthClient in existingOAuthClients) {
            jdbcTemplate.update(
                "INSERT INTO authorization_zone_client (issuer_id, client_id," + " authorization_zone_id) VALUES (?,?,?)",
                java.lang.Long.valueOf(
                    oauthClient.issuer!!
                        .issuerId!!
                ),
                oauthClient.clientId,
                acsAuthorizationZoneId
            )
        }
    }

    private fun dropConstraintsAndColumns(jdbcTemplate: JdbcTemplate) {
        jdbcTemplate.execute("ALTER TABLE resource DROP CONSTRAINT unique_issuer_client_resource_identifier;")
        jdbcTemplate.execute("ALTER TABLE resource DROP COLUMN issuer_id;")
        jdbcTemplate.execute("ALTER TABLE resource DROP COLUMN client_id;")
        jdbcTemplate.execute("ALTER TABLE subject DROP CONSTRAINT unique_issuer_client_subject_identifier;")
        jdbcTemplate.execute("ALTER TABLE subject DROP COLUMN issuer_id;")
        jdbcTemplate.execute("ALTER TABLE subject DROP COLUMN client_id;")
        jdbcTemplate.execute("ALTER TABLE policy_set DROP CONSTRAINT unique_issuer_client_pset;")
        jdbcTemplate.execute("ALTER TABLE policy_set DROP COLUMN issuer_id;")
        jdbcTemplate.execute("ALTER TABLE policy_set DROP COLUMN client_id;")
    }

    private fun removeDuplicateRows(
        jdbcTemplate: JdbcTemplate,
        zone: Long?
    ) {
        val subjects = jdbcTemplate
            .query("SELECT DISTINCT subject_identifier, attributes FROM subject", SubjectRowMapper())
        jdbcTemplate.update("DELETE FROM subject *")
        for (s in subjects) {
            jdbcTemplate.update(
                "INSERT INTO subject (subject_identifier, attributes, " + " authorization_zone_id) VALUES (?,?,?)",
                s.subjectIdentifier,
                s
                    .attributesAsJson,
                zone
            )
        }
        val resources = jdbcTemplate
            .query("SELECT DISTINCT resource_identifier, attributes FROM resource", ResourceRowMapper())
        jdbcTemplate.update("DELETE FROM resource *")
        for (r in resources) {
            jdbcTemplate.update(
                "INSERT INTO resource (resource_identifier, attributes, " + " authorization_zone_id) VALUES (?,?,?)",
                r.resourceIdentifier,
                r
                    .attributesAsJson,
                zone
            )
        }

        val policysets = jdbcTemplate
            .query("SELECT DISTINCT policy_set_id, policy_set_json FROM policy_set", PolicySetRowMapper())
        jdbcTemplate.update("DELETE FROM policy_set *")
        for (ps in policysets) {
            val row = jdbcTemplate
                .queryForRowSet("SELECT * FROM policy_set WHERE policy_set_id =?", ps.policySetId)
            if (row.next()) {
                jdbcTemplate.update(
                    "UPDATE policy_set SET policy_set_json = ? WHERE policy_set_id = ?",
                    ps.policySetJson, ps.policySetId
                )
            } else {
                jdbcTemplate.update(
                    "INSERT INTO policy_set (policy_set_id, policy_set_json, " + " authorization_zone_id) VALUES (?,?,?)",
                    ps.policySetId,
                    ps.policySetJson,
                    zone
                )
            }
        }
    }
}
