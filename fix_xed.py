import os

def fix_mistakes(d):
    for root, dirs, files in os.walk(d):
        for f in files:
            filepath = os.path.join(root, f)
            try:
                with open(filepath, 'r') as file:
                    content = file.read()
            except UnicodeDecodeError:
                continue
            
            new_content = content.replace('Indemobileide', 'Indexed').replace('IndeMobileIDE', 'Indexed')
            new_content = new_content.replace('indemobileide', 'indexed').replace('indeMobileIDE', 'indexed')
            new_content = new_content.replace('fimobileide', 'fixed').replace('fiMobileIDE', 'fixed')
            new_content = new_content.replace('Fimobileide', 'Fixed').replace('FiMobileIDE', 'Fixed')
            new_content = new_content.replace('mimobileide', 'mixed').replace('miMobileIDE', 'mixed')
            new_content = new_content.replace('Mimobileide', 'Mixed').replace('MiMobileIDE', 'Mixed')
            new_content = new_content.replace('prefimobileide', 'prefixed').replace('prefiMobileIDE', 'prefixed')
            new_content = new_content.replace('Prefimobileide', 'Prefixed').replace('PrefiMobileIDE', 'Prefixed')
            new_content = new_content.replace('suffimobileide', 'suffixed').replace('suffiMobileIDE', 'suffixed')
            new_content = new_content.replace('Suffimobileide', 'Suffixed').replace('SuffiMobileIDE', 'Suffixed')
            new_content = new_content.replace('bomobileide', 'boxed').replace('boMobileIDE', 'boxed')
            new_content = new_content.replace('Bomobileide', 'Boxed').replace('BoMobileIDE', 'Boxed')
            new_content = new_content.replace('unbomobileide', 'unboxed').replace('unboMobileIDE', 'unboxed')
            new_content = new_content.replace('Unbomobileide', 'Unboxed').replace('UnboMobileIDE', 'Unboxed')

            if new_content != content:
                with open(filepath, 'w') as file:
                    file.write(new_content)

if __name__ == '__main__':
    for d in ['/data/data/com.termux/files/home/MobileIDE/features', '/data/data/com.termux/files/home/MobileIDE/core']:
        if os.path.exists(d):
            fix_mistakes(d)
    print("Fixed mistakes.")
