package com.mobileide.app

/**
 * Central constants for MobileIDE.
 * All URLs, version strings and global keys live here.
 */
object AppConstants {

    // ── Identity ───────────────────────────────────────────────────────────────
    const val APP_NAME         = "MobileIDE"
    const val APP_VERSION      = "0.0.1"
    const val APP_PACKAGE      = "com.mobileide.app"

    // ── Documentation & Links ─────────────────────────────────────────────────
    /** Main GitHub repository */
    const val GITHUB_REPO_URL  = "https://github.com/scto/MobileIDE"
    /** Wiki / documentation root */
    const val DOCS_URL         = "https://github.com/scto/MobileIDE/wiki"
    /** Quick-start guide */
    const val QUICKSTART_URL   = "https://github.com/scto/MobileIDE/wiki/Quick-Start"
    /** Releases / changelog */
    const val CHANGELOG_URL    = "https://github.com/scto/MobileIDE/releases"
    /** Issue tracker */
    const val ISSUES_URL       = "https://github.com/scto/MobileIDE/issues"
    /** Termux on F-Droid */
    const val TERMUX_FDROID_URL = "https://f-droid.org/packages/com.termux/"
    /** Sora Editor repository */
    const val SORA_EDITOR_URL  = "https://github.com/Rosemoe/sora-editor"

    // ── Storage ────────────────────────────────────────────────────────────────
    /** Root folder for all projects on external storage */
    const val PROJECTS_DIR_NAME = "MobileIDEProjects"
    /** Root folder for IDE configuration */
    const val CONFIG_DIR_NAME   = "MobileIDEProjects/.config"

    // ── DataStore ─────────────────────────────────────────────────────────────
    const val WORKSPACE_STORE_NAME = "workspace_v2"

    // ── Termux ────────────────────────────────────────────────────────────────
    const val TERMUX_PACKAGE   = "com.termux"
    const val TERMUX_HOME      = "/data/data/com.termux/files/home"
    const val TERMUX_PREFIX    = "/data/data/com.termux/files/usr"
}
