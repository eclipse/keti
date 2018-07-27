/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.test

import org.slf4j.LoggerFactory
import org.testng.IInvokedMethod
import org.testng.IInvokedMethodListener
import org.testng.ITestResult

private val LOGGER = LoggerFactory.getLogger(TestNameLogger::class.java)

private enum class TestStatus constructor(val value: String) {

    STARTING("Starting"),
    FINISHING("Finishing"),
    SKIPPING("Skipping"),
    ERRORED_OUT("Errored out on");

    override fun toString(): String {
        return value
    }
}

private fun logInvocation(
    testStatus: TestStatus,
    method: IInvokedMethod
) {
    val iTestNGMethod = method.testMethod
    val methodName = iTestNGMethod.testClass.name + '#'.toString() + iTestNGMethod.methodName

    var methodType = "test"
    if (method.isConfigurationMethod) {
        methodType = "configuration"
    }

    LOGGER.info(
        "{} {} {} method: {}", if (testStatus == TestStatus.ERRORED_OUT) "!!!" else "===", testStatus,
        methodType, methodName
    )
}

class TestNameLogger : IInvokedMethodListener {
    override fun beforeInvocation(
        method: IInvokedMethod,
        testResult: ITestResult
    ) {
        logInvocation(TestStatus.STARTING, method)
    }

    override fun afterInvocation(
        method: IInvokedMethod,
        testResult: ITestResult
    ) {
        logInvocation(
            if (testResult.status == ITestResult.FAILURE) TestStatus.ERRORED_OUT else TestStatus.FINISHING,
            method
        )
    }
}
