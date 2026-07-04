import os
import zipfile

# Source files
assets_dir = "/data/data/com.termux/files/home/MobileIDE/app/src/main/assets/bundled_plugins"
shared_apk = "/data/data/com.termux/files/home/MobileIDE/plugins/java-lsp/build/outputs/apk/release/java-lsp-release-unsigned.apk"
output_dir = "/data/data/com.termux/files/home"

plugins = [
    "com.scto.mobile.ide.json_lsp",
    "com.scto.mobile.ide.xml_lsp",
    "com.scto.mobile.ide.yaml_lsp",
    "com.scto.mobile.ide.toml_lsp",
    "com.scto.mobile.ide.python_lsp",
    "com.scto.mobile.ide.bash_lsp",
    "com.scto.mobile.ide.cpp_lsp",
]

print("Packaging standalone zip plugins directly to user home (~/):")

for plugin in plugins:
    manifest_path = os.path.join(assets_dir, plugin, "manifest.json")
    zip_path = os.path.join(output_dir, f"{plugin}.zip")
    
    if not os.path.exists(manifest_path):
        print(f"Error: Manifest not found for {plugin}")
        continue

    print(f"- Packaging {plugin}.zip...")
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zip_file:
        # manifest.json at root
        zip_file.write(manifest_path, "manifest.json")
        # extension.apk
        zip_file.write(shared_apk, "extension.apk")

print("Done! All standalone plugins packaged successfully.")
