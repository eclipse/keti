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

package com.ge.predix.acs.commons.policy.condition;

/**
 * Represents a shell capable of compiling a policy condition.
 *
 * @author acs-engineers@ge.com
 */
@FunctionalInterface
public interface ConditionShell {

    /**
     * Validates the script & generates condition script object.
     *
     * @param script
     *            the policy condition string
     * @return a Script object instance capable of executing the policy condition.
     * @throws ConditionParsingException
     *             on validation error
     */
    ConditionScript parse(String script) throws ConditionParsingException;
}
