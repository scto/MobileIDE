import os
import glob
import re

directories = ["core/extension", "core/lsp", "core/commands"]

def refactor_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Remove kotlinx.serialization annotations and imports
    content = re.sub(r'import kotlinx\.serialization\.Serializable\n', '', content)
    content = re.sub(r'import kotlinx\.serialization\.ExperimentalSerializationApi\n', '', content)
    content = re.sub(r'@Serializable\s*', '', content)
    content = re.sub(r'@OptIn\(ExperimentalSerializationApi::class\)\s*', '', content)
    
    # Replace json parsing with gson
    if 'import kotlinx.serialization.json.Json' in content:
        content = re.sub(r'import kotlinx\.serialization\.json\.Json\n', 'import com.google.gson.GsonBuilder\n', content)
        content = re.sub(r'val json = Json \{ ignoreUnknownKeys = true \}', 'val gson = GsonBuilder().create()', content)
        content = re.sub(r'json\.decodeFromString<([^>]+)>\(([^)]+)\)', r'gson.fromJson(\2, \1::class.java)', content)
    
    # Remove App, DefaultScope, FileObject, LogCollector imports
    content = re.sub(r'import com\.rk\.App\n', '', content)
    content = re.sub(r'import com\.rk\.DefaultScope\n', '', content)
    content = re.sub(r'import com\.rk\.file\.FileObject\n', '', content)
    content = re.sub(r'import com\.rk\.settings\.debugOptions\.LogCollector\n', '', content)
    content = re.sub(r'import com\.rk\.crashhandler\.CrashActivity\n', '', content)
    content = re.sub(r'import com\.rk\.utils\.isMainThread\n', 'import android.os.Looper\n', content)
    content = re.sub(r'import com\.rk\.utils\.application\n', '', content)

    # Refactor usages
    content = content.replace('isMainThread()', '(Looper.myLooper() == Looper.getMainLooper())')
    content = content.replace('CoroutineScope by DefaultScope', '')
    content = content.replace('App.extensionManager', '/* TODO: extensionManager */ null')
    content = content.replace('LogCollector.reportDebug', 'android.util.Log.d')
    content = content.replace('LogCollector.reportInfo', 'android.util.Log.i')
    content = content.replace('LogCollector.reportWarn', 'android.util.Log.w')
    content = content.replace('LogCollector.reportError', 'android.util.Log.e')
    content = content.replace('errorDialog(e)', 'android.util.Log.e("Error", "Error", e)')
    content = content.replace('CrashActivity.start', 'android.util.Log.e("Crash", "Crash", ')
    content = content.replace('FileObject', 'java.io.File')

    # Remove internal package aliases
    content = content.replace('typealias Serializable = Serializable\n', '')
    
    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Refactored {filepath}")

for d in directories:
    for root, dirs, files in os.walk(d):
        for f in files:
            if f.endswith('.kt'):
                refactor_file(os.path.join(root, f))
