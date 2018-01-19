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

package org.eclipse.keti.acs.encryption;

import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import com.google.common.primitives.Bytes;

public final class Encryptor {

    private static final String ALGO_WITH_PADDING = "AES/CBC/PKCS5PADDING";
    private static final String ALGO = "AES";
    private static final int IV_LENGTH_IN_BYTES = 16;
    private static final int KEY_LENGTH_IN_BYTES = 16;
    private static final String CHARSET_NAME = "UTF-8";

    private static final ThreadLocal<Cipher> CIPHER_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        try {
            return Cipher.getInstance(ALGO_WITH_PADDING);
        } catch (Exception e) {
            throw new CipherInitializationFailureException(
                    "Could not create instance of cipher with algorithm: " + ALGO_WITH_PADDING, e);
        }
    });

    private SecretKeySpec secretKeySpec;

    private static final class CipherInitializationFailureException extends RuntimeException {

        CipherInitializationFailureException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    // Modified version of Cassandra's CipherFactory#buildCipher
    // (https://github.com/apache/cassandra/blob/trunk/src/java/org/apache/cassandra/security/CipherFactory.java)
    private Cipher buildCipher(final byte[] iv, final int cipherMode) {
        try {
            Cipher cipher = CIPHER_THREAD_LOCAL.get();
            cipher.init(cipherMode, this.secretKeySpec, new IvParameterSpec(iv));
            return cipher;
        } catch (Exception e) {
            throw new CipherInitializationFailureException(
                    "Could not initialize instance of cipher with algorithm: " + ALGO_WITH_PADDING, e);
        }
    }

    public String encrypt(final String value) {
        try {
            byte[] ivBytes = new byte[IV_LENGTH_IN_BYTES];
            new SecureRandom().nextBytes(ivBytes);

            byte[] encrypted = this.buildCipher(ivBytes, Cipher.ENCRYPT_MODE).doFinal(value.getBytes(CHARSET_NAME));
            byte[] result = Bytes.concat(ivBytes, encrypted);

            return Base64.encodeBase64String(result);
        } catch (Exception e) {
            throw new EncryptionFailureException("Unable to encrypt", e);
        }
    }

    public String decrypt(final String encrypted) {
        try {
            byte[] encryptedBytes = Base64.decodeBase64(encrypted);
            byte[] ivBytes = Arrays.copyOfRange(encryptedBytes, 0, IV_LENGTH_IN_BYTES);
            byte[] encryptedSecretBytes = Arrays.copyOfRange(encryptedBytes, IV_LENGTH_IN_BYTES, encryptedBytes.length);

            byte[] original = this.buildCipher(ivBytes, Cipher.DECRYPT_MODE).doFinal(encryptedSecretBytes);

            return new String(original, CHARSET_NAME);
        } catch (Exception e) {
            throw new DecryptionFailureException("Unable to decrypt", e);
        }
    }

    public void setEncryptionKey(final String encryptionKey) {
        try {
            this.secretKeySpec = new SecretKeySpec(encryptionKey.getBytes(CHARSET_NAME), 0, KEY_LENGTH_IN_BYTES, ALGO);
        } catch (Exception e) {
            throw new SymmetricKeyValidationException("Encryption key must be string of length: " + KEY_LENGTH_IN_BYTES,
                    e);
        }
    }
}
