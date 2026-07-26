# Layout Preview Module (`:features:layout-preview`)

## Architectural Design Decision: Build-Based & Screenshot-Testing Preview Render Mode

### Background
Evaluating `@Composable` functions live on-device directly within the host app process without running the Kotlin Compiler is technically unfeasible for arbitrary user code. Android Compose relies heavily on compiler plugins (`compose-compiler`) to transform `@Composable` functions into stateful positional-memoization trees (`Composer`).

### Design Strategy
`:features:layout-preview` implements a **Build-Based Preview Mode**:
1. **Composable Scanner**: Scans active `.kt` source files for `@Composable` / `@Preview` functions using lightweight AST/Regex parsing (`ComposePreviewScanner`).
2. **Preview Compilation Task**: Upon preview request (or auto-refresh on save), an isolated background build task (`compilePreviewKotlin` / Gradle task) compiles the target Composable file into a temporary preview runner binary.
3. **Bitmap Extraction & Rendering**: Renders the Composable layout into a bitmap image (`Bitmap` / `ImageBitmap`) via background screenshot testing principles and returns the result asynchronously to the Compose state tree without blocking UI threads.
4. **Target SLA**: < 10 seconds for standard Composable functions on mobile hardware.
