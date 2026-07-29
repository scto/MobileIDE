import os
import shutil

def rename_dir_and_files():
    base = '/data/data/com.termux/files/home/MobileIDE/features'
    # Step 2: Rename terminal/xed-cli to terminal/mobileide-cli
    xed_cli = os.path.join(base, 'terminal', 'xed-cli')
    mobileide_cli = os.path.join(base, 'terminal', 'mobileide-cli')
    if os.path.exists(xed_cli):
        os.rename(xed_cli, mobileide_cli)

    target_dirs = [
        os.path.join(base, 'git'),
        os.path.join(base, 'runner'),
        os.path.join(base, 'terminal'),
        os.path.join(base, 'extensions_migrated') # I'll just process this as well
    ]

    # Step 4: Rename files containing 'xed' or 'Xed'
    for d in target_dirs:
        if not os.path.exists(d):
            continue
        for root, dirs, files in os.walk(d, topdown=False):
            # rename files
            for f in files:
                new_f = f
                if 'xed' in new_f:
                    new_f = new_f.replace('xed', 'mobileide')
                if 'Xed' in new_f:
                    new_f = new_f.replace('Xed', 'MobileIDE')
                
                if new_f != f:
                    os.rename(os.path.join(root, f), os.path.join(root, new_f))
            
            # rename dirs
            for d_name in dirs:
                new_d = d_name
                if 'xed' in new_d:
                    new_d = new_d.replace('xed', 'mobileide')
                if 'Xed' in new_d:
                    new_d = new_d.replace('Xed', 'MobileIDE')
                
                if new_d != d_name:
                    os.rename(os.path.join(root, d_name), os.path.join(root, new_d))

def modify_contents():
    base = '/data/data/com.termux/files/home/MobileIDE/features'
    target_dirs = [
        os.path.join(base, 'git'),
        os.path.join(base, 'runner'),
        os.path.join(base, 'terminal'),
        os.path.join(base, 'extensions_migrated')
    ]

    for d in target_dirs:
        if not os.path.exists(d):
            continue
        for root, dirs, files in os.walk(d):
            for f in files:
                filepath = os.path.join(root, f)
                try:
                    with open(filepath, 'r') as file:
                        content = file.read()
                except UnicodeDecodeError:
                    continue # Skip binary files
                
                new_content = content
                
                # Exception: Leave LICENSE and README intact for attribution
                if f == 'LICENSE' or f == 'README.md':
                    continue

                # Step 3: Replace package and import
                new_content = new_content.replace('com.rk.terminal', 'com.scto.mobile.ide.features.terminal')
                new_content = new_content.replace('com.rk.git', 'com.scto.mobile.ide.features.git')
                new_content = new_content.replace('com.rk.runner', 'com.scto.mobile.ide.features.runner')
                new_content = new_content.replace('com.rk.extension', 'com.scto.mobile.ide.features.extension') # guessing
                new_content = new_content.replace('com.rk.', 'com.scto.mobile.ide.') 
                
                # Step 4: Text Replacement
                # "xed" -> "mobileide"
                # "Xed" -> "MobileIDE"
                # "XED" -> "MOBILEIDE"
                # "Xed-Editor" / "xed-editor" -> handled by above if we do it carefully
                # Wait, 'Xed-Editor' becomes 'MobileIDE-Editor' which is fine, or 'MobileIDE'.
                
                new_content = new_content.replace('Xed-Editor', 'MobileIDE')
                new_content = new_content.replace('xed-editor', 'mobileide')
                
                new_content = new_content.replace('XED', 'MOBILEIDE')
                new_content = new_content.replace('Xed', 'MobileIDE')
                new_content = new_content.replace('xed', 'mobileide')

                # Step 2: "mobileide <command>" -> "mobileide <command>" (already done by xed->mobileide)
                
                if new_content != content:
                    with open(filepath, 'w') as file:
                        file.write(new_content)

if __name__ == '__main__':
    rename_dir_and_files()
    modify_contents()
    print("Migration script executed.")
