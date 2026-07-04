package com.scto.mobile.ide.core.tooling.api

/**
 * A generic sealed class representing the state of an async resource.
 */
sealed class Resource<out T> {
    data object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
}
