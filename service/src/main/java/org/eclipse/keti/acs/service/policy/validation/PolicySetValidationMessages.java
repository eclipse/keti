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

package org.eclipse.keti.acs.service.policy.validation;

/**
 * @author acs-engineers@ge.com
 *
 */
public final class PolicySetValidationMessages {

    private PolicySetValidationMessages() {
    }

    public static final String POLICY_VAL_FAILED = "Policy validation failed.";

    public static final String OBL_VAL_FAILED = "Obligation Expression validation failed.";

    public static final String EXCEP_OBL_ACTION_ARG_DUPLICATED = OBL_VAL_FAILED
            + " Names of actionArguments have to be unique."
            + " Obligation expression [%s] contains the following repeated actionArguments: [%s].";

    public static final String EXCEP_OBL_ACTION_TEMPLATE_NULL_EMPTY = OBL_VAL_FAILED
            + " actionTemplate in obligation expression [%s] cannot be null or empty.";

    public static final String EXCEP_OBL_EXPRESSIONS_DUPLICATED = OBL_VAL_FAILED
            + " Id of obligation expression has to be unique. The following ids are repeated:  [%s]";

    public static final String EXCEP_POLICY_OBL_IDS_NOT_FOUND = POLICY_VAL_FAILED
            + " Unable to find matching obligation expression for the following obligationsIds in policy [%s]: [%s].";
}
