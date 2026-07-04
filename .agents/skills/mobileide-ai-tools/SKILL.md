---
name: mobileide-ai-tools
description: MobileIDE RikkaHub/AI integration development guide. Used for modifying the embedded RikkaHub entry point, sidebar chat container, model/channel boundaries, API key security boundaries, embedded compilation, or AI integration with the editor host.
---

# MobileIDE RikkaHub / AI Integration

## Read First

- `settings.gradle.kts`: `external/rikkahub` included build and `rikkahub-embedded` dependency substitution.
- `app/build.gradle.kts`: Dependency of the main APK on `me.rerere.rikkahub:rikkahub-embedded`.
- `app/src/main/java/com/scto/mobile/ide/ui/compose/components/DrawerContent.kt`.
- `app/src/main/java/com/scto/mobile/ide/settings/SettingsActivity.kt`.
- `external/rikkahub/embedded/**`.
- `external/rikkahub/app/src/main/java/me/rerere/rikkahub/RikkaHubEmbeddedChatPane.kt`: Contains `RikkaHubEmbeddedChatPane` and `RikkaHubEmbeddedSettingsPane`.
- `RikkaHub integration entry point in `docs/开发指南.md` (Development Guide).
- `RikkaHub integration description in `docs/架构概览.md` (Architecture Overview).
- `feature/help/src/main/assets/help/getting-started.md` and `known-issues.md`.
- 
## Current Facts

- MobileIDE has removed the self-developed `feature:ai`, chat repository, channel repository, and tool calling system.
- AI chat, models, channels, MCP, API Key, streaming responses, and stopping generation are maintained entirely by RikkaHub itself.
- The MobileIDE main repository is only responsible for the embedded library dependency, sidebar entry point, settings entry point, host lifecycle, and help documentation.
- RikkaHub source code is located in the `external/rikkahub` submodule; when modifying the submodule, you must commit and push the submodule changes first, and then commit the main repository gitlink.
- Do not add API Key mirror storage, log output, configuration export, or crash attachments in the main repository.
 
## Modification Workflow

1. Determine whether the change belongs to the host entry point or RikkaHub's internal capabilities first.
2. For host entry point changes, prioritize checking `DrawerContent`, `SettingsActivity`, and embedded dependency boundaries.
3. Capabilities such as chat, models, channels, MCP, API Keys, and stopping generation should reside in `external/rikkahub`.
4. If user-visible copy (strings) is located in the MobileIDE main repository, it must use `core/i18n`; if it is located in the RikkaHub submodule, maintain it according to RikkaHub's own resource rules.
5. When help documentation is involved, check and update `feature/help/src/main/assets/help/*.md` synchronously.

## High-Risk Pitfalls

- Do not restore the `feature:ai` or the legacy `AiTool`, `ToolRegistry`, and `AiChannelRepository` chains.
- Do not store copies of RikkaHub API Keys in the main MobileIDE repository.
- Do not move RikkaHub's internal page logic to app/.
- Do not commit only the main repository gitlink while forgetting to push the `external/rikkahub` submodule commits.
- Do not treat confirmed AGP internal deprecation warnings in the RikkaHub Problems report as compilation failures.

## Verification

```powershell
./gradlew :rikkahub:embedded:compileDebugKotlin --console=plain
./gradlew :app:compileArm64DebugKotlin --console=plain
```

- When only modifying the main repository documentation, at least check `git diff`, verify that the paths actually exist, and ensure help assets are synchronized.
- When modifying RikkaHub source code, prioritize running the embedded compile; only run the app compile when the changes involve the main APK host entry point.
