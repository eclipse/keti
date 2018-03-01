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

package org.eclipse.keti.acs.service.policy.validation;

/**
 *
 * @author acs-engineers@ge.com
 */
@SuppressWarnings("javadoc")
public class PolicySetValidationException extends RuntimeException {

    private static final long serialVersionUID = -2434141848064977230L;

    public PolicySetValidationException() {
        super();
    }

    public PolicySetValidationException(final String message, final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public PolicySetValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public PolicySetValidationException(final String message) {
        super(message);
    }

    public PolicySetValidationException(final Throwable cause) {
        super(cause);
    }

}
