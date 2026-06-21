/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  Thomas Schmid  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.scto.mobile.ide.ui.terminal

object TerminalConfig {

    const val VIRTUAL_KEYS_JSON =
        """
[
  [
    "ESC",
    {
      "key": "/",
      "popup": "\\"
    },
    {
      "key": "-",
      "popup": "|"
    },
    "HOME",
    "UP",
    "END",
    "PGUP"
  ],
  [
    "TAB",
    "CTRL",
    "ALT",
    "LEFT",
    "DOWN",
    "RIGHT",
    "PGDN"
  ]
]
"""

    fun getBackgroundColor(isDark: Boolean): Int {
        return if (isDark) {
            0xFF000000.toInt()
        } else {
            0xFFFFFFFF.toInt()
        }
    }
}
