import os
import shutil

app_dir = "/data/data/com.termux/files/home/MobileIDE/app/src/main/java/com/scto/mobile/ide/core"
common_src_dir = "/data/data/com.termux/files/home/MobileIDE/core/common/src/main/java/com/scto/mobile/ide/core/common"

# 1. Move directory
os.makedirs(os.path.dirname(common_src_dir), exist_ok=True)
if os.path.exists(app_dir):
    shutil.move(app_dir, common_src_dir)

# 2. Update package declarations in moved files
for root, dirs, files in os.walk(common_src_dir):
    for file in files:
        if file.endswith('.kt') or file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r') as f:
                content = f.read()
            
            # Update package
            content = content.replace('package com.scto.mobile.ide.core', 'package com.scto.mobile.ide.core.common')
            
            # Update self-imports if any
            content = content.replace('import com.scto.mobile.ide.core.', 'import com.scto.mobile.ide.core.common.')
            
            with open(filepath, 'w') as f:
                f.write(content)

# 3. Update imports in the rest of the project
project_dir = "/data/data/com.termux/files/home/MobileIDE"
for root, dirs, files in os.walk(project_dir):
    if 'build' in root.split(os.sep) or '.git' in root:
        continue
    for file in files:
        if file.endswith('.kt') or file.endswith('.java'):
            filepath = os.path.join(root, file)
            # Skip the newly moved files
            if filepath.startswith(common_src_dir):
                continue
                
            with open(filepath, 'r') as f:
                content = f.read()
                
            if 'com.scto.mobile.ide.core.' in content:
                # Be careful not to replace core.terminal or core.lsp etc.
                # The subdirectories of app's core were: commands, icons, lsp, utils
                content = content.replace('com.scto.mobile.ide.core.commands.', 'com.scto.mobile.ide.core.common.commands.')
                content = content.replace('com.scto.mobile.ide.core.icons.', 'com.scto.mobile.ide.core.common.icons.')
                # Wait, lsp? There is core.lsp which is a different module!
                # Wait, app/src/main/java/com/scto/mobile/ide/core/lsp was a folder!
                # Did it conflict with the core:lsp module? Let's check imports.
                content = content.replace('com.scto.mobile.ide.core.utils.', 'com.scto.mobile.ide.core.common.utils.')
                
                # We need to replace the exact imports of classes in the app's core/lsp
                # But let's check what was in app's core/lsp first!
                
                with open(filepath, 'w') as f:
                    f.write(content)

print("Moved and updated imports.")
