# Aider Execution & Architectural Guardrails

Whenever you write or modify code in this Android project, you must act as a strict Architectural Guardian. Enforce these specific constraints on your own output:

1. **The Single-File-Refactor Rule:**
   Never write UI, Domain, and Data layer code in a single step or file. Always execute feature requests strictly in this order:
   - Step 1: Data layer (DTOs, Repository implementation)
   - Step 2: Domain layer (Models, UseCases, Repository interfaces)
   - Step 3: UI layer (MVI Contract, ViewModel, Compose UI)

2. **The "No-Var" Enforcement:**
   Scan your generated Kotlin code. The `var` keyword is strictly forbidden in class properties or data classes. It is ONLY permitted inside local function algorithms or `remember { mutableStateOf() }` blocks. Refactor to `val` and `copy()` operations before submitting.

3. **Strict Return Type Enforcement:**
   Never rely on Kotlin's type inference for public functions or Flows. You MUST explicitly declare the return type for every function (e.g., `fun getUser(): Flow<User>`).

4. **Multi-Module Dependency Audit:**
   Before importing a class from another module, you must check the `build.gradle.kts` of the current module. If the dependency (e.g., `implementation(projects.core.ui)`) is missing, you must add it to the build script first.
