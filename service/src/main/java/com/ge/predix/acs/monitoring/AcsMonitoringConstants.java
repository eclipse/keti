/*******************************************************************************
 * Copyright 2016 General Electric Company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.ge.predix.acs.monitoring;

/**
 *
 * @author 212360328
 */
public final class AcsMonitoringConstants {

    private AcsMonitoringConstants() {
    }

    public static final int SUCCESS_CHECK = 0;
    public static final int FAILED_CHECK = -1;

    public static final String DB_SUCCESS_STATUS = "SUCCESS";
    public static final String DB_FAILED_STATUS = "FAILED";
    public static final String DB_UNAVAILABLE_STATUS = "UNAVAILABLE";
    public static final String DB_UNREACHABLE_STATUS = "UNREACHABLE";
    public static final String DB_MISCONFIGURATION_STATUS = "MISCONFIGURATION";

    public static final String UAA_FAILED_STATUS = "FAILED";
    public static final String UAA_SUCCESS_STATUS = "SUCCESS";

    public static final String ACS_DB_OUT_OF_SERVICE = "ACS_DB_OUT_OF_SERVICE";
    public static final String ACS_DB_MIGRATION_INCOMPLETE = "ACS_DB_MIGRATION_INCOMPLETE";
    public static final String UAA_OUT_OF_SERVICE = "UAA_OUT_OF_SERVICE";

}
