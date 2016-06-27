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
package db.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.ge.predix.acs.privilege.management.dao.ResourceEntity;
import com.ge.predix.acs.privilege.management.dao.SubjectEntity;
import com.ge.predix.acs.service.policy.admin.dao.PolicySetEntity;
import com.ge.predix.acs.zone.management.dao.ZoneClientEntity;

//CHECKSTYLE:OFF
@SuppressWarnings("deprecation")
public class V2_0_1__InitializeIdentityZones implements SpringJdbcMigration {
    // CHECKSTYLE:ON

    @Override
    public void migrate(final JdbcTemplate jdbcTemplate) throws Exception {
        Long acsAuthorizationZoneId = createDefaultAuthzZone(jdbcTemplate);
        /*
         * Let's find all the possible OAuth issuer_id and client_id combinations that exist in the old database
         * schema.
         */
        Set<ZoneClientEntity> existingOAuthClients = findExistingOAuthClients(jdbcTemplate);
        addOAuthClientsToDefaultAuthzZone(jdbcTemplate, acsAuthorizationZoneId, existingOAuthClients);
        dropConstraintsAndColumns(jdbcTemplate);
        removeDuplicateRows(jdbcTemplate, acsAuthorizationZoneId);
    }

    private Long createDefaultAuthzZone(final JdbcTemplate jdbcTemplate) {
        final String insertZoneSql = "INSERT INTO authorization_zone (name, description, subdomain) "
                + "VALUES (?,?,?)";
        KeyHolder holder = new GeneratedKeyHolder();

        jdbcTemplate.update(new PreparedStatementCreator() {

            @Override
            public PreparedStatement createPreparedStatement(final Connection connection) throws SQLException {
                PreparedStatement ps = connection.prepareStatement(insertZoneSql, new String[] { "id" });
                ps.setString(1, "apm-migrated");
                ps.setString(2, "APM Migrated Zone from mvp1");
                ps.setString(3, "apm-migrated");
                return ps;
            }
        }, holder);

        Long acsAuthorizationZoneId = holder.getKey().longValue();
        return acsAuthorizationZoneId;
    }

    private Set<ZoneClientEntity> findExistingOAuthClients(final JdbcTemplate jdbcTemplate) {
        Set<ZoneClientEntity> oauthClients = new HashSet<>();
        List<ZoneClientEntity> subjectOAuthClients = jdbcTemplate
                .query("SELECT DISTINCT issuer_id, client_id FROM subject", new ZoneClientRowMapper());
        oauthClients.addAll(subjectOAuthClients);

        List<ZoneClientEntity> resourceOAuthClients = jdbcTemplate
                .query("SELECT DISTINCT issuer_id, client_id FROM resource", new ZoneClientRowMapper());
        oauthClients.addAll(resourceOAuthClients);

        List<ZoneClientEntity> policySetOAuthClients = jdbcTemplate
                .query("SELECT DISTINCT issuer_id, client_id FROM policy_set", new ZoneClientRowMapper());
        oauthClients.addAll(policySetOAuthClients);
        return oauthClients;
    }

    private void addOAuthClientsToDefaultAuthzZone(final JdbcTemplate jdbcTemplate, final Long acsAuthorizationZoneId,
            final Set<ZoneClientEntity> existingOAuthClients) {

        for (ZoneClientEntity oauthClient : existingOAuthClients) {
            jdbcTemplate.update(
                    "INSERT INTO authorization_zone_client (issuer_id, client_id,"
                            + " authorization_zone_id) VALUES (?,?,?)",
                    Long.valueOf(oauthClient.getIssuer().getIssuerId()), oauthClient.getClientId(),
                    acsAuthorizationZoneId);
        }
    }

    private void dropConstraintsAndColumns(final JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("ALTER TABLE resource DROP CONSTRAINT unique_issuer_client_resource_identifier;");
        jdbcTemplate.execute("ALTER TABLE resource DROP COLUMN issuer_id;");
        jdbcTemplate.execute("ALTER TABLE resource DROP COLUMN client_id;");
        jdbcTemplate.execute("ALTER TABLE subject DROP CONSTRAINT unique_issuer_client_subject_identifier;");
        jdbcTemplate.execute("ALTER TABLE subject DROP COLUMN issuer_id;");
        jdbcTemplate.execute("ALTER TABLE subject DROP COLUMN client_id;");
        jdbcTemplate.execute("ALTER TABLE policy_set DROP CONSTRAINT unique_issuer_client_pset;");
        jdbcTemplate.execute("ALTER TABLE policy_set DROP COLUMN issuer_id;");
        jdbcTemplate.execute("ALTER TABLE policy_set DROP COLUMN client_id;");
    }

    private void removeDuplicateRows(final JdbcTemplate jdbcTemplate, final Long zone) {
        final List<SubjectEntity> subjects = jdbcTemplate
                .query("SELECT DISTINCT subject_identifier, attributes FROM subject", new SubjectRowMapper());
        jdbcTemplate.update("DELETE FROM subject *");
        for (SubjectEntity s : subjects) {
            jdbcTemplate.update(
                    "INSERT INTO subject (subject_identifier, attributes, " + " authorization_zone_id) VALUES (?,?,?)",
                    s.getSubjectIdentifier(), s.getAttributesAsJson(), zone);
        }
        final List<ResourceEntity> resources = jdbcTemplate
                .query("SELECT DISTINCT resource_identifier, attributes FROM resource", new ResourceRowMapper());
        jdbcTemplate.update("DELETE FROM resource *");
        for (ResourceEntity r : resources) {
            jdbcTemplate.update(
                    "INSERT INTO resource (resource_identifier, attributes, "
                            + " authorization_zone_id) VALUES (?,?,?)",
                    r.getResourceIdentifier(), r.getAttributesAsJson(), zone);
        }

        final List<PolicySetEntity> policysets = jdbcTemplate
                .query("SELECT DISTINCT policy_set_id, policy_set_json FROM policy_set", new PolicySetRowMapper());
        jdbcTemplate.update("DELETE FROM policy_set *");
        for (PolicySetEntity ps : policysets) {
            SqlRowSet row = jdbcTemplate.queryForRowSet("SELECT * FROM policy_set WHERE policy_set_id =?",
                    ps.getPolicySetID());
            if (row.next()) {
                jdbcTemplate.update("UPDATE policy_set SET policy_set_json = ? WHERE policy_set_id = ?",
                        ps.getPolicySetJson(), ps.getPolicySetID());
            } else {
                jdbcTemplate.update(
                        "INSERT INTO policy_set (policy_set_id, policy_set_json, "
                                + " authorization_zone_id) VALUES (?,?,?)",
                        ps.getPolicySetID(), ps.getPolicySetJson(), zone);
            }
        }
    }
}
