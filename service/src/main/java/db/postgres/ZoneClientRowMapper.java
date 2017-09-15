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
 *******************************************************************************/

package db.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.ge.predix.acs.issuer.management.dao.IssuerEntity;
import com.ge.predix.acs.zone.management.dao.ZoneClientEntity;

@SuppressWarnings("deprecation")
public class ZoneClientRowMapper implements RowMapper<ZoneClientEntity> {

    @Override
    public ZoneClientEntity mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        ZoneClientEntity zoneClient = new ZoneClientEntity();
        zoneClient.setClientId(rs.getString("client_id"));
        IssuerEntity issuer = new IssuerEntity();
        issuer.setIssuerId(rs.getString("issuer_id"));
        zoneClient.setIssuer(issuer);
        return zoneClient;
    }

}
