package com.scto.mobile.ide.features.runner

import com.scto.mobile.ide.events.Event

sealed interface RunnerEvent : Event {

    data class RunnerRun(val runner: Runner) : RunnerEvent
}
