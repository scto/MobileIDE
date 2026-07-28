# MobileIDE Plugin & Extension Development Guide

## 1. Introduction
MobileIDE features an extensible plugin system allowing developers to integrate custom Language Server Protocol (LSP) servers, syntax highlighters, task runners, developer tools, and UI extensions. Plugins are dynamically loaded at runtime using a specified `mainClass` that implements extension lifecycle methods (based on interfaces in `com.scto.mobile.ide.extension.*` or `com.scto.mobile.ide.features.extensions.*`).

---

## 2. Prerequisites
To develop plugins for MobileIDE, ensure you have the following tools installed:
- **JDK 17+** (or JDK 21)
- **Android SDK & Build-Tools** (API 34/35)
- **Gradle 8.x+**
- **MobileIDE SDK / Core Common Libraries** (`sdk.jar` or project dependencies)

---

## 3. `manifest.json` Structure

Every plugin **must** include a `manifest.json` file in its root directory. Below is the standard schema:

```json
{
  "id": "com.scto.mobile.ide.example_plugin",
  "name": "Example Plugin",
  "mainClass": "com.scto.mobile.ide.plugins.example.Main",
  "version": "1.0.0",
  "description": "An example plugin for MobileIDE",
  "author": {
    "displayName": "Developer Name",
    "github": "username"
  },
  "repository": "https://github.com/example/example-plugin",
  "license": "MIT",
  "tags": ["lsp", "example", "kotlin"],
  "minAppVersion": 1,
  "maxAppVersion": null
}
```

### Field Specification:
| Field | Required | Description |
|---|---|---|
| `id` | **Yes** | Unique package identifier in reverse domain notation (e.g., `com.scto.mobile.ide.bash_lsp`). |
| `name` | **Yes** | Display name shown in the MobileIDE user interface. |
| `mainClass` | **Yes** | Fully qualified class name of the plugin entry point. |
| `version` | **Yes** | Version number following Semantic Versioning (`x.y.z`). |
| `description` | No | Concise summary of the plugin's functionality. |
| `author.displayName` | No | Name of the author or development team. |
| `repository` | No | URL to the plugin's source code repository. |
| `license` | No | Open-source or proprietary license (e.g., `MIT`, `GPLv3`, `Apache-2.0`). |
| `tags` | No | Array of keywords for categorization (e.g., `["lsp", "linter"]`). |

> [!IMPORTANT]
> **Required Fields**: If any of `id`, `name`, `mainClass`, or `version` is missing, or if `manifest.json` contains invalid JSON syntax, MobileIDE will skip loading the plugin.

---

## 4. Typical Extension Project Structure

```
/plugin-root
  ├── manifest.json
  ├── icon.png (optional)
  ├── build.gradle.kts (or build.gradle)
  ├── settings.gradle.kts
  └── app/
      ├── src/main/java/com/scto/mobile/ide/plugins/example/
      │   ├── Main.kt
      │   └── ExampleServer.kt
      └── src/main/res/ (optional)
```

---

## 5. Step-by-Step Tutorial

### Step 1: Create the Plugin Project
Create a new directory for your plugin (e.g., `plugins/my-plugin`).

### Step 2: Create `manifest.json`
Add `manifest.json` in the root directory and populate all required fields (`id`, `name`, `mainClass`, `version`).

### Schritt 3: Implement `mainClass`
Create the entry point class for your extension:

```kotlin
package com.scto.mobile.ide.plugins.example

class Main {
    fun onEnable() {
        println("Example plugin successfully enabled!")
    }

    fun onDisable() {
        println("Example plugin disabled.")
    }
}
```

### Step 4: Package the Plugin
Plugins can be packaged as **ZIP** archives (for generic/non-Android plugins) or as **APK** files (for compiled Android extensions).

**Note**: The `manifest.json` **must** reside at the root of the ZIP archive or inside the APK assets directory.

File Naming Scheme:
- `<id>-<version>.zip` (e.g., `com.scto.mobile.ide.bash_lsp-1.0.0.zip`)
- `<id>-<version>.apk` (e.g., `com.scto.mobile.ide.example-1.0.0.apk`)

### Step 5: Sideload & Test
Copy or move the generated archive to MobileIDE's `assets` directory:
`~/MobileIDE/assets/<id>-<version>.zip`

MobileIDE automatically detects and registers the extension during application startup.

---

## 6. Complete Example: Hello World Extension

### `manifest.json`
```json
{
  "id": "com.scto.mobile.ide.helloworld",
  "name": "Hello World Extension",
  "mainClass": "com.scto.mobile.ide.plugins.helloworld.Main",
  "version": "1.0.0",
  "description": "Demonstrates basic extension structure for MobileIDE.",
  "author": {
    "displayName": "MobileIDE Team"
  },
  "license": "MIT",
  "tags": ["example", "hello-world"]
}
```

### `Main.kt`
```kotlin
package com.scto.mobile.ide.plugins.helloworld

class Main {
    fun init() {
        System.out.println("Hello World Extension loaded successfully!")
    }
}
```

---

## 7. Best Practices

- **Unique IDs**: Always use your own domain in reverse notation (e.g., `com.mydomain.plugin.feature`) to prevent naming collisions.
- **Semantic Versioning**: Adhere strictly to `MAJOR.MINOR.PATCH` (e.g., `1.0.2`) for accurate update handling.
- **Validation**: Validate your `manifest.json` with a JSON linter before packaging (avoid unescaped control characters or unclosed quotes).
- **Lightweight Assets**: Keep archive sizes minimal and download large binaries dynamically via setup scripts.

---

## 8. Troubleshooting & Common Issues

| Issue | Root Cause | Solution |
|---|---|---|
| **Plugin Not Detected** | Missing `manifest.json` or invalid JSON syntax. | Validate `manifest.json` syntax using a JSON linter. |
| **`ClassNotFoundException`** | Class specified in `mainClass` does not match the compiled binary. | Verify the fully qualified package and class name in `mainClass`. |
| **`Missing Required Fields`** | One or more of `id`, `name`, `mainClass`, or `version` is missing. | Ensure all 4 mandatory fields are populated in `manifest.json`. |

---

## 9. Reference Implementation
For a full working reference, inspect the **Bash Language Support** plugin:
- **ID**: `com.scto.mobile.ide.bash_lsp`
- **Path**: `assets/bundled_plugins/com.scto.mobile.ide.bash_lsp`
