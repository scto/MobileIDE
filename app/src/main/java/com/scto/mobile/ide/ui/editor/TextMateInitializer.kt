/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
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

package com.scto.mobile.ide.ui.editor

import android.content.Context
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.tm4e.core.registry.IThemeSource
import com.scto.mobile.ide.core.utils.LogCatcher

object TextMateInitializer {
    @Volatile private var isInitialized = false
    private val mutex = Mutex()
    private val callbacks = mutableListOf<() -> Unit>()

    const val THEME_LIGHT = "quietlight"
    const val THEME_DARK = "darcula"

    fun isReady() = isInitialized

    @OptIn(DelicateCoroutinesApi::class)
    fun initialize(context: Context, onComplete: (() -> Unit)? = null) {
        if (isInitialized) {
            onComplete?.invoke()
            return
        }

        synchronized(callbacks) { if (onComplete != null) callbacks.add(onComplete) }

        GlobalScope.launch(Dispatchers.IO) {
            mutex.withLock {
                if (isInitialized) {
                    notifyCallbacks()
                    return@withLock
                }

                LogCatcher.i("TextMate", "Starting TextMate initialization...")
                try {
                    val appContext = context.applicationContext

                    // Always re-register the file provider (handles fresh process)
                    try {
                        FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(appContext.assets))
                        LogCatcher.i("TextMate", "AssetsFileResolver registered")
                    } catch (e: Exception) {
                        LogCatcher.w("TextMate", "AssetsFileResolver registration note: ${e.message}")
                    }

                    val themeRegistry = ThemeRegistry.getInstance()
                    val themes =
                        mapOf(THEME_LIGHT to "textmate/$THEME_LIGHT.json", THEME_DARK to "textmate/$THEME_DARK.json")

                    themes.forEach { (name, path) ->
                        try {
                            val stream = FileProviderRegistry.getInstance().tryGetInputStream(path)
                            if (stream == null) {
                                LogCatcher.e("TextMate", "Theme file not found: $path")
                            } else {
                                stream.use { inputStream ->
                                    themeRegistry.loadTheme(
                                        ThemeModel(IThemeSource.fromInputStream(inputStream, path, null), name)
                                    )
                                    LogCatcher.i("TextMate", "Theme loaded successfully: $name")
                                }
                            }
                        } catch (e: Exception) {
                            LogCatcher.e("TextMate", "Failed to load theme: $name", e)
                            e.printStackTrace()
                        }
                    }

                    // Load grammars
                    try {
                        LogCatcher.i("TextMate", "Loading grammars from textmate/languages.json...")
                        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
                        LogCatcher.i("TextMate", "Grammars loaded successfully.")
                    } catch (e: Exception) {
                        LogCatcher.e("TextMate", "Failed to load grammars from languages.json", e)
                        e.printStackTrace()
                    }

                    isInitialized = true
                    LogCatcher.i("TextMate", "TextMate initialization completed.")
                    notifyCallbacks()
                } catch (e: Exception) {
                    LogCatcher.e("TextMate", "Initialization crash", e)
                    e.printStackTrace()
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun notifyCallbacks() {
        synchronized(callbacks) {
            val iter = callbacks.iterator()
            while (iter.hasNext()) {
                val cb = iter.next()
                try {
                    GlobalScope.launch(Dispatchers.Main) { cb() }
                } catch (_: Exception) {}
                iter.remove()
            }
        }
    }
}
