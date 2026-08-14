package com.vos.accounting.auto

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Test

/** 验证 OCR 下载文件使用的摘要计算。 */
class LocalOcrModelsTest {
    /** 固定输入应生成标准 SHA-256 小写摘要。 */
    @Test
    fun sha256HexReturnsExpectedDigest() {
        val input = ByteArrayInputStream("abc".toByteArray())

        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256Hex(input),
        )
    }
}
