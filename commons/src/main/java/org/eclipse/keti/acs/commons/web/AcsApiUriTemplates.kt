/*******************************************************************************
 * Copyright 2017 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.commons.web

/**
 * Constants that define the set of URI Templates of the resources exposed by the ACS.
 *
 * @author acs-engineers@ge.com
 */
const val V1 = "/v1"
const val POLICY_SETS_URL = "/policy-set"
const val POLICY_EVALUATION_URL = "/policy-evaluation"
const val POLICY_SET_URL = "$POLICY_SETS_URL/{policySetId}"
const val MANAGED_RESOURCES_URL = "/resource"
const val MANAGED_RESOURCE_URL = "$MANAGED_RESOURCES_URL/{resourceIdentifier}"
const val SUBJECTS_URL = "/subject"
const val SUBJECT_URL = "$SUBJECTS_URL/{subjectIdentifier:.+}"
const val MONITORING_URL = "/monitoring"
const val HEARTBEAT_URL = "$MONITORING_URL/heartbeat"
const val ZONE_URL = "/zone/{zoneName}"
const val CONNECTOR_URL = "/connector"
const val RESOURCE_CONNECTOR_URL = "$CONNECTOR_URL/resource"
const val SUBJECT_CONNECTOR_URL = "$CONNECTOR_URL/subject"
const val HEALTH_URL = "/health"
