package com.scto.mobile.ide

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val DefaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
