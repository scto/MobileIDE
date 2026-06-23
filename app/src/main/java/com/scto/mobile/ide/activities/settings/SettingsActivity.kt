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

package com.scto.mobile.ide.activities..settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

import com.rk.file.FileManager
import com.rk.resources.strings
import com.rk.theme.XedTheme
import com.rk.utils.toast

import java.lang.ref.WeakReference

var settingsNavController = WeakReference<NavController?>(null)
var snackbarHostStateRef: WeakReference<SnackbarHostState?> = WeakReference(null)

class SettingsActivity : AppCompatActivity() {
    val fileManager = FileManager(this)

    companion object {
        private var activityRef = WeakReference<SettingsActivity?>(null)
        var instance: SettingsActivity?
            get() = activityRef.get()
            private set(value) {
                activityRef = WeakReference(value)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        enableEdgeToEdge()
        setContent {
            XedTheme {
                Surface {
                    val navController = rememberNavController()
                    settingsNavController = WeakReference(navController)
                    SettingsNavHost(activity = this@SettingsActivity, navController = navController)

                    val snackbarHostState = remember { SnackbarHostState() }
                    LaunchedEffect(snackbarHostState) { snackbarHostStateRef = WeakReference(snackbarHostState) }

                    LaunchedEffect(intent) {
                        if (intent.hasExtra("route")) {
                            val route = intent.getStringExtra("route")
                            if (route != null) {
                                navController.navigate(route)
                            } else {
                                toast(strings.unknown_err)
                            }
                        }
                    }
                }
            }
        }
    }
}
