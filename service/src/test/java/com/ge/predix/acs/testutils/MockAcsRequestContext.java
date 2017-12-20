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

package com.ge.predix.acs.testutils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ge.predix.acs.request.context.AcsRequestContextHolder;

public final class MockAcsRequestContext {
    private MockAcsRequestContext() {
        // Prevents instantiation.
    }

    public static void mockAcsRequestContext() {
        try {
            Method method = AcsRequestContextHolder.class.getDeclaredMethod("initialize");
            method.setAccessible(true);
            Object nullObj = null;
            Object[] nullArgs = null;
            method.invoke(nullObj, nullArgs);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            Throwable cause = e.getCause();
            cause.printStackTrace();
            // System.err.format("drinkMe() failed: %s%n", cause.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Test Set up Failed.");
        }
    }
}
