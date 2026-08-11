package com.vos.accounting.auto

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.equationl.ncnnandroidppocr.OCR
import com.equationl.ncnnandroidppocr.bean.Device
import com.equationl.ncnnandroidppocr.bean.DrawModel
import com.equationl.ncnnandroidppocr.bean.ImageSize
import com.equationl.ncnnandroidppocr.bean.ModelType
import com.vos.accounting.model.PaymentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/** 串行运行内存截图 OCR，并按支付平台执行十秒冷却。 */
class LocalAutoBookkeepingOcr(context: Context) {
    private val assets = context.applicationContext.assets
    private val lastRunAt = mutableMapOf<PaymentProvider, Long>()
    private val executor = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(false)
    private var ocr: OCR? = null

    /** 在五秒超时内返回有序 OCR 文本行，失败或冷却中返回空值。 */
    suspend fun recognize(provider: PaymentProvider, bitmap: Bitmap): List<String>? {
        val now = SystemClock.elapsedRealtime()
        val coolingDown = synchronized(lastRunAt) {
            val previous = lastRunAt[provider]
            if (previous != null && now - previous < OCR_COOLDOWN_MILLIS) {
                true
            } else {
                lastRunAt[provider] = now
                false
            }
        }
        if (coolingDown) return null
        if (!running.compareAndSet(false, true)) return null
        val workerBitmap = runCatching {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }.getOrNull() ?: run {
            running.set(false)
            return null
        }
        return withContext(Dispatchers.IO) {
            val future = runCatching {
                executor.submit<List<String>?> {
                    try {
                        runCatching {
                            val engine = ocr ?: OCR().let { created ->
                                if (created.initModelFromAssert(assets, ModelType.Mobile, ImageSize.Size720, Device.CPU)) {
                                    created.also { ocr = it }
                                } else {
                                    created.release()
                                    null
                                }
                            } ?: error("OCR 模型初始化失败")
                            engine.detectBitmap(workerBitmap, DrawModel.None)
                                ?.textLines
                                ?.map { line -> line.text.trim() }
                                ?.filter(String::isNotEmpty)
                                ?.takeIf(List<String>::isNotEmpty)
                        }.getOrNull()
                    } finally {
                        workerBitmap.recycle()
                        running.set(false)
                    }
                }
            }.getOrElse {
                workerBitmap.recycle()
                running.set(false)
                return@withContext null
            }
            try {
                future.get(OCR_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
                null
            } catch (_: Exception) {
                null
            }
        }
    }

    /** 在当前识别任务结束后串行释放模型与执行器。 */
    fun close() {
        runCatching {
            executor.execute {
                ocr?.release()
                ocr = null
            }
        }
        executor.shutdown()
    }

    private companion object {
        const val OCR_COOLDOWN_MILLIS = 10_000L
        const val OCR_TIMEOUT_MILLIS = 5_000L
    }
}
