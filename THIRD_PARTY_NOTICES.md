# 第三方依赖与模型来源

## AI 无感记账新增项

- RE2/J 1.8：`com.google.re2j:re2j`，Go License。来源：<https://github.com/google/re2j>。
- Xposed API 82：仅 `compileOnly`，不随 APK 打包。来源：<https://github.com/rovo89/XposedBridge>，Apache License 2.0。
- ncnn：BSD 3-Clause。来源：<https://github.com/Tencent/ncnn>。
- PaddleOCR / PP-OCRv5 Mobile 模型：Apache License 2.0。来源：<https://github.com/PaddlePaddle/PaddleOCR>。
- PP-OCRv5 ncnn 转换模型：按需下载自固定提交 <https://github.com/nihui/ncnn-android-ppocrv5/tree/671ac4a72299a86ddee160131ba88fed748df425>。
- `ncnnandroidppocr` v1.3.0：来源：<https://github.com/equationl/paddleocr4android>。该包装仓库当前未声明顶层许可证，记为 `NOASSERTION`；本项目仅按个人侧载范围使用，公开或商业分发前必须替换为许可证明确的构建或取得授权。
- AutoAccounting：仅用于行为与测试思路调研，未复制其 GPL-3.0 源码。来源：<https://github.com/AutoAccountingOrg/AutoAccounting>。

OCR 截图、Bitmap、PNG 字节与识别原文仅在内存中处理，不写入日志、数据库或备份。
