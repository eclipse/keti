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

package com.ge.predix.integration.test;

import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;

public final class SubjectResourceFixture {
    
    private SubjectResourceFixture() {
        //not called
        throw new IllegalAccessError("Class is non-instantiable");
     }

    public static final BaseSubject MARISSA_V1 = new BaseSubject("marissa");
    public static final BaseSubject BOB_V1 = new BaseSubject("GE/bob");
    public static final BaseSubject JOE_V1 = new BaseSubject("joe@gmail.com");
    public static final BaseSubject PETE_V1 = new BaseSubject("pete@gmail.com");
    public static final BaseSubject JLO_V1 = new BaseSubject("123412341324");

    public static final BaseResource SANRAMON = new BaseResource("/sites/sanramon/");

}
