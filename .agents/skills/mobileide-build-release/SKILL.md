---
name: mobileide-build-release
description: MobileIDE build, Gradle, ABI, CI, release, signing, and R8 troubleshooting guide. Used for handling assemble/compile failures, APK builds, version numbers, GitHub Actions, toolchain asset verification, or Release obfuscation issues.
---

# MobileIDE Build and Release

## Read the files first

- `gradle/wrapper/gradle-wrapper.properties`: Gradle Wrapper version, currently pointing to Gradle 9.6.0.
- `settings.gradle.kts`: `includeBuild("build-logic")`, module and external tree-sitter replacement.
- `build.gradle.kts`: Root plugin, ktlint configuration.
- `gradle/libs.versions.toml`: AGP, Kotlin, AGP `legacy-kapt` plugin, Compose BOM, and dependency versions.
- `app/build.gradle.kts`: ABI flavor, Release, signing, R8 configuration.
- `build-logic/convention/src/main/kotlin/**`: ABI aggregation, versioning, mapping, Tree-sitter, toolchain assets verification, and other convention plugins.
- `.github/workflows/**`: dev, PR, and release build matrices.
- `version.properties`, `keystore.properties.example`, `docs/proguard-rules-reference.md`.

## Commonly Used Commands

```powershell
./gradlew :app:compileArm64DebugKotlin --console=plain
./gradlew :app:assembleArm64Debug --console=plain
./gradlew -Pmobileide.devAbi=x86_64 :app:assembleX86_64Debug --console=plain
./gradlew :app:assembleDebugAllAbi --console=plain
./gradlew :app:assembleArm64Release --console=plain
./gradlew ktlintCheck --console=plain

```

- The Windows helper script exists: `tools/build-apk.ps1`.
- `tools/build-apk.ps1 -Universal` will temporarily write `app/build.gradle.kts`; you must confirm the impact before execution.

````

## Project Facts
- Dependency versions are centralized in `gradle/libs.versions.toml`; avoid scattering hard-coded versions throughout modules.
- `app` flavors are `arm64` and `x86_64`, with the local default being `mobileide.devAbi=arm64`.
- Use `:app:assembleDebugAllAbi`, `:app:assembleReleaseAllAbi`, or `-Pmobileide.allAbi=true` for all ABIs.
- Releases are enabled by default with `isMinifyEnabled = true` and `isShrinkResources = true`.
- Release signing reads `keystore.properties`; the actual keystore and password cannot be submitted.
- Non-tagged releases may auto-increment `version.properties` during `assemble/bundle/install`.
- Tag releases require `v*` to be equal to `versionName` after removing the `v`.
- Release builds back up R8 mappings; mapping files are only archived locally by the public build logic.
- CI uses JDK 17, CMake 3.22.1, tree-sitter-cli 0.22.1, and recursively checks out submodules.

## Reusable Entry Points

- ABI aggregation: `MobileAndroidAppAbiAggregationPlugin`.
- Version increment: `MobileAndroidAppVersioningPlugin`.
- Mapping backup: `MobileAndroidAppMappingPlugin`.
- Tree-sitter registry and ktlint task integration: `MobileAndroidAppTreeSitterPlugin`.
- Toolchain asset integrity: `verifyMobileToolchainAssets` related build logic.
- Automatic registration of library modules `consumer-rules.pro`: `MobileAndroidLibraryPlugin`.

## Prohibited Practices

- Do not duplicate `assembleDebugAllAbi` or reimplement version incrementing.
- Do not attach `syncTreeSitterQueries` to a regular build; it is a manual network task.
- Do not redirect Gradle build artifacts to `LOCALAPPDATA/MobileIDE/gradle-out/**`.
- Do not read, print, or commit actual `keystore.properties`, keystore, or CI secrets.
- Adding third-party libraries must evaluate R8 keep rules, preferably written in the `consumer-rules.pro` of the importing module.
- Do not restore or copy `docs/workflows/receive-release.yml` to `.github/workflows/`; the release chain for this private repository `repository_dispatch` is deprecated.

## Verification

- For documentation or script changes, at least check `git diff -- AGENTS.md .agents tools build-logic app/build.gradle.kts`.
- For build logic changes, run the smallest target first: `:app:compileArm64DebugKotlin`.
- For ABI or packaging changes, run the corresponding assemble.
- For R8/Release changes, ensure the user explicitly accepts the version/mapping side effects before running the target release assemble, and check that keep-alive rules are in the correct module.