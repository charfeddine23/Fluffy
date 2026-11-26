package com.charfeddine.fluffy.scanner.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Local AES-256-GCM encryption & SHA-256 PIN vault helper powered by AndroidKeyStore.
 * Used for encrypting sensitive local scanned images/pages and saved E-Signatures completely offline.
 */
object CryptoVaultHelper {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "fluffy_scanner_aes256_master"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_SIZE = 12
    private const val PREFS_NAME = "fluffy_scanner_vault_prefs"
    private const val KEY_PIN_HASH = "scanner_pin_hash"

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encryptBytes(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain)
        return ByteArray(IV_SIZE + cipherText.size).apply {
            System.arraycopy(iv, 0, this, 0, IV_SIZE)
            System.arraycopy(cipherText, 0, this, IV_SIZE, cipherText.size)
        }
    }

    fun decryptBytes(payload: ByteArray): ByteArray {
        if (payload.size <= IV_SIZE) return ByteArray(0)
        val iv = payload.copyOfRange(0, IV_SIZE)
        val cipherText = payload.copyOfRange(IV_SIZE, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherText)
    }

    fun writeEncryptedFile(destFile: File, plainBytes: ByteArray) {
        destFile.parentFile?.mkdirs()
        destFile.writeBytes(encryptBytes(plainBytes))
    }

    fun readEncryptedFile(srcFile: File): ByteArray? {
        if (!srcFile.exists()) return null
        return runCatching { decryptBytes(srcFile.readBytes()) }.getOrNull()
    }

    fun hasPinConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()
    }

    fun savePin(context: Context, pin: String) {
        val clean = pin.trim()
        if (clean.length < 4) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PIN_HASH, sha256(clean)).commit()
    }

    fun verifyPin(context: Context, candidate: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return stored == sha256(candidate.trim())
    }

    private fun sha256(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest("fluffy_scanner_pin::$raw".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
