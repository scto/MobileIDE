package com.mobileide.lsp

import com.mobileide.lsp.model.LspNotification
import kotlinx.coroutines.flow.Flow

interface LspClient {
    val incomingNotifications: Flow<LspNotification>
    fun sendNotification(method: String, params: Any)
    suspend fun <T> sendRequest(method: String, params: Any): T
    fun shutdown()
}
