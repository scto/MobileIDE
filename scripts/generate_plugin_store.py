import os
import json

plugins_dir = "plugins"
docs_json = "docs/plugins.json"

plugins_list = []
for plugin_name in os.listdir(plugins_dir):
    manifest_path = os.path.join(plugins_dir, plugin_name, "manifest.json")
    if os.path.exists(manifest_path):
        with open(manifest_path, "r") as f:
            data = json.load(f)
            plugins_list.append(data)

with open(docs_json, "w") as f:
    json.dump(plugins_list, f, indent=2)

print("Generated docs/plugins.json!")
