package com.scto.mobile.ide.features.proot

import android.content.Context
import java.io.File

interface ProotSandbox {
    fun buildProotCommand(context: Context, command: Array<String>): List<String>
    fun getProotEnv(context: Context): Map<String, String>
}

object ProotSandboxImpl : ProotSandbox {

    private fun getPrefixDir(context: Context): File = context.filesDir.parentFile!!
    private fun getLocalDir(context: Context): File = File(getPrefixDir(context), "local").apply { mkdirs() }
    private fun getBinDir(context: Context): File = File(getLocalDir(context), "bin").apply { mkdirs() }

    override fun buildProotCommand(context: Context, command: Array<String>): List<String> {
        val prefixDir = getPrefixDir(context)
        val distroDir = File(prefixDir, "local/sandbox")
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val libProot = File(nativeLibDir, "libproot.so")
        val prootExec = if (libProot.exists()) libProot.absolutePath else File(getBinDir(context), "proot").absolutePath

        val args = mutableListOf<String>()
        args.add(prootExec)
        args.add("--kill-on-exit")
        args.add("--link2symlink")
        args.add("--sysvipc")
        args.add("-L")
        args.add("-0")

        val mounts = listOf("/proc", "/sys", "/dev", "/data", "/storage", "/system")
        mounts.forEach {
            if (File(it).exists()) {
                args.add("-b")
                args.add(it)
            }
        }

        val tmpDir = File(distroDir, "tmp").apply { mkdirs() }
        args.add("-b")
        args.add("${tmpDir.absolutePath}:/dev/shm")

        val rootHome = File(distroDir, "root")
        if (!rootHome.exists()) {
            rootHome.mkdirs()
        }
        args.add("-b")
        args.add("${rootHome.absolutePath}:/root")
        args.add("-b")
        args.add(context.filesDir.absolutePath)
        args.add("-r")
        args.add(distroDir.absolutePath)
        args.add("-w")
        args.add("/root")

        args.add("/usr/bin/env")
        args.add("-i")
        args.add("HOME=/root")
        args.add("PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
        args.add("LANG=C.UTF-8")
        args.add("TERM=xterm-256color")
        args.add("TMPDIR=/tmp")

        args.addAll(command)
        return args
    }

    override fun getProotEnv(context: Context): Map<String, String> {
        val env = mutableMapOf<String, String>()
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val prootTmpDir = File(context.filesDir, "usr/tmp").apply { mkdirs() }
        prootTmpDir.setReadable(true, false)
        prootTmpDir.setWritable(true, false)
        prootTmpDir.setExecutable(true, false)

        env["PROOT_TMP_DIR"] = prootTmpDir.absolutePath
        env["TMPDIR"] = prootTmpDir.absolutePath
        env["TMP_DIR"] = prootTmpDir.absolutePath
        val libPath = "${context.filesDir.absolutePath}:${context.filesDir.absolutePath}/local/lib:$nativeLibDir"
        env["LD_LIBRARY_PATH"] = libPath

        val loader64 = listOf(File(nativeLibDir, "libproot-loader.so"), File(nativeLibDir, "libloader.so")).firstOrNull { it.exists() }
        val loader32 = listOf(File(nativeLibDir, "libproot-loader32.so"), File(nativeLibDir, "libloader32.so")).firstOrNull { it.exists() }

        if (loader64 != null) {
            loader64.setExecutable(true, false)
            env["PROOT_LOADER"] = loader64.absolutePath
        }
        if (loader32 != null) {
            loader32.setExecutable(true, false)
            env["PROOT_LOADER32"] = loader32.absolutePath
            env["PROOT_LOADER_32"] = loader32.absolutePath
        }
        return env
    }
}
