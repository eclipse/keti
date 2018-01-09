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

package com.ge.predix.acs.encryption;

import org.testng.Assert;
import org.testng.annotations.Test;

public class EncryptorTest {

    private static final String VALUE_TO_ENCRYPT = "testValue";

    @Test
    public void testEncryptCompleteFlow() {
        Encryptor encryption = new Encryptor();
        encryption.setEncryptionKey("FooBarFooBarFooB");
        Assert.assertNotEquals(encryption.encrypt(VALUE_TO_ENCRYPT), VALUE_TO_ENCRYPT);
        Assert.assertEquals(encryption.decrypt(encryption.encrypt(VALUE_TO_ENCRYPT)), VALUE_TO_ENCRYPT);
    }

    @Test(expectedExceptions = { SymmetricKeyValidationException.class })
    public void testCreateEncryptionWithTooShortOfAKey() {
        Encryptor encryption = new Encryptor();
        encryption.setEncryptionKey("Too_short");
    }

    @Test
    public void testCreateEncryptionWithTooLongOfAKey() {
        try {
            Encryptor encryption = new Encryptor();
            encryption.setEncryptionKey("Toooooooooo_loooooooooong");
        } catch (Throwable e) {
            Assert.fail();
        }
    }

}
