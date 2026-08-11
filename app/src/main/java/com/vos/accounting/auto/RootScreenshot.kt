package com.vos.accounting.auto

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 只执行固定 su screencap 命令并返回一次性内存 Bitmap。 */
internal suspend fun takeRootScreenshotBitmap(): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val process = ProcessBuilder("su", "-c", "screencap -p")
            .redirectErrorStream(true)
            .start()
        val output = CompletableFuture.supplyAsync {
            process.inputStream.use { it.readNBytes(MAX_ROOT_SCREENSHOT_BYTES + 1) }
        }
        if (!process.waitFor(ROOT_SCREENSHOT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return@runCatching null
        }
        val bytes = output.get(1, TimeUnit.SECONDS)
        if (process.exitValue() != 0 || bytes.isEmpty() || bytes.size > MAX_ROOT_SCREENSHOT_BYTES) {
            return@runCatching null
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

/** 测试 Root 是否允许执行固定内存截图命令。 */
suspend fun testRootScreenshotAccess(): Boolean {
    val bitmap = takeRootScreenshotBitmap() ?: return false
    bitmap.recycle()
    return true
}

private const val MAX_ROOT_SCREENSHOT_BYTES = 24 * 1024 * 1024
private const val ROOT_SCREENSHOT_TIMEOUT_SECONDS = 3L
