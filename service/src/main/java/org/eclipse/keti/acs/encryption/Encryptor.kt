/*******************************************************************************
 * Copyright 2017 General Electric Company
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

package org.eclipse.keti.acs.encryption

import com.google.common.primitives.Bytes
import org.apache.commons.codec.binary.Base64
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val ALGO_WITH_PADDING = "AES/CBC/PKCS5PADDING"
private const val ALGO = "AES"
private const val IV_LENGTH_IN_BYTES = 16
private const val KEY_LENGTH_IN_BYTES = 16
private const val CHARSET_NAME = "UTF-8"

private class CipherInitializationFailureException internal constructor(
    message: String,
    cause: Throwable
) : RuntimeException(message, cause)

private val CIPHER_THREAD_LOCAL = ThreadLocal.withInitial<Cipher> {
    try {
        return@withInitial Cipher.getInstance(ALGO_WITH_PADDING)
    } catch (e: Exception) {
        throw CipherInitializationFailureException(
            "Could not create instance of cipher with algorithm: $ALGO_WITH_PADDING", e
        )
    }
}

class Encryptor {

    private var secretKeySpec: SecretKeySpec? = null

    // Modified version of Cassandra's CipherFactory#buildCipher
    // (https://github.com/apache/cassandra/blob/trunk/src/java/org/apache/cassandra/security/CipherFactory.java)
    private fun buildCipher(
        iv: ByteArray,
        cipherMode: Int
    ): Cipher {
        try {
            val cipher = CIPHER_THREAD_LOCAL.get()
            cipher.init(cipherMode, this.secretKeySpec, IvParameterSpec(iv))
            return cipher
        } catch (e: Exception) {
            throw CipherInitializationFailureException(
                "Could not initialize instance of cipher with algorithm: $ALGO_WITH_PADDING", e
            )
        }

    }

    fun encrypt(value: String): String {
        try {
            val ivBytes = ByteArray(IV_LENGTH_IN_BYTES)
            SecureRandom().nextBytes(ivBytes)

            val encrypted =
                this.buildCipher(ivBytes, Cipher.ENCRYPT_MODE).doFinal(value.toByteArray(charset(CHARSET_NAME)))
            val result = Bytes.concat(ivBytes, encrypted)

            return Base64.encodeBase64String(result)
        } catch (e: Exception) {
            throw EncryptionFailureException("Unable to encrypt", e)
        }

    }

    fun decrypt(encrypted: String): String {
        try {
            val encryptedBytes = Base64.decodeBase64(encrypted)
            val ivBytes = Arrays.copyOfRange(encryptedBytes, 0, IV_LENGTH_IN_BYTES)
            val encryptedSecretBytes = Arrays.copyOfRange(encryptedBytes, IV_LENGTH_IN_BYTES, encryptedBytes.size)

            val original = this.buildCipher(ivBytes, Cipher.DECRYPT_MODE).doFinal(encryptedSecretBytes)

            return String(original, charset(CHARSET_NAME))
        } catch (e: Exception) {
            throw DecryptionFailureException("Unable to decrypt", e)
        }

    }

    fun setEncryptionKey(encryptionKey: String) {
        try {
            this.secretKeySpec =
                SecretKeySpec(encryptionKey.toByteArray(charset(CHARSET_NAME)), 0, KEY_LENGTH_IN_BYTES, ALGO)
        } catch (e: Exception) {
            throw SymmetricKeyValidationException(
                "Encryption key must be string of length: $KEY_LENGTH_IN_BYTES", e
            )
        }

    }
}
