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

package org.eclipse.keti.acs.config

import org.springframework.core.env.Environment

class LocalRedisProperties(
    private val env: Environment,
    cacheType: String
) {

    val redisHost: String = this.env.getProperty(cacheType + "_REDIS_HOST", String::class.java, "localhost")
    val redisPort: Int = this.env.getProperty(cacheType + "_REDIS_PORT", Int::class.java, 6379)
    val minActive: Int = this.env.getProperty(cacheType + "_REDIS_MIN_ACTIVE", Int::class.java, 0)
    val maxActive: Int = this.env.getProperty(cacheType + "_REDIS_MAX_ACTIVE", Int::class.java, 100)
    val maxWaitTime: Int = this.env.getProperty(cacheType + "_REDIS_MAX_WAIT_TIME", Int::class.java, 2000)
    val soTimeout: Int = this.env.getProperty(cacheType + "_REDIS_SOCKET_TIMEOUT", Int::class.java, 3000)
}
