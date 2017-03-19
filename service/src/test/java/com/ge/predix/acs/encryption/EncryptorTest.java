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
    public void testCreateEncryptionWithWrongKeySize() {
        Encryptor encryption = new Encryptor();
        encryption.setEncryptionKey("Key_With_Wrong_Size");
    }

}
