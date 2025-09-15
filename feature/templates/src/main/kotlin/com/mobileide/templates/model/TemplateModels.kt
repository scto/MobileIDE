package com.mobileide.templates.model

/**
 * Represents a project template.
 */
data class Template(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val files: List<TemplateFile>
)

/**
 * Represents a single file within a template.
 * Its path and content can contain variables like ${variableName}.
 */
data class TemplateFile(
    val path: String,
    val content: String
)

/**
 * Represents a file that has been processed by the TemplateEngine.
 */
data class GeneratedFile(
    val path: String,
    val content: String
)
