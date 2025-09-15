package com.mobileide.common

/**
 * Repräsentiert ein Element im Datei-Explorer.
 * Diese Datenklasse wird von verschiedenen Features verwendet, um auf Dateien und Ordner zu verweisen.
 */
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val lastModified: Long
)

/**
 * Hält das Ergebnis eines `git status`-Befehls.
 * Diese Klasse wird vom Git-Feature verwendet und könnte in Zukunft auch vom Editor
 * genutzt werden, um den Status von Dateien anzuzeigen.
 *
 * NEU: `conflicting` wurde hinzugefügt, um Merge-Konflikte abzubilden.
 */
data class GitStatusResult(
    val branch: String = "",
    val modified: Set<String> = emptySet(),
    val untracked: Set<String> = emptySet(),
    val added: Set<String> = emptySet(),
    val changed: Set<String> = emptySet(),
    val removed: Set<String> = emptySet(),
    val conflicting: Set<String> = emptySet() // Hinzugefügt für Merge-Konflikte
)
