package com.phazei.dynamicgptchat.data.datastore
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager() {

    companion object {
        private const val KEY_ALIAS = "my_key_alias"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val BUFFER_SIZE = 4096
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore

    init {
        keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
            load(null)
        }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }

    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encrypt(bytes: ByteArray, outputStream: OutputStream) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, getSecretKey())
        }
        val iv = cipher.iv
        outputStream.write(iv)
        CipherOutputStream(outputStream, cipher).use { cipherOut ->
            cipherOut.write(bytes)
        }
    }

    fun decrypt(inputStream: InputStream): ByteArray {
        val iv = ByteArray(12) // 12 bytes is the default IV size for GCM
        inputStream.read(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        }
        return CipherInputStream(inputStream, cipher).use { cipherIn ->
            cipherIn.readBytes()
        }
    }
}
