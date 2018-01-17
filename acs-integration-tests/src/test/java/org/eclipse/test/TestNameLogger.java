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

package org.eclipse.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

public class TestNameLogger implements IInvokedMethodListener {
    protected enum TestStatus {

        STARTING("Starting"),
        FINISHING("Finishing"),
        SKIPPING("Skipping"),
        ERRORED_OUT("Errored out on");

        private final String name;

        TestStatus(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TestNameLogger.class);

    protected static void logInvocation(final TestStatus testStatus, final IInvokedMethod method) {
        ITestNGMethod iTestNGMethod = method.getTestMethod();
        String methodName = iTestNGMethod.getTestClass().getName() + '#' + iTestNGMethod.getMethodName();

        String methodType = "test";
        if (method.isConfigurationMethod()) {
            methodType = "configuration";
        }

        LOGGER.info("{} {} {} method: {}", (testStatus == TestStatus.ERRORED_OUT ? "!!!" : "==="), testStatus,
                methodType, methodName);
    }

    @Override
    public void beforeInvocation(final IInvokedMethod method, final ITestResult testResult) {
        logInvocation(TestStatus.STARTING, method);
    }

    @Override
    public void afterInvocation(final IInvokedMethod method, final ITestResult testResult) {
        logInvocation((testResult.getStatus() == ITestResult.FAILURE ? TestStatus.ERRORED_OUT : TestStatus.FINISHING),
                method);
    }
}
