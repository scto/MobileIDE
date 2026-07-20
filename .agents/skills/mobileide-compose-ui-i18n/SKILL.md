---
name: mobileide-compose-ui-i18n
description: MobileIDE Compose UI、主题、导航和国际化开发指南。用于新增或修改用户界面、Activity setContent、设置页路由、主界面 Tab、底栏/抽屉/弹窗、用户可见文案和多语言资源。
---

# MobileIDE Compose UI 与国际化

## 先读文件

- `app/src/main/java/com/scto/mobile/ide/ui/theme/Theme.kt`：全局主题与颜色定义（`AppTheme`）。
- `core/resources/src/main/java/com/scto/mobile/ide/core/terminal/resources/Res.kt`：资源访问辅助工具（`Int.getString()`）。
- `app/src/main/res/values/strings.xml` 与 `app/src/main/res/values-zh/strings.xml`。
- `core/resources/src/main/res/values/strings.xml` 与 `core/resources/src/main/res/values-zh/strings.xml`。
- `app/src/main/java/com/scto/mobile/ide/MainActivity.kt`。
- `app/src/main/java/com/scto/mobile/ide/MainScreen.kt`：主页 Tab 界面。
- `app/src/main/java/com/scto/mobile/ide/ui/settings/SettingsScreen.kt`：设置页面。
- `core/main/src/main/java/com/scto/mobile/ide/activities/settings/SettingsRoutes.kt`：设置页路由定义。

## UI 约定

- Activity `setContent` 应包含 `AppTheme`。
- 优先复用 Material3 原生组件及已有公共组件，避免重复造轮子。
- `StateFlow` 在 Compose 中优先用 `collectAsStateWithLifecycle` 或 `collectAsState`。
- 主入口是 `MainScreen`，通过 Tab 进行页面切换，而非全局统一 `NavHost`。
- 设置页使用 Jetpack Navigation 路由，以 `SettingsRoutes` 统一管理，通过 `settingsNavController` 导航。

## 国际化规则

- 任何展现给用户的文本（UI、Toast、Snackbar、Dialog、Notification、错误提示等）均需定义 in `strings.xml` 中，避免硬编码。
- 推荐使用 `Int.getString()` / `Int.getFilledString(vararg args)` 访问字符串资源。
- 带有参数的文本应当使用标准的占位符（如 `%s`, `%d`），禁止在 Kotlin 中使用硬编码的字符串拼接。
- 新增字符串需同时维护 `values/strings.xml` 及其翻译（如 `values-zh`、`values-de` 等）。

## 修改流程

1. 寻找相关的 UI 页面及字符串资源。
2. 确保状态管理符合已有的 State Hoisting 和 ViewModel/StateFlow 模式。
3. 增加对应多语言字符串资源。
4. 如果修改了设置项，请一并检查并调整 `SettingsRoutes`。
5. 运行最小化构建进行编译验证。

## 高风险误区

- 严禁在 Kotlin 源码中硬编码用户可见文本。
- 严禁绕过全局 `AppTheme`。
- 不要将首页 Tab 与 Jetpack Navigation 路由混淆。

## 验证

- UI 或文案修改后，至少运行 `./gradlew :app:compileArm64DebugKotlin --console=plain` 确保无编译错误。
