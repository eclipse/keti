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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.testng.IAnnotationTransformer2;
import org.testng.annotations.IConfigurationAnnotation;
import org.testng.annotations.IDataProviderAnnotation;
import org.testng.annotations.IFactoryAnnotation;
import org.testng.annotations.ITestAnnotation;
import org.testng.annotations.ITestOrConfiguration;

import com.ge.predix.acs.cloudfoundry.AcsCloudFoundryUtilities;

public class TestAnnotationTransformer implements IAnnotationTransformer2 {

    public static final String INTEGRATION_TEST_GROUP = "integrationTests";

    private static void transformHelper(final ITestOrConfiguration annotation,
            final Class testClass, final Constructor testConstructor, final Method testMethod) {

        boolean cloudFoundryTestClass = (testClass != null
                && testClass.getPackage().getName().contains(TestNameLogger.ACS_CLOUD_FOUNDRY_PACKAGE));
        boolean cloudFoundryTestConstructor = (testConstructor != null
                && testConstructor.getDeclaringClass().getPackage().getName().contains(
                        TestNameLogger.ACS_CLOUD_FOUNDRY_PACKAGE));
        boolean cloudFoundryTestMethod = (testMethod != null
                && testMethod.getDeclaringClass().getPackage().getName().contains(
                        TestNameLogger.ACS_CLOUD_FOUNDRY_PACKAGE));

        // For all integration tests, ensure all configuration and test classes/constructors/methods:
        //   - Depend on test applications being successfully pushed to Cloud Foundry
        //   - Are all added to the same group so that methods that delete test applications from Cloud Foundry
        //     start _after_ these tests have had a chance to run.
        if (!cloudFoundryTestClass && !cloudFoundryTestConstructor && !cloudFoundryTestMethod) {
            Set<String> dependsOnGroups = new HashSet<>(Arrays.asList(annotation.getDependsOnGroups()));
            dependsOnGroups.add(AcsCloudFoundryUtilities.CHECK_APP_HEALTH_TEST_GROUP);
            annotation.setDependsOnGroups(dependsOnGroups.toArray(new String[dependsOnGroups.size()]));

            Set<String> groups = new HashSet<>(Arrays.asList(annotation.getGroups()));
            groups.add(INTEGRATION_TEST_GROUP);
            annotation.setGroups(groups.toArray(new String[groups.size()]));
        }
    }

    @Override
    public void transform(final ITestAnnotation annotation, final Class testClass, final Constructor testConstructor,
            final Method testMethod) {

        transformHelper(annotation, testClass, testConstructor, testMethod);
    }

    @Override
    public void transform(final IConfigurationAnnotation annotation, final Class testClass,
            final Constructor testConstructor, final Method testMethod) {

        transformHelper(annotation, testClass, testConstructor, testMethod);
    }

    @Override
    public void transform(final IDataProviderAnnotation annotation, final Method method) {
        // Purposely empty since it's required by the IAnnotationTransformer2 interface but unused here
    }

    @Override
    public void transform(final IFactoryAnnotation annotation, final Method method) {
        // Purposely empty since it's required by the IAnnotationTransformer2 interface but unused here
    }
}
