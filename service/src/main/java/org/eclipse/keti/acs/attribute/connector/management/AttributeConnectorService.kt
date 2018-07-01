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

package org.eclipse.keti.acs.attribute.connector.management

import org.eclipse.keti.acs.rest.AttributeConnector

interface AttributeConnectorService {

    val resourceAttributeConnector: AttributeConnector?

    val isResourceAttributeConnectorConfigured: Boolean

    val subjectAttributeConnector: AttributeConnector?

    val isSubjectAttributeConnectorConfigured: Boolean

    fun upsertResourceConnector(connector: AttributeConnector?): Boolean

    fun retrieveResourceConnector(): AttributeConnector?

    fun deleteResourceConnector(): Boolean

    fun upsertSubjectConnector(connector: AttributeConnector?): Boolean

    fun retrieveSubjectConnector(): AttributeConnector?

    fun deleteSubjectConnector(): Boolean
}
