import os
import glob
import shutil

# Revert to Java 21
for path in glob.glob("plugins/*/app/build.gradle.kts"):
    with open(path, "r") as f:
        content = f.read()
    content = content.replace("JavaVersion.VERSION_17", "JavaVersion.VERSION_21")
    content = content.replace("jvmToolchain(17)", "jvmToolchain(21)")
    with open(path, "w") as f:
        f.write(content)
    print(f"Reverted Java version to 21 in {path}")

# Copy gradle.properties
root_props = "gradle.properties"
if os.path.exists(root_props):
    for plugin_dir in glob.glob("plugins/*-lsp"):
        shutil.copy(root_props, os.path.join(plugin_dir, "gradle.properties"))
        print(f"Copied gradle.properties to {plugin_dir}")
        
