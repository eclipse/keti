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

package org.eclipse.keti.acs.attribute.readers;

import java.util.Set;

import org.eclipse.keti.acs.attribute.cache.AttributeCache;
import org.eclipse.keti.acs.attribute.connector.management.AttributeConnectorService;
import org.eclipse.keti.acs.rest.AttributeAdapterConnection;

public class ExternalResourceAttributeReader extends ExternalAttributeReader implements ResourceAttributeReader {

    public ExternalResourceAttributeReader(final AttributeConnectorService connectorService,
            final AttributeCache resourceAttributeCache, final int adapterTimeoutMillis) {
        super(connectorService, resourceAttributeCache, adapterTimeoutMillis);
    }

    @Override
    Set<AttributeAdapterConnection> getAttributeAdapterConnections() {
        return this.getConnectorService().getResourceAttributeConnector().getAdapters();
    }
}
