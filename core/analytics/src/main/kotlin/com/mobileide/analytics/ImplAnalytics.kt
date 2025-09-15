/*
 * Copyright 2025 | Dmitri Chernysh | https://github.com/dmitriy-chernysh
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.mobiledevpro.analytics

import android.app.Activity
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics


/**
 *
 * Created on Feb 15, 2025.
 *
 */
class ImplAnalytics(
    private val provider: FirebaseAnalytics
) : Analytics {
    override fun trackAction(actionName: String) {
        TODO("Not yet implemented")
    }

    override fun trackScreen(screenName: String, activity: Activity?) {
        val activityName = activity?.let { it::class.simpleName } ?: "Unknown Activity"
        Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, activityName)
        }.also { bundle ->
            provider.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        }
    }
}