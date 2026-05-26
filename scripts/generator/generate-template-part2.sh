#!/usr/bin/env bash
set -e

ROOT="core-template-part2"

rm -rf "$ROOT"
mkdir -p "$ROOT"

create_file() {
  local path="$1"
  mkdir -p "$(dirname "$path")"
  cat > "$path"
}

############################################
# DATA MODULE
############################################

DATA_BASE="$ROOT/core/template/data/src/main/kotlin/com/mcside/core/template/data"

create_file "$DATA_BASE/repository/TemplateVersionRepository.kt" <<'EOF'
package com.mcside.core.template.data.repository

/**
 * Repository für verfügbare Template-Versionen.
 */
interface TemplateVersionRepository {

    /**
     * Liefert die aktuell bekannte Version.
     */
    suspend fun getLatestVersion(
        templateId: String
    ): String

    /**
     * Prüft ob Version verfügbar ist.
     */
    suspend fun exists(
        templateId: String,
        version: String
    ): Boolean
}
EOF


create_file "$DATA_BASE/repository/InMemoryTemplateVersionRepository.kt" <<'EOF'
package com.mcside.core.template.data.repository

/**
 * Basisimplementierung für Template-Metadaten.
 */
class InMemoryTemplateVersionRepository(
    private val versions: Map<String, String>
) : TemplateVersionRepository {

    override suspend fun getLatestVersion(
        templateId: String
    ): String {
        return versions[templateId]
            ?: error("Template nicht gefunden")
    }

    override suspend fun exists(
        templateId: String,
        version: String
    ): Boolean {
        return versions[templateId] == version
    }
}
EOF


############################################
# IMPL MODULE
############################################

IMPL_BASE="$ROOT/core/template/impl/src/main/kotlin/com/mcside/core/template/impl"

############################################
# ZipExtractor
############################################

create_file "$IMPL_BASE/extractor/ZipExtractor.kt" <<'EOF'
package com.mcside.core.template.impl.extractor

import com.mcside.core.template.api.exception.TemplateExtractionException
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Entpackt ZIP-Dateien mit Schutz gegen Zip-Slip.
 */
class ZipExtractor {

    /**
     * Entpackt ein Archiv sicher.
     */
    fun extract(
        zipFile: File,
        targetDir: File
    ) {

        targetDir.mkdirs()

        val targetCanonical = targetDir.canonicalPath

        try {

            ZipInputStream(
                zipFile.inputStream()
            ).use { zip ->

                var entry = zip.nextEntry

                while (entry != null) {

                    val outputFile = File(
                        targetDir,
                        entry.name
                    )

                    val outputCanonical =
                        outputFile.canonicalPath

                    if (
                        !outputCanonical.startsWith(
                            targetCanonical + File.separator
                        )
                    ) {
                        throw TemplateExtractionException(
                            "Zip-Slip erkannt: ${entry.name}"
                        )
                    }

                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {

                        outputFile.parentFile?.mkdirs()

                        FileOutputStream(
                            outputFile
                        ).use { out ->

                            zip.copyTo(out)
                        }
                    }

                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

        } catch (e: Exception) {

            throw TemplateExtractionException(
                "Entpacken fehlgeschlagen",
                e
            )
        }
    }
}
EOF


############################################
# ProjectGenerator
############################################

create_file "$IMPL_BASE/generator/ProjectGenerator.kt" <<'EOF'
package com.mcside.core.template.impl.generator

import java.io.File

/**
 * Generiert Build-Dateien für neue Projekte.
 */
class ProjectGenerator {

    /**
     * Erzeugt Projektdateien.
     */
    fun generate(
        projectDir: File,
        projectName: String
    ) {

        generateBuildGradle(
            projectDir,
            projectName
        )

        generateVersionCatalog(
            projectDir
        )
    }

    private fun generateBuildGradle(
        dir: File,
        name: String
    ) {

        File(
            dir,
            "build.gradle.kts"
        ).writeText(
            """
            plugins {
                kotlin("jvm")
            }

            group = "$name"
            """.trimIndent()
        )
    }

    private fun generateVersionCatalog(
        dir: File
    ) {

        val gradleDir = File(
            dir,
            "gradle"
        )

        gradleDir.mkdirs()

        File(
            gradleDir,
            "libs.versions.toml"
        ).writeText(
            """
            [versions]
            kotlin = "2.0.0"
            """.trimIndent()
        )
    }
}
EOF


############################################
# TemplateQueryServiceImpl
############################################

create_file "$IMPL_BASE/service/TemplateQueryServiceImpl.kt" <<'EOF'
package com.mcside.core.template.impl.service

import com.mcside.core.template.api.model.TemplateMetadata
import com.mcside.core.template.api.service.TemplateQueryService
import com.mcside.core.template.data.repository.TemplateVersionRepository

/**
 * Liefert Template-Metadaten.
 */
class TemplateQueryServiceImpl(
    private val repository: TemplateVersionRepository
) : TemplateQueryService {

    override suspend fun getAvailableTemplates(): List<TemplateMetadata> {

        return listOf(
            TemplateMetadata(
                id = "default",
                name = "Default",
                description = "Standard Template",
                version = repository.getLatestVersion("default"),
                thumbnailPath = "templates/default/thumbnail.png"
            )
        )
    }

    override suspend fun getTemplate(
        templateId: String
    ): TemplateMetadata {

        return TemplateMetadata(
            id = templateId,
            name = templateId,
            description = "Template",
            version = repository.getLatestVersion(templateId),
            thumbnailPath = null
        )
    }
}
EOF


############################################
# TemplateManagerImpl
############################################

create_file "$IMPL_BASE/service/TemplateManagerImpl.kt" <<'EOF'
package com.mcside.core.template.impl.service

import com.mcside.core.template.api.model.ProjectCreationConfig
import com.mcside.core.template.api.service.TemplateManager
import com.mcside.core.template.impl.extractor.ZipExtractor
import com.mcside.core.template.impl.generator.ProjectGenerator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * Orchestriert Template-Erstellung.
 */
class TemplateManagerImpl(
    private val ioDispatcher: CoroutineDispatcher,
    private val zipExtractor: ZipExtractor,
    private val generator: ProjectGenerator
) : TemplateManager {

    override fun createProject(
        config: ProjectCreationConfig
    ): Flow<Int> {

        return flow {

            emit(10)

            val zip = File(
                config.targetDirectory,
                "template.zip"
            )

            val extracted = File(
                config.targetDirectory,
                config.projectName
            )

            emit(30)

            zipExtractor.extract(
                zip,
                extracted
            )

            emit(70)

            generator.generate(
                extracted,
                config.projectName
            )

            emit(100)

        }.flowOn(ioDispatcher)
    }
}
EOF


############################################
# ZIP
############################################

echo "Erzeuge ZIP..."

if command -v zip >/dev/null 2>&1; then
    zip -rq core-template-part2.zip "$ROOT"
    echo "Fertig: core-template-part2.zip"
else
    echo "zip nicht installiert."
    echo "Projektstruktur erzeugt:"
    echo "$ROOT"
fi