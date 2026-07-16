import os
import glob
import re

directory = "core/lsp/src/main/java/com/rk/lsp/servers"

for filepath in glob.glob(os.path.join(directory, "*.kt")):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Remove all Xed utility imports so it uses the ones from LspStubs in com.rk.lsp
    content = re.sub(r'import com\.rk\.file\..*\n', '', content)
    content = re.sub(r'import com\.rk\.utils\..*\n', '', content)
    content = re.sub(r'import com\.rk\.NpmUtils\n', '', content)
    content = re.sub(r'import com\.rk\.BuiltinFileType\n', '', content)
    content = re.sub(r'import com\.rk\.RunnerImpl\n', '', content)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
        
print("Removed Xed imports from LSP servers")
