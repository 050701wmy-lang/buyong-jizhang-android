package com.vos.accounting.auto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.vos.accounting.data.AccountingRepository
import com.vos.accounting.data.AutoAiCredentialEntity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val AI_KEY_ALIAS = "auto_bookkeeping_ai_api_key"
private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"

/** 使用 Android Keystore 加密和读取私有 AI API Key。 */
class AutoAiCredentialStore(private val repository: AccountingRepository) {
    /** 加密保存 API Key，空值表示删除。 */
    suspend fun save(apiKey: String) {
        if (apiKey.isBlank()) {
            repository.deleteAutoAiCredential()
            return
        }
        val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val encrypted = cipher.doFinal(apiKey.trim().toByteArray(Charsets.UTF_8))
        repository.upsertAutoAiCredential(
            AutoAiCredentialEntity(
                cipherText = encrypted,
                initializationVector = cipher.iv,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    /** 解密 API Key，凭据损坏或密钥失效时返回空值。 */
    suspend fun read(): String? {
        val stored = repository.findAutoAiCredential() ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, stored.initializationVector))
            }
            cipher.doFinal(stored.cipherText).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    /** 返回已存在密钥或创建仅允许 AES-GCM 加解密的新密钥。 */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(AI_KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                AI_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }
}
