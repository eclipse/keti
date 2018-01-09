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
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package com.ge.predix.acs.attribute.readers;

import java.util.Set;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.attribute.connector.management.AttributeConnectorService;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.AttributeAdapterConnection;

public class ExternalSubjectAttributeReader extends ExternalAttributeReader implements SubjectAttributeReader {

    public ExternalSubjectAttributeReader(final AttributeConnectorService connectorService,
            final AttributeCache subjectAttributeCache, final int adapterTimeoutMillis) {
        super(connectorService, subjectAttributeCache, adapterTimeoutMillis);
    }

    @Override
    Set<AttributeAdapterConnection> getAttributeAdapterConnections() {
        return this.getConnectorService().getSubjectAttributeConnector().getAdapters();
    }

    @Override
    public Set<Attribute> getAttributesByScope(final String subjectId, final Set<Attribute> scopes) {
        // Connectors have no notion of scoped attributes
        return this.getAttributes(subjectId);
    }
}
