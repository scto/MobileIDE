import os

dirs = ["/data/data/com.termux/files/home/MobileIDE/core/runner", "/data/data/com.termux/files/home/MobileIDE/core/terminal"]

replacements = {
    "com.scto.mobile.ide.settings.Settings": "com.scto.mobile.ide.core.terminal.settings.Settings",
    "com.scto.mobile.ide.utils.toast": "com.scto.mobile.ide.core.common.utils.toast",
    "com.scto.mobile.ide.resources.getFilledString": "com.scto.mobile.ide.core.terminal.resources.getFilledString",
    "com.scto.mobile.ide.resources.getString": "com.scto.mobile.ide.core.terminal.resources.getString",
    "com.scto.mobile.ide.resources.strings": "com.scto.mobile.ide.core.terminal.resources.strings"
}

for d in dirs:
    for root, _, files in os.walk(d):
        for f in files:
            if f.endswith(".kt"):
                path = os.path.join(root, f)
                with open(path, "r") as file:
                    content = file.read()
                
                new_content = content
                for old, new in replacements.items():
                    new_content = new_content.replace(old, new)
                
                if new_content != content:
                    with open(path, "w") as file:
                        file.write(new_content)
                    print(f"Updated {path}")
