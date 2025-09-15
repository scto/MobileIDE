package com.mobileide.git.presentation

import com.mobileide.common.GitStatusResult

data class GitState(
    val repoUrl: String = "",
    val localPath: String = "/storage/emulated/0/MobileIDEProjects/DefaultRepo",
    val isLoading: Boolean = false,
    val statusResult: GitStatusResult = GitStatusResult(),
    val commitMessage: String = "",
    val userMessage: String? = null, // Für Snackbar-Nachrichten
    val error: String? = null
)

sealed interface GitEvent {
    data class UrlChanged(val url: String) : GitEvent
    data class LocalPathChanged(val path: String) : GitEvent
    data class CommitMessageChanged(val message: String) : GitEvent
    data class StageFile(val file: String) : GitEvent
    data class UnstageFile(val file: String) : GitEvent
    object CloneClicked : GitEvent
    object StatusClicked : GitEvent
    object CommitClicked : GitEvent
    object PushClicked : GitEvent
    object PullClicked : GitEvent
    object UserMessageShown : GitEvent
}