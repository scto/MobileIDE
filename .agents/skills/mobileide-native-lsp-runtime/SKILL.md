---
name: mobileide-native-lsp-runtime
description: MobileIDE PRoot 运行环境、LSP 语言服务、Tree-sitter 语法高亮与 Terminal 排障指南。用于处理 PRoot 容器、LSP 服务器连接、Tree-sitter 高亮以及 Terminal 会话。
---

# MobileIDE Native / LSP / Runtime

## 先读文件

- `editor-lsp/**`：LSP 客户端底层实现，处理 JSON-RPC 交互。
- `core/lsp/**`：语言服务注册、连接与管理（`LspServer.kt`, `LspRegistry.kt`）。
- `app/src/main/java/com/scto/mobile/ide/lsp/ProotStreamConnectionProvider.kt`：桥接 LSP 与 PRoot 容器的流连接提供器。
- `app/src/main/java/com/scto/mobile/ide/ui/terminal/DistroManager.kt`：PRoot 运行环境（如 Alpine/Ubuntu 沙盒）及终端会话的核心管理者。
- `language-treesitter/**`：Tree-sitter 的 Android/Rosemoe 适配及语法树解析。
- `extension-languages/**`：内置 LSP 扩展加载器（Java, Kotlin, C++, Bash 等）。

## 项目事实

- **PRoot 编译与执行**：构建任务（如 gradle 编译）与部分语言服务通常运行在 PRoot 容器环境中。使用 `DistroManager.buildProotCommand` 包装命令并使用 `DistroManager.getProotEnv` 传递环境变量（如 `PROOT_LOADER`、`PROOT_TMP_DIR`）。
- **LSP 连接模式**：对运行于沙盒中的语言服务，客户端通过 `ProotStreamConnectionProvider` 与之进行 IO 流通信。
- **语法高亮**：IDE 在编辑器中集成了 Tree-sitter 进行高亮解析（如 `TsLanguage`），若失败则使用正则高亮作为 fallback。
- **LSP 安装与更新**：通过设置页面（`LspSettingsScreen`）进行 LSP 服务器的管理与更新，通过 `initCommand` 唤起 Terminal 执行自动安装脚本。

## 修改流程

1. 确认更改属于 LSP 协议交互（`editor-lsp`）、宿主连接管理（`core:lsp` / `ProotStreamConnectionProvider`）、运行容器（`DistroManager`）还是具体语言插件。
2. 尽量复用已有的 LSP 结构和 Provider，不要重新实现平行的连接器。
3. 修改沙盒配置时需评估 `DistroManager` 对 Alpine / Ubuntu 环境参数的兼容性影响。
4. 运行编译及测试，检验 LSP 连接是否稳健。

## 禁止事项

- 不要硬编码容器内的临时目录，请优先使用 `PROOT_TMP_DIR`。
- 不要直接在 Android 宿主执行未被 PRoot 包裹的 Linux 二进制文件。

## 验证

- 修改 LSP 或沙盒环境后，通过 `./gradlew :app:compileArm64DebugKotlin --console=plain` 检验编译。
- 启动终端或 LSP 安装脚本，确保 `initCommand` 执行无异常。
