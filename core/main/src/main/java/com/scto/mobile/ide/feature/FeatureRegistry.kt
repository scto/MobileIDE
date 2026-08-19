package com.scto.mobile.ide.feature





import android.app.Activity
import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.settings.Preference
import com.scto.mobile.ide.utils.application
import com.scto.mobile.ide.utils.dialogRes
import com.scto.mobile.ide.core.main.BuildConfig











interface Feature {
    fun init(application: Application)

    fun dispose(application: Application) {}

    val toggle: FeatureToggle?
        get() = null
}

data class FeatureToggle(
    val name: String,
    val key: String,
    val default: Boolean,
    val icon: Icon,
    val onSwitch: ((Activity, Boolean, onComplete: (Boolean) -> Unit) -> Unit)? = null,
) {
    val state: MutableState<Boolean> by lazy {
        mutableStateOf(Preference.getBoolean(key, default))
    }

    fun setEnable(enable: Boolean) {
        Preference.setBoolean(key, enable)
        state.value = enable
        FeatureRegistry.onToggleChange(key, enable)
    }
}

object FeatureRegistry {
    private val features = mutableMapOf<String, Feature>()
    private val featuresWithoutToggles = mutableListOf<Feature>()
    val toggles = mutableStateListOf<FeatureToggle>()

    init {
        registerToggle(
            FeatureToggle(
                name = com.scto.mobile.ide.core.main.R.string.debug_options.getString(),
                key = "debug_mode",
                default = BuildConfig.DEBUG,
                icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.build),
                onSwitch = { activity, checked, onComplete ->
                    if (checked) {
                        dialogRes(
                            activity = activity,
                            title = com.scto.mobile.ide.core.main.R.string.attention.getString(),
                            msg = com.scto.mobile.ide.core.main.R.string.debug_mode_warn.getString(),
                            onCancel = { onComplete(false) },
                            onOk = { onComplete(true) },
                        )
                    } else {
                        onComplete(false)
                    }
                },
            )
        )
    }

    fun register(feature: Feature) {
        val toggle = feature.toggle
        if (toggle != null) {
            features[toggle.key] = feature
            registerToggle(toggle)
        } else {
            featuresWithoutToggles.add(feature)
        }
    }

    fun initFeatures(application: Application) {
        featuresWithoutToggles.forEach { it.init(application) }
        features.forEach { (key, feature) ->
            if (isEnabled(key)) {
                feature.init(application)
            }
        }
    }

    fun onToggleChange(key: String, enabled: Boolean) {
        val application = application ?: return
        val feature = features[key] ?: return
        if (enabled) {
            feature.init(application)
        } else {
            feature.dispose(application)
        }
    }

    fun registerToggle(toggle: FeatureToggle) {
        if (toggles.any { it.key == toggle.key }) return
        toggles.add(toggle)
    }

    fun isEnabled(key: String): Boolean {
        return toggles.find { it.key == key }?.state?.value ?: false
    }
}
