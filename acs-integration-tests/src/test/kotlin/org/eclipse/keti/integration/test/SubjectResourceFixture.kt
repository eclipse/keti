/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.integration.test

import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject

val MARISSA_V1 = BaseSubject("marissa")
val BOB_V1 = BaseSubject("GE/bob")
val JOE_V1 = BaseSubject("joe@gmail.com")
val PETE_V1 = BaseSubject("pete@gmail.com")
val JLO_V1 = BaseSubject("123412341324")

val SANRAMON = BaseResource("/sites/sanramon/")
