package com.mobileide.svgtoavd.presentation

data class SvgToAvdState(
    val svgInput: String = "",
    val avdOutput: String = "",
    val error: String? = null,
    val isLoading: Boolean = false
)

sealed interface SvgToAvdEvent {
    data class SvgInputChanged(val svg: String) : SvgToAvdEvent
    object ConvertClicked : SvgToAvdEvent
}
