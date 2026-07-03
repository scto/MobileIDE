import os
import glob
import re

directory = "core/lsp/src/main/java/com/rk/lsp/servers"

for filepath in glob.glob(os.path.join(directory, "*.kt")):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Add import com.rk.lsp.* if not present
    if "import com.rk.lsp.*" not in content:
        content = re.sub(r'(package com\.rk\.lsp\.servers\n)', r'\1\nimport com.rk.lsp.*\n', content)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
        
print("Added com.rk.lsp.* import to LSP servers")
