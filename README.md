# 随记

随记是一个面向 Android 的本地优先记账应用，界面采用 Xiaomi HyperOS 风格的 MIUIX Compose Multiplatform 组件。

## 当前功能

- 首页净资产与真实账户分组、账户详情和账户编辑
- 明细按日分组、账目编辑删除与周/月/年收支报表
- 手动录入金额、类型、账户、分类、时间、对象和备注
- 自然语言生成 AI 记账草稿，确认后才写入账本
- Room 本地数据库、默认账户与收支分类
- 手机底部导航与 600dp 以上可展开侧边导航
- MIUIX Backdrop 顶栏和底栏模糊
- 深浅色主题跟随系统

## 技术环境

- Android Gradle Plugin 9.3.1
- Kotlin 2.4.10
- Jetpack Compose Multiplatform 1.11.1
- Navigation 3 1.1.4
- Room 2.8.4
- MIUIX 0.9.3
- compileSdk / targetSdk 37
- minSdk 33（MIUIX Blur 0.9.3 的最低要求）

## 构建

使用 Android Studio 打开项目，等待 Gradle 同步完成后运行 `app`。命令行构建：

```powershell
.\gradlew.bat assembleDebug
```

Debug APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

## 数据保护边界

当前版本只使用本地 Room 数据库，不登录也不会上传账本。由于版本化备份与恢复功能尚未完成，应用明确关闭 Android 云端自动备份和设备迁移；卸载应用会删除本地账本。正式长期使用前应等待应用内备份恢复功能完成。

## 规则

界面与工程约束保存在 [AGENTS.md](AGENTS.md)，后续开发应持续遵守其中的 MIUIX、squircle、自适应布局、Card 分组和 AI 草稿确认规则。
