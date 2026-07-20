---
name: mobileide-build-release
description: MobileIDE build, Gradle, ABI, CI, release, signing, and build verification guide. Used for handling assemble/compile failures, APK builds, product flavors, versions, and signing configuration.
---

# MobileIDE Build and Release

## 先读文件

- `gradle/wrapper/gradle-wrapper.properties`：Gradle Wrapper 版本配置（当前为 Gradle 9.6.0）。
- `settings.gradle.kts`：包含的子模块定义。
- `build.gradle.kts`：根目录 Gradle 配置。
- `gradle/libs.versions.toml`：依赖项版本及应用程序版本信息（`versionCode`、`versionName`、`applicationId`）。
- `app/build.gradle.kts`：包含 `Fdroid` / `PlayStore` 渠道（flavors）定义、签名（Signing）及编译打包配置。
- `app/keystore.properties`：签名文件配置文件路径和口令（实际证书密码由用户本地控制，不能提交）。

## 常用命令

```powershell
# 编译 Fdroid Debug 版本
./gradlew :app:compileFdroidDebugKotlin --console=plain

# 构建 Fdroid Debug APK
./gradlew :app:assembleFdroidDebug --console=plain

# 构建 PlayStore Debug APK
./gradlew :app:assemblePlayStoreDebug --console=plain

# 运行代码格式检查 (项目使用的是 ktfmt 插件而非 ktlint)
./gradlew ktfmtCheck --console=plain

# 自动格式化代码
./gradlew ktfmtFormat --console=plain
```

## 项目构建事实

- **版本号控制**：版本号集中托管在 `gradle/libs.versions.toml` 中，杜绝在各模块中硬编码。
- **渠道（Product Flavors）**：
  - `Fdroid`：面向 F-Droid 商店，限制 `targetSdk = 28`（以规避某些高版本 Android 特权限制）。
  - `PlayStore`：面向 Google Play，设置 `targetSdk = 35`。
- **打包签名**：`release` 构建使用 `app/build.gradle.kts` 中的 `acsProps` 获取 `keystore.properties`。在未配置该属性时默认回退。
- **代码混淆/裁剪**：当前版本的 `release` 配置中，`isMinifyEnabled` 和 `isShrinkResources` 默认设为 `false`。

## 高风险误区

- 严禁提交任何真实的 keystore 密钥证书或包含实际密码的 `keystore.properties`。
- 新增依赖时应优先写入 `gradle/libs.versions.toml`。
- 进行代码验证时请统一使用 `ktfmt` 格式化命令。

## 验证

- 任何涉及构建、配置及依赖的修改，至少运行对应渠道的编译任务（如 `./gradlew :app:compileFdroidDebugKotlin --console=plain`）以确保无编译失败。