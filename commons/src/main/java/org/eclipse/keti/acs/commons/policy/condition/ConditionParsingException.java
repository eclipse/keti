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

package org.eclipse.keti.acs.commons.policy.condition;

/**
 * A condition shell may throw this exception when parsing a condition script if it is invalid or has other errors
 * that result in a script compilation failure.
 *
 * @author acs-engineers@ge.com
 */
public class ConditionParsingException extends Exception {
    /**
     * Serialization id.
     */
    private static final long serialVersionUID = 2112986552966674621L;

    private final String failedScript;

    public ConditionParsingException(final String message, final String failedScript, final Throwable cause) {
        super(message, cause);
        this.failedScript = failedScript;
    }

    public String getFailedScript() {
        return this.failedScript;
    }

}
