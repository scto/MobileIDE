---
name: mobileide-architecture-navigation
description: MobileIDE project architecture navigation. Used to understand module boundaries, startup entry points, main interface/editor data flow, or to determine whether new code should be placed in app, core, extension-languages, or plugins.
---

# MobileIDE Architecture Navigation

## 先读文件

- `settings.gradle.kts`：模块列表配置。
- `app/src/main/AndroidManifest.xml`：四大组件及权限声明。
- `app/src/main/java/com/scto/mobile/ide/App.kt`：Application 初始化入口类。
- `app/src/main/java/com/scto/mobile/ide/MainActivity.kt`：主页及核心编辑器工作区 Activity 容器。
- `app/src/main/java/com/scto/mobile/ide/MainScreen.kt`：主页 Tab 路由页面。

## 模块边界

- `app/`：宿主 APK 装载层。包含全局 UI 架构（侧边栏、状态过渡）、主要设置页面以及各子系统的直接协调。
- `core/*`：提供系统核心能力与基础结构。
  - `:core:main`：设置路由导航与共享 UI 状态。
  - `:core:terminal`, `:core:terminal-emulator`, `:core:terminal-view`：终端 PTY 仿真及 PRoot 容器支持。
  - `:core:extension`：本地 APK 插件动态装载。
  - `:core:lsp` 与 `:core:commands`：LSP 连接定义及宿主命令系统。
  - `:core:apk-builder`：APK 打包、打包模板替换、Manifest 补丁与签名管理。
  - `:core:common`：核心工具类和文件管理（FileManager）。
- `editor` 与 `editor-lsp`：Rosemoe 编辑器引擎及其 LSP 客户端实现。
- `language-treesitter`：语法高亮实现。
- `extension-languages`：内置语言服务扩展，定义 Java, Kotlin, Bash 等语言的连接。
- `plugins/*`：独立内置插件（如 `:plugins:java-lsp`, `:plugins:kotlin-lsp`）。

## 确定新代码的位置

- 多个模块或全局共享的非 UI 基础逻辑应放在 `core/common` 或对应 `core/*` 模块中。
- 主编辑器交互逻辑放在 `app` 模块的 `ui/editor` 目录下。
- 新增语言支持放在 `extension-languages` 下，或作为独立插件放在 `plugins/*` 中。

## 验证

- 在结构调整或改动后，执行主工程编译验证：
  ```powershell
  ./gradlew :app:compileArm64DebugKotlin --console=plain
  ```
