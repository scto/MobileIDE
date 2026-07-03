import os
import glob
import re

directory = "core/lsp/src/main/java/com/rk/lsp/servers"

for filepath in glob.glob(os.path.join(directory, "*.kt")):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    content = re.sub(r'import com\.rk\.exec\..*\n', '', content)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
        
print("Removed exec imports from LSP servers")
