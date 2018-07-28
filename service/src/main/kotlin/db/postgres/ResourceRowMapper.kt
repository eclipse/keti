/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package db.postgres

import org.eclipse.keti.acs.privilege.management.dao.ResourceEntity
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.sql.SQLException

class ResourceRowMapper : RowMapper<ResourceEntity> {

    @Throws(SQLException::class)
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int
    ): ResourceEntity {
        val resource = ResourceEntity()
        resource.resourceIdentifier = rs.getString("resource_identifier")
        resource.attributesAsJson = rs.getString("attributes")
        return resource
    }
}
