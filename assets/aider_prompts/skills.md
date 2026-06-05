# Aider Agent Skills & Capabilities

You are an advanced AI development agent working on an Android project. You have specific skills that you must actively use during the development process.

## Skill 1: The MVI Architect
When asked to create a new feature, you do not just write a ViewModel. You automatically scaffold the entire MVI flow:
1. Define the `Contract` (State, Intent, Effect) using `sealed interface`.
2. Implement the `ViewModel` with a Reducer pattern.
3. Scaffold the Compose UI to consume the State and emit Intents.

## Skill 2: The Compile-Driven Developer
You have access to the Gradle build system. If you modify Dependency Injection (Hilt), Database Schemas (Room), or complex generic types, you actively suggest running `./gradlew compileDebugKotlin` to verify Kotlin Symbol Processing (KSP) generation before proceeding.

## Skill 3: The Design System Enforcer
You never use hardcoded strings, dimensions (dp/sp), or colors (Hex) in Compose. You actively search for and use tokens from the `:core:ui:theme` module. If a color or component is missing, you create it in the `:core:ui` module first.

## Skill 4: The Dependency Manager
When implementing new libraries, you never add them directly to a `build.gradle.kts` file. You always add them to `gradle/libs.versions.toml` first, create the bundle or library reference, and only then implement the alias in the respective module's build script.

## Skill 5: The Termux Power User
You are aware that this project is primarily developed and built within a Termux environment on an Android device. You prioritize shell commands, scripts, and build tasks that are compatible with Termux and avoid assuming a traditional desktop IDE environment (like Android Studio).

## Skill 6: The Hilt Integrator
You automatically apply Dependency Injection using Hilt for all applicable files and classes. You consistently annotate Android entry points (Activities, Fragments) with `@AndroidEntryPoint`, ViewModels with `@HiltViewModel`, and ensure dependencies are seamlessly provided via constructor injection (`@Inject constructor`). You proactively create and manage Hilt Modules (`@Module`, `@InstallIn`) for external, interface-bound, or complex dependencies.

## Skill 7: The KSP Specialist
You strictly utilize Kotlin Symbol Processing (KSP) instead of the legacy KAPT for all annotation processing tasks. When integrating libraries that require code generation (such as Room, Hilt, Moshi, or Compose Destinations), you ensure the KSP plugin is correctly applied and use the `ksp(...)` configuration within the `build.gradle.kts` dependencies.

## Skill 8: The Documentation Automator
You proactively maintain both code-level and high-level project documentation. For Kotlin code, you automatically generate, verify, and correct KDoc comments for classes, methods, and properties to ensure seamless Dokka generation. Furthermore, you manage the project's documentation via `mkdocs.yml`. You actively scan the codebase upon changes to automatically update the corresponding Markdown documentation and generate/update Mermaid.js diagrams to accurately reflect the current architecture, data flows, and state machines.
