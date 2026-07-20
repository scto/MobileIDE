import os
import glob
import subprocess
import shutil

os.environ["JAVA_HOME"] = "/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk"
os.environ["PATH"] = "/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk/bin:" + os.environ.get("PATH", "")

if not os.path.exists("assets"):
    os.makedirs("assets")

for d in glob.glob("plugins/*-lsp"):
    if os.path.isdir(d):
        compile_script = os.path.join(d, "compileRelease")
        if os.path.exists(compile_script):
            print(f"Building {d} with Java 21")
            subprocess.run(["sh", "compileRelease"], cwd=d)
            
            output_dir = os.path.join(d, "output")
            if os.path.exists(output_dir):
                for f in os.listdir(output_dir):
                    if f.endswith(".zip"):
                        src = os.path.join(output_dir, f)
                        dst = os.path.join("assets", f)
                        shutil.copy2(src, dst)
                    elif f.endswith(".tinaplug"):
                        src = os.path.join(output_dir, f)
                        dst = os.path.join("assets", f.replace(".tinaplug", ".zip"))
                        shutil.copy2(src, dst)
