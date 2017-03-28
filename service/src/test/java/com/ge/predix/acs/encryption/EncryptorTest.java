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
