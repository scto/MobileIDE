import os

def fix_file(path):
    with open(path, 'r') as f:
        content = f.read()
    
    content = content.replace('import androidx.compose.material.icons.filled.Error', 'import androidx.compose.material.icons.filled.Info')
    content = content.replace('Icons.Default.Error', 'Icons.Default.Info')
    content = content.replace('Icons.Filled.Error', 'Icons.Filled.Info')
    
    with open(path, 'w') as f:
        f.write(content)

fix_file('/data/data/com.termux/files/home/MobileIDE/core/runner/src/main/java/com/scto/mobile/ide/settings/runners/RunnerSettings.kt')
print("Done")
