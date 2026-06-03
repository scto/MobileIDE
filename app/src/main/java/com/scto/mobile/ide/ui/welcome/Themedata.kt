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
 
package com.scto.mobile.ide.ui.welcome

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

// 1. Keep this new structure unchanged
data class ThemeColorSpec(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val accent: Color
)

// 2. Modify here: add "compatibility layer"
data class ThemeColor(
    val name: String,
    val dark: ThemeColorSpec,
    val light: ThemeColorSpec,
    val style: PaletteStyle = PaletteStyle.TonalSpot
) {
    val primaryColor: Color    get() = dark.primary
    // =========== ⬆️ Compatibility code ends ⬆️ ===========
}

// 3. Data list keeps the two-color configuration given to you before (just copy and overwrite)
val themeColors = listOf(
    ThemeColor(
        name = "Catppuccin",
        dark = ThemeColorSpec(Color(0xFF1E1E2E), Color(0xFF313244), Color(0xFFCBA6F7), Color(0xFFF5C2E7)),
        light = ThemeColorSpec(Color(0xFFEFF1F5), Color(0xFFE6E9EF), Color(0xFF8839EF), Color(0xFFEA76CB)),
        style = PaletteStyle.TonalSpot
    ),
    ThemeColor(
        name = "Apple",
        dark = ThemeColorSpec(Color(0xFF000000), Color(0xFF1C1C1E), Color(0xFF32D74B), Color(0xFF0A84FF)),
        light = ThemeColorSpec(Color(0xFFF2F2F7), Color(0xFFFFFFFF), Color(0xFF34C759), Color(0xFF007AFF)),
        style = PaletteStyle.Fidelity
    ),
    ThemeColor(
        name = "Lavender",
        dark = ThemeColorSpec(Color(0xFF1A1626), Color(0xFF49454F), Color(0xFFB8ADFF), Color(0xFFCBC0FF)),
        light = ThemeColorSpec(Color(0xFFFFFBFF), Color(0xFFE6E0EC), Color(0xFF6750A4), Color(0xFFEADDFF)),
        style = PaletteStyle.TonalSpot
    ),
    ThemeColor(
        name = "Midnight",
        dark = ThemeColorSpec(Color(0xFF0D1117), Color(0xFF161B22), Color(0xFF58A6FF), Color(0xFF79C0FF)),
        light = ThemeColorSpec(Color(0xFFFFFFFF), Color(0xFFF6F8FA), Color(0xFF0969DA), Color(0xFF54AEFF)),
        style = PaletteStyle.TonalSpot
    ),
    ThemeColor(
        name = "Nord",
        dark = ThemeColorSpec(Color(0xFF2E3440), Color(0xFF3B4252), Color(0xFF88C0D0), Color(0xFF81A1C1)),
        light = ThemeColorSpec(Color(0xFFECEFF4), Color(0xFFE5E9F0), Color(0xFF5E81AC), Color(0xFF88C0D0)),
        style = PaletteStyle.TonalSpot
    ),
    ThemeColor(
        name = "Strawberry",
        dark = ThemeColorSpec(Color(0xFF201418), Color(0xFF524347), Color(0xFFFF8FB4), Color(0xFFFFB1C8)),
        light = ThemeColorSpec(Color(0xFFFFF0F5), Color(0xFFFFE6ED), Color(0xFFD63384), Color(0xFFFF87AB)),
        style = PaletteStyle.Vibrant
    ),
    ThemeColor(
        name = "Tako",
        dark = ThemeColorSpec(Color(0xFF1A1625), Color(0xFF2D2640), Color(0xFF9D7CD8), Color(0xFFB79FE8)),
        light = ThemeColorSpec(Color(0xFFF3E8FF), Color(0xFFE9D5FF), Color(0xFF7E22CE), Color(0xFFA855F7)),
        style = PaletteStyle.Vibrant
    )
)