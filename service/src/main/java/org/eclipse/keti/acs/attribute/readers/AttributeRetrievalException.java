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

import java.text.MessageFormat;

public class AttributeRetrievalException extends RuntimeException {

    public AttributeRetrievalException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public AttributeRetrievalException(final String message) {
        super(message);
    }

    static String getAdapterErrorMessage(final String adapterEndpoint) {
        return MessageFormat.format("Couldn''t get attributes from the adapter with endpoint ''{0}''", adapterEndpoint);
    }

    static String getStorageErrorMessage(final String id) {
        return String.format("Total size of attributes or number of attributes too large for id: '%s'", id);
    }
}
