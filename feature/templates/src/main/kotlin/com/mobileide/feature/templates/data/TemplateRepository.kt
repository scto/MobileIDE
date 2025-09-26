package com.mobileide.templates.data

import com.mobileide.templates.model.Template
import com.mobileide.templates.data.previews.getBasicActivityTemplate
import com.mobileide.templates.data.previews.getEmptyComposeActivityTemplate
import javax.inject.Inject
import javax.inject.Singleton

interface TemplateRepository {
    fun getTemplates(): List<Template>
}

@Singleton
class TemplateRepositoryImpl @Inject constructor() : TemplateRepository {

    // In einer echten App würden diese Vorlagen wahrscheinlich aus Assets oder einer
    // Remote-Quelle geladen.
    private val templates = listOf(
        getEmptyComposeActivityTemplate(),
        getBasicActivityTemplate()
        // Hier können weitere Vorlagen hinzugefügt werden
    )

    override fun getTemplates(): List<Template> {
        return templates
    }
}
