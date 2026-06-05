/*
 * WebIDE - A powerful IDE for Android web development.
 * Copyright (C) 2025  如日中天  <3382198490@qq.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scto.mobile.ide.lsp

import android.content.Context
import android.util.Log
import com.scto.mobile.ide.ui.terminal.AlpineManager
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import java.io.FilterInputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread

class ProotStreamConnectionProvider(private val context: Context, private val command: List<String>) :
    StreamConnectionProvider {

    private var process: Process? = null

    override fun start() {
        val fullCommand = AlpineManager.buildProotCommand(context, command.toTypedArray())
        val hostEnv = AlpineManager.getProotEnv(context)

        val pb = ProcessBuilder(fullCommand)
        pb.environment().putAll(hostEnv)
        pb.directory(context.filesDir)

        Log.d("LSP-Process", "启动命令: $fullCommand")

        try {
            process = pb.start()
            Log.d("LSP-Process", "进程启动成功 PID: ${process.toString()}")

            // === 修复崩溃的关键点：监控错误流时增加 try-catch ===
            thread {
                try {
                    process?.errorStream?.bufferedReader()?.forEachLine { Log.e("LSP-Stderr", it) }
                } catch (_: IOException) {
                    // 正常现象：当 destroy() 被调用时，流被关闭会抛出此异常，忽略即可
                } catch (e: Exception) {
                    Log.e("LSP-Stderr", "读取错误流失败", e)
                }
            }
            // ===========================================

        } catch (e: Exception) {
            Log.e("LSP-Process", "启动失败", e)
            throw e
        }
    }

    // === 修复编译错误：使用 val 加 get()，而不是 fun ===

    // 包装输入流，打印 LSP 回复给 Editor 的内容 (LSP -> App)
    override val inputStream: InputStream
        get() =
            object : FilterInputStream(process?.inputStream) {
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    try {
                        val read = super.read(b, off, len)
                        if (read > 0) {
                            // 这里的日志用于调试，内容太多可以注释掉
                            Log.v("LSP-RX", "收到 $read 字节数据")
                        }
                        return read
                    } catch (e: IOException) {
                        throw e
                    }
                }
            }

    // 包装输出流，打印 Editor 发送给 LSP 的内容 (App -> LSP)
    override val outputStream: OutputStream
        get() =
            object : FilterOutputStream(process?.outputStream) {
                override fun write(b: ByteArray, off: Int, len: Int) {
                    try {
                        val content = String(b, off, len)
                        val logContent = if (content.length > 200) content.substring(0, 200) + "..." else content
                        Log.d("LSP-TX", "发送: $logContent")

                        super.write(b, off, len)
                    } catch (e: IOException) {
                        throw e
                    }
                }
            }

    override fun close() {
        Log.d("LSP-Process", "正在关闭 LSP 进程...")
        process?.destroy()
        process = null
    }
}
