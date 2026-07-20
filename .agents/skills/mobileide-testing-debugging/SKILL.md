---
name: mobileide-testing-debugging
description: MobileIDE 测试与排障指南。用于选择单元测试/仪器测试命令、定位失败原因、验证 APK 构建、编辑器及 Terminal 等核心模块改动。
---

# MobileIDE 测试与排障

## 先读文件

- 目标模块的 `build.gradle` 或 `build.gradle.kts`：确认测试配置和依赖。
- 目标模块的 `src/test/**` 与 `src/androidTest/**`：参考已有测试类来编写测试用例。
- `gradle/libs.versions.toml`：集中的依赖管理。

## 常用命令

```powershell
# 运行 apk-builder 单元测试
./gradlew :core:apk-builder:testDebugUnitTest --console=plain

# 运行 terminal-emulator 单元测试
./gradlew :core:terminal-emulator:test --console=plain

# 运行 editor 单元测试
./gradlew :editor:testDebugUnitTest --console=plain

# 运行 language-treesitter 单元测试
./gradlew :language-treesitter:testDebugUnitTest --console=plain
```

## 测试覆盖重点

- **APK 构建功能**：在 `:core:apk-builder` 中对 `ApkSigner`, `ManifestPatcher` 以及 `DebugKeyStore` 进行签名与打包模板替换逻辑测试。
- **终端仿真（Terminal）**：在 `:core:terminal-emulator` 中对终端输入输出、光标操作、控制序列等进行基于 JUnit 的常规测试。
- **编辑器核心 (editor)**：对文本 IO、缓存、撤销重做、高亮等底层文本库机制（如 `io.github.rosemoe.sora`）进行充分测试。
- **语法高亮 (language-treesitter)**：对语法树节点解析及相关逻辑进行测试。

## 高风险误区

- 严禁在单元测试中读取宿主的真实私有目录或外部网络。
- 测试过程中应当 mock 复杂的外部环境（如 Android Context），避免对真实系统环境产生依赖。

## 验证

- 代码变更后优先编译目标模块，然后执行对应的单元测试。
- 针对修改涉及的端到端页面，请启动 App 并人工交互验证以确保无 Regression。
