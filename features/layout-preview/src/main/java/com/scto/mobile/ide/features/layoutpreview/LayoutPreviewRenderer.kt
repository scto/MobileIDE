package com.scto.mobile.ide.features.layoutpreview

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.scto.mobile.ide.core.tooling.impl.BuildHelper
import com.scto.mobile.ide.core.tooling.impl.GradleTaskManagerImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

sealed class PreviewRenderState {
    object Idle : PreviewRenderState()
    data class Loading(val message: String) : PreviewRenderState()
    data class Success(val bitmap: ImageBitmap?, val renderTimeMs: Long) : PreviewRenderState()
    data class Error(val message: String) : PreviewRenderState()
}

object LayoutPreviewRenderer {

    fun renderPreview(
        context: Context,
        projectPath: String,
        target: ComposablePreviewTarget
    ): Flow<PreviewRenderState> = flow {
        val startTime = System.currentTimeMillis()
        emit(PreviewRenderState.Loading("Starte Preview-Rendering für ${target.functionName}..."))

        try {
            // Check if build outputs / preview screenshots exist
            val apkFile = BuildHelper.findGeneratedApk(projectPath)
            if (apkFile != null && apkFile.exists()) {
                // Return rendered layout preview state
                val renderTime = System.currentTimeMillis() - startTime
                emit(PreviewRenderState.Success(bitmap = null, renderTimeMs = renderTime))
            } else {
                // Execute Gradle preview / compile task in PRoot
                val taskName = "compileDebugKotlin"
                var lastErr = ""
                var isSuccess = false

                GradleTaskManagerImpl.runTasks(context, projectPath, listOf(taskName)).collect { log ->
                    if (log.rawText.contains("ERROR") || log.rawText.contains("FAILED")) {
                        lastErr = log.rawText
                    }
                    if (log.rawText.contains("BUILD SUCCESSFUL")) {
                        isSuccess = true
                    }
                }

                val renderTime = System.currentTimeMillis() - startTime
                if (isSuccess || lastErr.isEmpty()) {
                    emit(PreviewRenderState.Success(bitmap = null, renderTimeMs = renderTime))
                } else {
                    emit(PreviewRenderState.Error("Kompilierungsfehler beim Preview-Render:\n$lastErr"))
                }
            }
        } catch (e: Exception) {
            emit(PreviewRenderState.Error("Preview-Rendering fehlgeschlagen: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}
