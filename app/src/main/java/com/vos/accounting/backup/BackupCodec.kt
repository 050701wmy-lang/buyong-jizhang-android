package com.vos.accounting.backup

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** 表示备份文件读取或校验失败。 */
class BackupException(message: String) : IllegalArgumentException(message)

/** 解包后的备份数据与媒体字节。 */
data class ZipContent(
    val data: BackupData,
    val media: Map<String, ByteArray>,
)

/**
 * 负责备份文件的加密与压缩编解码。
 */
@OptIn(ExperimentalSerializationApi::class)
object BackupCodec {
    private val MAGIC = "SJZBK001".toByteArray(Charsets.US_ASCII)
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val JSON_ENTRY = "backup.json"
    private const val MEDIA_DIR = "media/"
    private const val MAX_PAYLOAD_BYTES = 64L * 1024 * 1024

    /** 备份包内媒体字节总量上限。 */
    const val MAX_TOTAL_MEDIA_BYTES = 64L * 1024 * 1024

    private val json = Json {
        namingStrategy = JsonNamingStrategy.SnakeCase
        ignoreUnknownKeys = true
    }

    /** 使用密码派生密钥加密压缩包。 */
    fun encrypt(payload: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(payload)
        return ByteArrayOutputStream().use { out ->
            out.write(MAGIC)
            out.write(salt)
            out.write(iv)
            out.write(
                ByteBuffer.allocate(Long.SIZE_BYTES)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putLong(ciphertext.size.toLong())
                    .array(),
            )
            out.write(ciphertext)
            out.toByteArray()
        }
    }

    /** 使用密码派生密钥解密并校验备份文件。 */
    fun decrypt(blob: ByteArray, password: String): ByteArray {
        val input = ByteArrayInputStream(blob)
        val magic = ByteArray(MAGIC.size)
        if (input.read(magic) != magic.size || !magic.contentEquals(MAGIC)) {
            throw BackupException("不是有效的备份文件")
        }
        val salt = input.readNBytes(SALT_LENGTH)
        val iv = input.readNBytes(IV_LENGTH)
        val payloadLength = ByteBuffer.wrap(input.readNBytes(Long.SIZE_BYTES))
            .order(ByteOrder.LITTLE_ENDIAN)
            .long
        if (payloadLength < 0 || payloadLength > MAX_PAYLOAD_BYTES) {
            throw BackupException("备份文件大小异常")
        }
        val ciphertext = input.readNBytes(payloadLength.toInt())
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return try {
            cipher.doFinal(ciphertext)
        } catch (error: Exception) {
            throw BackupException("密码错误或备份文件已损坏")
        }
    }

    /** 把数据与媒体打包为压缩包。 */
    fun buildZip(data: BackupData, media: Map<String, ByteArray>): ByteArray {
        val jsonBytes = json.encodeToString(BackupData.serializer(), data).toByteArray(Charsets.UTF_8)
        return ByteArrayOutputStream().use { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry(JSON_ENTRY))
                zip.write(jsonBytes)
                zip.closeEntry()
                media.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(MEDIA_DIR + name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            out.toByteArray()
        }
    }

    /** 解包并校验压缩包内容。 */
    fun unzip(payload: ByteArray): ZipContent {
        var jsonBytes: ByteArray? = null
        val media = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(payload)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == JSON_ENTRY -> jsonBytes = zip.readBytes()
                    entry.name.startsWith(MEDIA_DIR) -> media[entry.name.removePrefix(MEDIA_DIR)] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val jsonText = jsonBytes?.toString(Charsets.UTF_8)
            ?: throw BackupException("备份内容缺失")
        val data = json.decodeFromString(BackupData.serializer(), jsonText)
        if (data.formatVersion != 1) throw BackupException("不支持的备份版本")
        return ZipContent(data, media)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

}
