package com.scto.mobile.ide.features.git

import com.scto.mobile.ide.events.Event
import com.scto.mobile.ide.file.FileObject

/** Events related to Git version control operations. */
sealed interface GitEvent : Event {
    // TODO: Implement repo init, branch deletion, stash management, rebase, merge, conflicts
    // data class RepositoryInitialized(val path: String) : GitEvent

    /** Event triggered when a Git repository has been successfully cloned. */
    data class RepositoryCloned(
        val remoteUrl: String,
        val branch: String,
        val destination: FileObject,
    ) : GitEvent

    /** Event triggered when a new Git branch has been created. */
    data class BranchCreated(
        val root: FileObject,
        val name: String,
        val fromBranch: String?,
    ) : GitEvent

    // data class BranchDeleted(val name: String) : GitEvent

    /** Event triggered when a Git branch has been checked out. */
    data class BranchCheckedOut(val root: FileObject, val name: String) : GitEvent

    /**
     * Event triggered when a new Git commit has been created.
     *
     * @see CommitAmended
     */
    data class CommitCreated(
        val root: FileObject,
        val message: String,
    ) : GitEvent

    /**
     * Event triggered when a Git commit has been amended.
     *
     * @see CommitCreated
     */
    data class CommitAmended(
        val root: FileObject,
        val message: String,
    ) : GitEvent

    /** Event triggered when a Git fetch operation has completed. */
    data class FetchCompleted(val root: FileObject, val remote: String, val branch: String) : GitEvent

    /** Event triggered when a Git pull operation has completed. */
    data class PullCompleted(
        val root: FileObject,
        val remote: String,
        val branch: String,
    ) : GitEvent

    /** Event triggered when a Git push operation has completed. */
    data class PushCompleted(
        val root: FileObject,
        val remote: String,
        val branch: String,
        val force: Boolean = false,
    ) : GitEvent

    /** Event triggered when the Git working tree has been updated with changes. */
    data class WorkingTreeUpdated(val root: FileObject, val changes: List<GitChange>) : GitEvent
}
