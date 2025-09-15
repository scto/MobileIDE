package com.mobileide.xmltocompose.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileide.common.Resource
import com.mobileide.xmltocompose.data.XmlConverterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class XmlToComposeViewModel @Inject constructor(
    private val converterRepository: XmlConverterRepository
) : ViewModel() {

    private val _state = MutableStateFlow(XmlToComposeState())
    val state = _state.asStateFlow()

    fun onEvent(event: XmlToComposeEvent) {
        when (event) {
            is XmlToComposeEvent.XmlInputChanged -> {
                _state.update { it.copy(xmlInput = event.xml, error = null) }
            }
            XmlToComposeEvent.ConvertClicked -> {
                convert()
            }
        }
    }

    private fun convert() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, composeOutput = "") }

            when (val result = converterRepository.convertXmlToCompose(_state.value.xmlInput)) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false, composeOutput = result.data ?: "") }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {
                    // Nicht relevant
                }
            }
        }
    }
}
