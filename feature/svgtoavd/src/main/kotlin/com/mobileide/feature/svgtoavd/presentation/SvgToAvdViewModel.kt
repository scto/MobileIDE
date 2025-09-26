package com.mobileide.svgtoavd.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.mobileide.common.Resource
import com.mobileide.svgtoavd.data.SvgConverterRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SvgToAvdViewModel @Inject constructor(
    private val converterRepository: SvgConverterRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SvgToAvdState())
    val state = _state.asStateFlow()

    fun onEvent(event: SvgToAvdEvent) {
        when (event) {
            is SvgToAvdEvent.SvgInputChanged -> {
                _state.update { it.copy(svgInput = event.svg, error = null) }
            }
            SvgToAvdEvent.ConvertClicked -> {
                convert()
            }
        }
    }

    private fun convert() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, avdOutput = "") }

            when (val result = converterRepository.convertSvgToAvd(_state.value.svgInput)) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false, avdOutput = result.data ?: "") }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {
                    // Nicht relevant in diesem Fall, da wir einen einzelnen Aufruf haben
                }
            }
        }
    }
}
