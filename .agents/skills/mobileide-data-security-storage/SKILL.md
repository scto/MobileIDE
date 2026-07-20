---
name: mobileide-data-security-storage
description: MobileIDE 数据层、安全、权限、路径、敏感信息和文件分享指南。用于修改 Preferences DataStore、SharedPreferences、文件沙盒路径、FileProvider 文件分享、及 Android 权限。
---

# MobileIDE 数据、安全与存储

## 先读文件

- `app/src/main/java/com/scto/mobile/ide/utils/ThemeDataStore.kt`：主题和颜色配置的数据存储（DataStore）。
- `app/src/main/java/com/scto/mobile/ide/utils/AppLanguageManager.kt`：语言配置存储（SharedPreferences）。
- `app/src/main/java/com/scto/mobile/ide/files/sandboxHomeDir.kt`：定义应用沙盒主路径（`files/sandbox`）。
- `app/src/main/java/com/scto/mobile/ide/App.kt`：包含临时文件夹获取逻辑 `getTempDir()`（`files/parent/tmp`）。
- `app/src/main/AndroidManifest.xml`：注册权限、服务及 FileProvider。
- `app/src/main/res/xml/file_paths.xml`：FileProvider 暴露的文件路径配置。

## 数据层事实

- **配置管理**：主要采用 Jetpack Preferences DataStore 存储（如 `mobileide_theme_settings`）和标准 Android SharedPreferences 存储（如 `MobileIDE_Settings`）。
- **无 Room 数据库**：目前本仓无 Room 数据库，所有项目文件索引和编辑器状态大多存储在内存或直接在磁盘上管理，配置存储通过 DataStore / SharedPreferences 实现。
- **文件沙盒**：应用拥有私有沙盒环境 `sandboxHomeDir(context)`，包含 Alpine 等 PRoot 环境文件均在应用专有目录下运行。

## 安全与路径事实

- 项目和系统临时路径应分别使用 `sandboxHomeDir` 与 `App.getTempDir()` 获取。
- 外部文件分享统一使用 Android `FileProvider` 进行，授权以 `${applicationId}.fileprovider` 进行 `content://` 资源映射，禁止直接暴露 `file://` URI。
- Android Manifest 允许明文流量（`usesCleartextTraffic="true"`），修改安全网络策略需确认对远程 LSP 及本地调试接口的影响。

## Android 权限

- 包含：网络访问、POST_NOTIFICATIONS、MANAGE_EXTERNAL_STORAGE（管理外部存储）、FOREGROUND_SERVICE、安装/删除包权限等。
- 权限说明与用户可见文案需走本地化多语言资源（`strings.xml`）。

## 高风险误区

- 严禁拼接或信任外部传入的越界路径，防范路径截断与目录穿越漏洞。
- 严禁在普通日志中打印敏感信息、API key 等。
- 绝不直接暴露 `file://` 给外部 Activity；请转为 `content://` URI 进行共享。

## 验证

- 对存储或权限改动后，运行 `./gradlew :app:compileArm64DebugKotlin --console=plain` 验证编译。
- 人工核对 Manifest 与 `file_paths.xml` 确保路径映射正确，未破坏外部共享接口。
