package com.scto.mobile.ide





import androidx.lifecycle.lifecycleScope
import com.scto.mobile.ide.activities.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope











// same as MainActivity.lifeCycleScope
@OptIn(DelicateCoroutinesApi::class)
val DefaultScope: CoroutineScope
    get() {
        return XedHost?.lifecycleScope ?: GlobalScope
    }
