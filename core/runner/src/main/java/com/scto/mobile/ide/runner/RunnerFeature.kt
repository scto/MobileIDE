package com.scto.mobile.ide.runner

import android.app.Application
import com.scto.mobile.ide.components.DialogRegistry
import com.scto.mobile.ide.feature.Feature

class RunnerFeature : Feature {
    override fun init(application: Application) {
        // Register RunnerSheet overlay
        DialogRegistry.dialogs.add {
            if (RunnerUI.showRunnerDialog) {
                RunnerSheet()
            }
        }
    }
}
