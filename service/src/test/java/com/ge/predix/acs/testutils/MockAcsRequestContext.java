package com.ge.predix.acs.testutils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ge.predix.acs.request.context.AcsRequestContextHolder;
import com.ge.predix.acs.rest.Zone;

public final class MockAcsRequestContext {
    private MockAcsRequestContext() {
        // Prevents instantiation.
    }

    public static void mockAcsRequestContext(final Zone zone) {
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
