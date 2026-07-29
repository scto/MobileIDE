import os

def rename_and_modify(d):
    # Rename files and dirs
    for root, dirs, files in os.walk(d, topdown=False):
        for f in files:
            if f == 'LICENSE' or f == 'README.md':
                continue
            new_f = f.replace('Xed', 'MobileIDE').replace('xed', 'mobileide').replace('XED', 'MOBILEIDE')
            if new_f != f:
                os.rename(os.path.join(root, f), os.path.join(root, new_f))
        for d_name in dirs:
            new_d = d_name.replace('Xed', 'MobileIDE').replace('xed', 'mobileide').replace('XED', 'MOBILEIDE')
            if new_d != d_name:
                os.rename(os.path.join(root, d_name), os.path.join(root, new_d))
                
    # Modify contents
    for root, dirs, files in os.walk(d):
        for f in files:
            if f == 'LICENSE' or f == 'README.md':
                continue
            filepath = os.path.join(root, f)
            try:
                with open(filepath, 'r') as file:
                    content = file.read()
            except UnicodeDecodeError:
                continue
            
            new_content = content.replace('Xed-Editor', 'MobileIDE').replace('xed-editor', 'mobileide')
            new_content = new_content.replace('XED', 'MOBILEIDE').replace('Xed', 'MobileIDE').replace('xed', 'mobileide')
            
            if new_content != content:
                with open(filepath, 'w') as file:
                    file.write(new_content)

for d in ['/data/data/com.termux/files/home/MobileIDE/features/extensions', '/data/data/com.termux/files/home/MobileIDE/features/layout-preview']:
    if os.path.exists(d):
        rename_and_modify(d)
