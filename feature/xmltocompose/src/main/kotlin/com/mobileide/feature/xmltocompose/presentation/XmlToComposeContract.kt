package com.mobileide.xmltocompose.presentation

data class XmlToComposeState(
    val xmlInput: String = "",
    val composeOutput: String = "",
    val error: String? = null,
    val isLoading: Boolean = false
)

sealed interface XmlToComposeEvent {
    data class XmlInputChanged(val xml: String) : XmlToComposeEvent
    object ConvertClicked : XmlToComposeEvent
}
