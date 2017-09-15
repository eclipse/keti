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

package com.ge.predix.acs.attribute.connector.management;

import com.ge.predix.acs.rest.AttributeConnector;

public interface AttributeConnectorService {

    boolean upsertResourceConnector(AttributeConnector connector);

    AttributeConnector retrieveResourceConnector();

    boolean deleteResourceConnector();

    AttributeConnector getResourceAttributeConnector();

    boolean isResourceAttributeConnectorConfigured();

    boolean upsertSubjectConnector(AttributeConnector connector);

    AttributeConnector retrieveSubjectConnector();

    boolean deleteSubjectConnector();

    AttributeConnector getSubjectAttributeConnector();

    boolean isSubjectAttributeConnectorConfigured();
}
