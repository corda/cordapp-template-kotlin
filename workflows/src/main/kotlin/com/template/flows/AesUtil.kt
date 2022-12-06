package com.template.flows

import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES encryption/description util. This is only for testing purposes as it uses fixed values for key "salt" and
 * "Initialisation vector".
 * Reference: https://www.baeldung.com/java-aes-encryption-decryption
 */
class AesUtil private constructor() {
    companion object {
        private const val KEY_SIZE = 256
        private const val KEY_ALG = "AES"
        private const val CIPHER_ALG = "AES/CBC/PKCS5Padding"

        private fun generateKey(): SecretKey {
            val keyGenerator = KeyGenerator.getInstance(KEY_ALG)
            keyGenerator.init(KEY_SIZE)
            return keyGenerator.generateKey()
        }

        private fun generateKey(password: String): SecretKey {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(
                password.toCharArray(),
                "salt".toByteArray(),
                65536,
                256
            )
            return SecretKeySpec(factory.generateSecret(spec).encoded, KEY_ALG)
        }

        fun encrypt(data: String, password: String): String {
            val cipher = Cipher.getInstance(CIPHER_ALG)
            val key = generateKey(password)
            cipher.init(Cipher.ENCRYPT_MODE, key, generateIv())

            val cipheredData = cipher.doFinal(data.toByteArray())
            return Base64.getEncoder().encodeToString(cipheredData)
        }

        fun decrypt(encryptedData: String, password: String): String {
            val cipher = Cipher.getInstance(CIPHER_ALG)
            val key = generateKey(password)
            cipher.init(Cipher.DECRYPT_MODE, key, generateIv())

            val decipheredData = cipher.doFinal(Base64.getDecoder().decode(encryptedData))
            return String(decipheredData)
        }

        private fun generateIv(): IvParameterSpec {
            val iv = ByteArray(16) { 1 }
            // SecureRandom().nextBytes(iv)
            return IvParameterSpec(iv)
        }

        fun genMD5(input: String): String {
            val md5 = MessageDigest.getInstance("MD5")
            return BigInteger(1, md5.digest(input.toByteArray())).toString(16)
        }
    }
}
