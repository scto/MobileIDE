---
name: mobileide-extensions-system
description: MobileIDE 插件与扩展系统开发指南。用于新增/修改扩展 API、APK 插件装载、ExtensionContext 注入及扩展生命周期管理。
---

# MobileIDE 扩展系统

## 先读文件

- `core/extension/**`：核心扩展子系统，包含加载、定义和管理。
- `core/extension/src/main/java/com/rk/extension/api/ExtensionContext.kt`：向扩展暴露的上下文环境，用于获取宿主资源、运行域及 settings。
- `core/extension/src/main/java/com/rk/extension/loader/ExtensionLoader.kt`：使用 PathClassLoader 动态载入扩展 APK 并验证兼容性。
- `core/extension/src/main/java/com/rk/extension/manager/ExtensionManager.kt`：维护本地扩展列表及开关状态。
- `app/src/main/java/com/scto/mobile/ide/MainActivity.kt`：在宿主启动时初始化 ExtensionManager，并演示配置测试扩展。

## 项目事实

- **技术原理**：宿主基于 `PathClassLoader` 动态装载未安装的 APK 插件。
- **扩展清单 (manifest.json)**：每个扩展的包目录中需包含 `manifest.json` 描述文件，包含 `id`, `name`, `mainClass`, `version`, `minAppVersion` 等属性。
- **扩展入口**：主入口类必须实现 `ExtensionAPI` 接口，并且必须有一个接收 `ExtensionContext` 的构造函数。
- **生命周期**：扩展支持 `onInstalled()` 与 `onExtensionLoaded()` 生命周期方法。
- **状态存储**：每个扩展拥有独立的 `SharedPrefExtensionSettings`。
- **测试环境**：`MainActivity` 中会为调试目的在本地目录下自动释放测试扩展以验证系统装载是否正常。

## 修改流程

1. 确认属于扩展宿主接口设计（`ExtensionAPI` / `ExtensionContext`）还是生命周期加载管理（`ExtensionLoader`）。
2. 在 `ExtensionContext` 中新增方法时，确保遵循向前兼容约定，不要破坏已有外部插件的加载。
3. 如果修改了 Manifest 定义，需更新 `ExtensionManifest.kt` 类。
4. 运行编译确认核心模块没有编译失败。

## 禁止事项

- 严禁在主线程同步加载或初始化扩展，所有装载过程必须跑在 background 协程（如 `Dispatchers.IO`）中。
- 绝不随意更改核心扩展的包名（`com.rk.extension`），否则会破坏对已有已编译扩展的引用兼容。

## 验证

- 修改扩展系统逻辑后，通过以下命令运行编译：
  ```powershell
  ./gradlew :core:extension:compileDebugKotlin --console=plain
  ./gradlew :app:compileArm64DebugKotlin --console=plain
  ```
