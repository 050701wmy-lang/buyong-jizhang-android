package com.vos.accounting.auto

import android.content.Context
import com.equationl.ncnnandroidppocr.OCR
import com.equationl.ncnnandroidppocr.bean.Device
import com.equationl.ncnnandroidppocr.bean.ImageSize
import java.io.File
import java.io.InputStream
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 管理 PP-OCRv5 模型的按需下载、校验与本地初始化。 */
class LocalOcrModels(context: Context) {
    private val modelDirectory = File(context.applicationContext.filesDir, MODEL_DIRECTORY)
    private val readyFile = File(modelDirectory, READY_FILE_NAME)

    /** 判断当前版本的全部模型是否已经完整安装。 */
    fun isInstalled(): Boolean =
        readyFile.takeIf(File::isFile)?.readText() == MODEL_VERSION &&
            MODEL_FILES.all { model -> File(modelDirectory, model.name).length() == model.size }

    /** 在 IO 线程下载并校验缺失模型，全部成功后再标记当前版本可用。 */
    suspend fun install() = withContext(Dispatchers.IO) {
        check(modelDirectory.mkdirs() || modelDirectory.isDirectory) { "无法创建 OCR 模型目录" }
        readyFile.delete()
        MODEL_FILES.forEach(::installModel)
        readyFile.writeText(MODEL_VERSION)
    }

    /** 使用已经完整安装的文件初始化 OCR 引擎。 */
    fun initialize(ocr: OCR): Boolean {
        if (!isInstalled()) return false
        return ocr.initModel(
            File(modelDirectory, DET_PARAM_FILE).absolutePath,
            File(modelDirectory, DET_MODEL_FILE).absolutePath,
            File(modelDirectory, REC_PARAM_FILE).absolutePath,
            File(modelDirectory, REC_MODEL_FILE).absolutePath,
            ImageSize.Size720,
            Device.CPU,
        )
    }

    /** 复用校验通过的文件，否则下载到临时文件并原子替换。 */
    private fun installModel(model: OcrModelFile) {
        val target = File(modelDirectory, model.name)
        if (target.length() == model.size && target.inputStream().use(::sha256Hex) == model.sha256) return
        val pending = File(modelDirectory, "${model.name}.download")
        pending.delete()
        download(model, pending)
        check(pending.length() == model.size) { "OCR 模型大小校验失败" }
        check(pending.inputStream().use(::sha256Hex) == model.sha256) { "OCR 模型摘要校验失败" }
        check(!target.exists() || target.delete()) { "无法替换旧 OCR 模型" }
        check(pending.renameTo(target)) { "无法安装 OCR 模型" }
    }

    /** 从固定上游提交通过 HTTPS 下载一个模型文件。 */
    private fun download(model: OcrModelFile, target: File) {
        val connection = URL("$MODEL_BASE_URL/${model.name}").openConnection() as HttpsURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = READ_TIMEOUT_MILLIS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Accounting/$MODEL_VERSION")
        try {
            check(connection.responseCode in 200..299) { "OCR 模型下载失败" }
            val responseSize = connection.contentLengthLong
            check(responseSize < 0 || responseSize == model.size) { "OCR 模型响应大小异常" }
            connection.inputStream.use { input ->
                target.outputStream().buffered().use(input::copyTo)
            }
        } finally {
            connection.disconnect()
        }
    }

    /** 描述一个固定版本模型文件的名称、大小与可信摘要。 */
    private data class OcrModelFile(
        val name: String,
        val size: Long,
        val sha256: String,
    )

    private companion object {
        const val MODEL_VERSION = "pp_ocrv5_mobile_671ac4a"
        const val MODEL_DIRECTORY = "ocr_models/pp_ocrv5_mobile"
        const val READY_FILE_NAME = "ready"
        const val DET_MODEL_FILE = "PP_OCRv5_mobile_det.ncnn.bin"
        const val DET_PARAM_FILE = "PP_OCRv5_mobile_det.ncnn.param"
        const val REC_MODEL_FILE = "PP_OCRv5_mobile_rec.ncnn.bin"
        const val REC_PARAM_FILE = "PP_OCRv5_mobile_rec.ncnn.param"
        const val MODEL_BASE_URL =
            "https://raw.githubusercontent.com/nihui/ncnn-android-ppocrv5/" +
                "671ac4a72299a86ddee160131ba88fed748df425/app/src/main/assets"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 60_000
        val MODEL_FILES = listOf(
            OcrModelFile(
                DET_MODEL_FILE,
                2_357_216L,
                "857a96bc963725105b78a178dfcc3c0c3db1a7b9eef32244367b2cb105ccf60b",
            ),
            OcrModelFile(
                DET_PARAM_FILE,
                24_821L,
                "358f459680ae0e7a73e477469e529ce116f68c629019ec7a0b6457d2d9117934",
            ),
            OcrModelFile(
                REC_MODEL_FILE,
                8_242_276L,
                "49d9907a55ba20fa6637f9f788f66ab00793bc8ce57a733a09dbe86b9a2e3db0",
            ),
            OcrModelFile(
                REC_PARAM_FILE,
                20_031L,
                "f52a6586ac3338d8c350db0c9f3c55ff2ecd3763327f9bc7f0efc091f8a63e74",
            ),
        )
    }
}

/** 读取输入流并返回小写十六进制 SHA-256 摘要。 */
internal fun sha256Hex(input: InputStream): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        digest.update(buffer, 0, count)
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}
