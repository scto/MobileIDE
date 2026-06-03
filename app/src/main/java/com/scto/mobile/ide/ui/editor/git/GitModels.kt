




/*
 * WebIDE - A powerful IDE for Android web development.
 * Copyright (C) 2025  如日中天  <3382198490@qq.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
/*
 * GitModels.kt
 */
package com.scto.mobile.ide.ui.editor.git

import androidx.compose.ui.graphics.Color

// --- 以下保持原有的 Git 模型不变 ---

data class GitCommitUI(
    val hash: String,
    val shortHash: String,
    val message: String,
    val fullMessage: String,
    val author: String,
    val email: String,
    val time: Long,
    val parents: List<String>,
    val refs: List<GitRefUI>,
    val lane: Int,
    val totalLanes: Int,
    val childLanes: List<Int>,
    val parentLanes: List<Int>,
    val color: Color
)

data class GitRefUI(val name: String, val type: RefType)
enum class RefType { HEAD, LOCAL_BRANCH, REMOTE_BRANCH, TAG }

data class GitBranch(
    val name: String,
    val fullRef: String,
    val type: BranchType,
    val isCurrent: Boolean
)

enum class BranchType { LOCAL, REMOTE }

data class GitFileChange(val filePath: String, val status: GitFileStatus)

data class GitAuth(
    val type: AuthType = AuthType.HTTPS,
    val username: String = "",
    val token: String = "",
    val privateKey: String = "",
    val passphrase: String = ""
)

enum class AuthType { HTTPS, SSH }

enum class GitFileStatus { ADDED, MODIFIED, UNTRACKED, MISSING, REMOVED, CONFLICTING }