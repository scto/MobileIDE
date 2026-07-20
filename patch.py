import os
import glob

# 1. Patch build.gradle.kts for all plugins
for path in glob.glob("plugins/*/app/build.gradle.kts"):
    with open(path, "r") as f:
        content = f.read()
    content = content.replace("JavaVersion.VERSION_21", "JavaVersion.VERSION_17")
    content = content.replace("jvmToolchain(21)", "jvmToolchain(17)")
    with open(path, "w") as f:
        f.write(content)
    print(f"Patched Java version in {path}")

# 2. Patch KmpServer.kt
kmp_server = "plugins/kotlin-kmp-lsp/app/src/main/java/com/scto/mobile/ide/plugins/kotlin/kmp/lsp/KmpServer.kt"
if os.path.exists(kmp_server):
    with open(kmp_server, "r") as f:
        content = f.read()
    if "hasUpdate" not in content:
        content = content.replace("override fun update(activity: Activity) {", "override suspend fun hasUpdate(context: Context): Boolean = false\n\n    override fun update(activity: Activity) {")
        with open(kmp_server, "w") as f:
            f.write(content)
        print("Patched KmpServer.kt")

# 3. Patch Main.kt
main_kt = "plugins/kotlin-kmp-lsp/app/src/main/java/com/scto/mobile/ide/plugins/kotlin/kmp/lsp/Main.kt"
if os.path.exists(main_kt):
    with open(main_kt, "r") as f:
        content = f.read()
    if "onLoad" not in content:
        content = content.replace("override fun onExtensionLoaded() {", "override fun onLoad() {}\n\n    override fun onExtensionLoaded() {")
    content = content.replace("override fun onUpdated()", "fun onUpdated()")
    with open(main_kt, "w") as f:
        f.write(content)
    print("Patched Main.kt")

