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
 
package com.scto.mobile.ide.ui.terminal

import android.content.Context
import android.graphics.Typeface

object TerminalFontManager {
    // Default font path
    private const val DEFAULT_FONT_PATH = "ttf/JetBrainsMono-Regular.ttf"

    // Cache Typeface to avoid repeated loading
    private var cachedTypeface: Typeface? = null

    /**
     * Get global terminal font
     */
    fun getTypeface(context: Context): Typeface {
        // If already loaded, return cache directly
        if (cachedTypeface != null) {
            return cachedTypeface!!
        }

        // Attempt to load font
        return try {
            val font = Typeface.createFromAsset(context.assets, DEFAULT_FONT_PATH)
            cachedTypeface = font
            font
        } catch (e: Exception) {
            e.printStackTrace()
            // If loading fails (file does not exist), fallback to system monospace font
            Typeface.MONOSPACE
        }
    }

}