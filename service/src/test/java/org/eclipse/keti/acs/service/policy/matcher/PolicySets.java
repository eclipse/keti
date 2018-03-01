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

package org.eclipse.keti.acs.service.policy.matcher;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.keti.acs.model.PolicySet;

/**
 *
 * @author acs-engineers@ge.com
 */
public final class PolicySets {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PolicySets() {
        // Prevents instantiation.
    }

    @SuppressWarnings("javadoc")
    public static PolicySet loadFromFile(final File file) throws IOException {
        return OBJECT_MAPPER.readValue(file, PolicySet.class);
    }
}
