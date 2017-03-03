package com.ge.predix.test;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IClass;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

public class TestNameLogger implements IInvokedMethodListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestNameLogger.class);

    private static void logInvocation(final String prefix, final IInvokedMethod method) {
        if (!method.isTestMethod()) {
            return;
        }

        ITestNGMethod iTestNGMethod = method.getTestMethod();
        IClass iClass = iTestNGMethod.getTestClass();
        String methodName = iClass.getName() + '#' + iTestNGMethod.getMethodName();

        if (StringUtils.isEmpty(methodName)) {
            return;
        }

        LOGGER.info("=== {} test: {}", prefix, methodName);
    }

    @Override
    public void beforeInvocation(final IInvokedMethod method, final ITestResult testResult) {
        logInvocation("Starting", method);
    }

    @Override
    public void afterInvocation(final IInvokedMethod method, final ITestResult testResult) {
        logInvocation("Finished", method);
    }
}
