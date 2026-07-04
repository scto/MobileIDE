---
name: mobileide-architecture-navigation
description: MobileIDE project architecture navigation. Used to understand module boundaries, startup entry points, multi-process initialization, main interface/editor data flow, or to determine whether new code should be placed in app, core, feature, external, tools, or build-logic.
---

# MobileIDE Architecture Navigation

## Read First

- `settings.gradle.kts`: Module list, included build, external source code substitution.
- `docs/Architecture Overview.md` (docs/架构概览.md), `docs/Module Functional Specifications.md` (docs/模块功能说明.md): If these files exist, use the project documentation as the entry point first.
- `app/src/main/AndroidManifest.xml`: Application, Activity, provider, permissions, and process declarations.
- `app/src/main/java/com/scto/mobile/ide/MobileIDEApplication.kt`: Initialization shunting for the main process, toolchain process, crash process, and user native runtime.
- `app/src/main/java/com/scto/mobile/ide/ui/MainPortalActivity.kt`: Startup entry point.
- `app/src/main/java/com/scto/mobile/ide/MainActivity.kt`: Project editor workspace entry point.
- `app/src/main/java/com/scto/mobile/ide/ui/compose/screens/main/MainScreen.kt`: Home tab entry point.
- `app/src/main/java/com/scto/mobile/ide/ui/compose/screens/main/MainActivityScreenHost.kt`: Main editor interface assembly.

## Module Boundaries

- `app/` is the startup, navigation, DI assembly, and cross-module coordination layer; do not continue stuffing domain logic into Activities or Hosts.
- `feature/*` are user-visible functional slices, such as settings, workspace, editor, help, and preview-related features.
- `core/*` are UI-less reusable capabilities and runtime infrastructure, such as i18n, designsystem, storage, security, database, compile, lsp, plugin, and tree-sitter.
- `external/*` maintains third-party source code or local fork boundaries; confirm whether the code is synchronized with upstream before making changes.
- `tools/*` stores templates, verification, and build helper `scripts`; do not place runtime code here.
- `build-logic/` stores Gradle convention plugins; prioritize reusing existing build logic such as ABI aggregation, versioning, R8, Tree-sitter, and toolchain assets.

## Determining the Location of New Code

- First, use rg or ace-tool.search_context to search for existing implementations by intent.
- User-visible pages, ViewModels, and functional workflows should prioritize being placed in the corresponding `feature/*`.
- Capabilities shared by multiple `features/apps` that do not depend on the UI should be placed in `core/*`.
- Only modify `app/` when dealing with cross-module assembly, Activity entry points, global DI, or main interface coordination.
- Third-party source code, generated files, and toolchain assets should only be modified in their respective maintenance workflows.

## Must Remember

- `MainPortalActivity` is the startup portal, and `MainActivity` is the project editor workspace.
- `MobileIDEApplication` has multi-process shunting; `:toolchain`, `:crash`, and user native runtime processes must not accidentally initialize main process services.
- Default compilation/LSP depends on native mobileide-toolchain + Android sysroot; PRoot is an optional Linux environment, not the default path.
- AI chat, models, channels, and MCP are maintained by embedded RikkaHub; the MobileIDE main repository only maintains packaging, entry points, and host boundaries.
- The main interface does not have a globally unified `NavHost;` the home page uses `MainScreen` Tab + Activity/callback-style navigation.
- Newly added user-visible copy (strings) must use `core/i18n` resources and wrappers.

## High-Risk Pitfalls

- Do not move business logic from `feature/*` up to `app/`.
- Do not initialize main process components like logging, crash reporting, Tree-sitter, or Koin outside of the multi-process Application branches.
- Do not manually modify the generated Tree-sitter registry or integrate manual synchronization tasks into the regular build.
- Do not bypass `ProjectPaths` to manually splice persistence paths for projects, logs, PRoot, templates, etc.

## Verification

- After structural adjustments, run compilation for the target module, for example: `./gradlew :app:compileArm64DebugKotlin --console=plain`.
- When dealing with module boundaries, check `settings.gradle.kts` and the dependency direction in the corresponding `build.gradle.kts`.
- When dealing with the main interface, prioritize running the related Activity, navigation, settings, or editor tests in `src/test`.
