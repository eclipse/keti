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
package com.ge.predix.acs.commons.web;

import javax.annotation.Resource;

import org.springframework.core.env.Environment;

/**
 * Base class for REST Apis that provides common error handler.
 *
 * @author 212360328
 *
 */
public class BaseRestApi {
    public static final String PARENTS_ATTR_NOT_SUPPORTED_MSG = "The parents attribute isn't supported yet";

    @Resource
    private Environment environment;

    public Environment getEnvironment() {
        return this.environment;
    }

}
