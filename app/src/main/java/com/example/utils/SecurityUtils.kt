package com.example.utils

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    // Static fallback salt/key derivation
    private const val DEFAULT_SALT = "SaltAndPepperForPinVault_123!"

    /**
     * Derives a 16-byte AES key from a PIN string and custom salt.
     */
    private fun deriveKey(pin: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest((pin + DEFAULT_SALT).toByteArray(Charsets.UTF_8))
        // Take first 16 bytes for AES-128
        val keyBytes = hash.copyOf(16)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts plain text using AES with the specified PIN as the key seed.
     */
    fun encrypt(plainText: String, pin: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val keySpec = deriveKey(pin)
            val cipher = Cipher.getInstance(ALGORITHM)
            // Use static IV derived from key for simple local determinism or custom 16 bytes
            val iv = ByteArray(16) { i -> (i * 7).toByte() }
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to Base64 on error to prevent database crashes
            Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    /**
     * Decrypts encrypted text using AES with the specified PIN as the key seed.
     */
    fun decrypt(encryptedText: String, pin: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val keySpec = deriveKey(pin)
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(16) { i -> (i * 7).toByte() }
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback decode on error
            try {
                String(Base64.decode(encryptedText, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (ex: Exception) {
                encryptedText
            }
        }
    }
}
