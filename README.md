# 不用记帐

不用记帐是一款面向 Android 的本地优先个人记账应用。数据以设备内的 Room 数据库为准，界面使用 MIUIX Compose Multiplatform 实现 Xiaomi HyperOS 风格，并针对手机与宽屏设备提供自适应布局。

## 当前功能

- 首页展示净资产、总资产、总负债，以及按类型分组的真实账户余额
- 支持多账本、账本切换、自定义封面、本位币和账户适用账本
- 支持账户、账户类型、分类与币种的新增、编辑、停用和删除约束
- 手动新增及编辑收支，金额使用最小货币单位 `Long` 保存
- 明细按设备时区的自然日分组，可编辑或二次确认后删除
- 周报、月报、年报，以及周期趋势、近六期趋势和分类构成
- 预置及自定义币种，支持手动汇率和每日缓存的公开自动汇率
- 密码加密备份与覆盖恢复，包含账本、账户、账目、币种、分类、设置及自定义媒体
- 深色、浅色和跟随系统外观，可选动态系统配色、预测性返回与收支金额颜色
- 手机端水平 Pager 与底部导航，`600.dp` 以上使用宽屏侧边导航
- MIUIX Backdrop 顶栏和底栏模糊；支持设备上可用的小米超级岛通知

## AI 记账

AI 记账支持微信、支付宝和云闪付的简体中文支付结果页。采集结果会先进入待确认列表，用户确认或编辑后才通过统一保存流程写入正式账目，不会由采集链路直接入账。

可按需组合以下来源：

- 通知读取与无障碍页面文本
- 声明式规则包，可查看内置规则并导入、启停或删除用户规则
- 本地 PP-OCRv5 截图识别；首次开启会下载约 11 MB 模型
- Root 内存截图兜底
- LSPosed Hook 采集微信和支付宝账单详情
- 用户自行配置的 OpenAI 兼容私有 AI 服务，可独立决定是否允许上传裁剪压缩后的截图

部分模式需要在系统设置、Root 管理器或 LSPosed 管理器中单独授权。侧载应用启用无障碍时，Android 可能要求先在应用信息页允许“受限制的设置”。

## 数据与隐私

- 账本默认离线可用，无登录或应用自建云同步
- Android 云端自动备份和设备迁移已关闭；卸载应用会删除应用内数据库和私有媒体
- 可在“设置 → 数据备份与恢复”导出密码加密的 `.bak` 文件；恢复会预览摘要并覆盖当前全部数据
- OCR 截图、Bitmap、PNG 字节及识别原文只在内存中处理，不写入文件、日志、数据库或备份
- 联网仅用于用户启用的功能：下载 OCR 模型、刷新公开汇率，以及访问用户配置的私有 AI 服务
- 私有 AI 的视觉识别默认关闭；开启前应用会提示截图可能包含的敏感信息

请妥善保存备份文件和密码。忘记密码时无法解密备份，覆盖恢复也不可撤销。

## 运行要求

- Android 13（API 33）及以上
- 当前 APK 仅包含 `arm64-v8a`
- 应用界面当前固定为竖屏
- 本地 OCR、Root 截图、LSPosed Hook 和小米超级岛均为可选能力，不影响基础手动记账

## 构建与验证

使用 Android Studio 打开项目并等待 Gradle 同步，选择 `app` 配置运行。命令行需要 JDK 17：

```powershell
.\gradlew.bat assembleDebug
```

Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

常用验证命令：

```powershell
.\gradlew.bat lintDebug testDebugUnitTest assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

CI 会执行 Lint、JVM 单元测试、Debug APK 构建和 Release Bundle 构建。Room 迁移及数据库集成测试需要已连接的 Android 设备或模拟器。

## 发布构建

版本号可在构建时覆盖：

```powershell
.\gradlew.bat bundleRelease -PversionCode=2 -PversionName=0.2.0
```

正式签名从仓库根目录的 `keystore.properties` 读取：

```properties
storeFile=path/to/release.keystore
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

未提供签名配置时，Release 构建会回退到 Debug 签名，仅用于本地复现，不应作为正式发布包。

## 技术栈

- Android Gradle Plugin 9.3.1、Kotlin 2.4.10、JDK 17
- Jetpack Compose Multiplatform 1.11.1
- Navigation 3 1.1.4
- Room 2.8.4
- MIUIX Compose Multiplatform 0.9.3
- RE2/J、PaddleOCR ncnn、HyperNotification Focus API 与 libxposed API
- `compileSdk` / `targetSdk` 37，`minSdk` 33

第三方组件和 OCR 模型来源见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。界面与工程约束见 [AGENTS.md](AGENTS.md)。
