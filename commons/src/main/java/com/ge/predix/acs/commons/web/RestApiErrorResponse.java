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

package com.ge.predix.acs.commons.web;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Used to provide error payload with code and message.
 *
 * @author acs-engineers@ge.com
 *
 */
@SuppressWarnings({ "nls", "javadoc" })
public class RestApiErrorResponse {



    private ErrorDetails errorDetails = new ErrorDetails();

    public void setErrorDetails(final ErrorDetails errorDetails) {
        this.errorDetails = errorDetails;
    }
    @JsonProperty("ErrorDetails")
    public ErrorDetails getErrorDetails() {
        return this.errorDetails;
    }

    class ErrorDetails {

        private String errorCode = "FAILED";
        private String errorMessage = "Operation Failed";

        public String getErrorCode() {
            return this.errorCode;
        }

        public void setErrorCode(final String errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return this.errorMessage;
        }

        public void setErrorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
        }

    }

}
