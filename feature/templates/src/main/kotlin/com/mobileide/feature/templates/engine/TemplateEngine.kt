package com.mobileide.templates.engine

import com.mobileide.templates.model.GeneratedFile
import com.mobileide.templates.model.Template
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A powerful and dynamic template engine.
 *
 * It can replace any variable defined in the template's files with
 * values from a provided map. This makes the engine extremely flexible.
 */
interface TemplateEngine {
    /**
     * Processes a template with the given parameters.
     *
     * @param template The template to process.
     * @param parameters A map of key-value pairs to replace in the template files.
     * @return A list of generated files with their final paths and content.
     */
    fun processTemplate(template: Template, parameters: Map<String, String>): List<GeneratedFile>
}

class TemplateEngineImpl : TemplateEngine {
    override fun processTemplate(template: Template, parameters: Map<String, String>): List<GeneratedFile> {
        // Add default dynamic values like the current date
        val extendedParams = parameters.toMutableMap()
        extendedParams.putIfAbsent("creationDate", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
        
        val packagePath = (parameters["packageName"] ?: "").replace('.', '/')
        extendedParams["packagePath"] = packagePath

        return template.files.map { templateFile ->
            val finalPath = replaceVariables(templateFile.path, extendedParams)
            val finalContent = replaceVariables(templateFile.content, extendedParams)
            GeneratedFile(path = finalPath, content = finalContent)
        }
    }

    private fun replaceVariables(input: String, parameters: Map<String, String>): String {
        var result = input
        parameters.forEach { (key, value) ->
            result = result.replace("\${$key}", value)
        }
        return result
    }
}

