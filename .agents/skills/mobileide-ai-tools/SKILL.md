---
name: mobileide-ai-tools
description: MobileIDE AI 智能编程助手指南。用于修改 AI 聊天面板、API 提供商支持（如 DeepSeek、OpenAI、Gemini 等）、流式 API 调用以及会话保存。
---

# MobileIDE AI 智能编程助手

## 先读文件

- `app/src/main/java/com/scto/mobile/ide/ui/editor/aicoding/AICodingPanel.kt`：AI 聊天界面的 Compose UI 组件。
- `app/src/main/java/com/scto/mobile/ide/ui/editor/aicoding/AICodingViewModel.kt`：核心业务逻辑。包含 API 调用、模型拉取、会话管理等。
- `app/src/main/java/com/scto/mobile/ide/ui/editor/aicoding/AICodingState.kt`：AI 面板的显示与过渡状态。
- `app/src/main/java/com/scto/mobile/ide/ui/editor/aicoding/MarkdownRenderer.kt`：AI 回复的 Markdown 渲染。
- `app/src/main/java/com/scto/mobile/ide/ui/editor/aicoding/AICodingLocalizedText.kt`：AI 聊天助手的本地化文案辅助工具。

## 功能设计

- **API 连接**：基于 `OkHttpClient` 以流式请求（Streaming）与各大主流 AI 厂商（OpenAI, DeepSeek, Google Gemini, Anthropic, SiliconFlow 等）的兼容 API 进行交互。
- **配置持久化**：API Key、Base URL 及模型等配置保存在 `ai_coding_settings` SharedPreferences 中。
- **会话持久化**：历史聊天会话在私有文件系统下以 `ai_chat_sessions.json` 进行 JSON 读写保存。
- **本地化**：通过 `AICodingLocalizedText` 进行宿主语言的即时转换，以在未配置 Key 时展示正确的错误提示与预设助理消息。

## 修改流程

1. 确认更改属于 UI 层面（如 `AICodingPanel` 布局）还是网络/底层接口（如 `AICodingViewModel` 内的 HTTP 组装或 JSON 解析）。
2. 在 `AICodingViewModel` 中加入新的 API 提供商时，需在 `ApiProvider` 枚举中注册其名称、默认 Base URL 及默认模型。
3. 如果修改了流式返回的 JSON 结构，需重点测试不同厂商（特别是 DeepSeek 的 reasoning_content 思考过程或 Google API）的兼容性。

## 禁止事项

- 严禁在日志中输出用户输入的 API Key。
- 不要将庞大的 Markdown 解析放到 UI 主线程执行，避免打字机输出或流式回显时造成掉帧。

## 验证

- 运行 `./gradlew :app:compileArm64DebugKotlin --console=plain` 确保无编译错误。
- 本地启动 AI 面板测试，配置正确的 API 密钥，验证流式回显与思考过程是否正常展现。
