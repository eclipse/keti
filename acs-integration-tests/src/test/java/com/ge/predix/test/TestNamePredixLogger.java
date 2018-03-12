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
 *******************************************************************************/

package com.ge.predix.test;

import org.eclipse.keti.test.TestNameLogger;
import org.testng.IInvokedMethod;
import org.testng.ITestResult;
import org.testng.SkipException;

import com.ge.predix.acs.cloudfoundry.DeleteApplications;

/**
 * @author Voolla Sandeep Kumar
 */
public class TestNamePredixLogger extends TestNameLogger {

    static final String ACS_CLOUD_FOUNDRY_PACKAGE = "com.ge.predix.acs.cloudfoundry";

    private static volatile boolean suiteFailed = false;

    private static boolean skipTest(final IInvokedMethod method) {
        return (suiteFailed && !DeleteApplications.class
                .isAssignableFrom(method.getTestMethod().getTestClass().getRealClass()));
    }

    @Override
    public void beforeInvocation(final IInvokedMethod method, final ITestResult testResult) {
        // Skip the test if the suite has been marked as failing and the test being executed isn't a
        // Cloud-Foundry-related deletion operation (i.e. a normal integration test)
        if (skipTest(method)) {
            throw new SkipException("Test skipped due to a failure detected in the suite");
        }

        super.beforeInvocation(method, testResult);
    }

    @Override
    public void afterInvocation(final IInvokedMethod method, final ITestResult testResult) {
        // Mark a suite as failing only when a Cloud-Foundry-related push operation fails
        if (!testResult.isSuccess() && method.getTestMethod().getTestClass().getRealClass().getPackage().getName()
                .contains(ACS_CLOUD_FOUNDRY_PACKAGE)) {
            suiteFailed = true;
            logInvocation(TestStatus.ERRORED_OUT, method);
            return;
        }

        if (skipTest(method)) {
            logInvocation(TestStatus.SKIPPING, method);
            return;
        }

        super.afterInvocation(method, testResult);
    }

}
