#!/usr/bin/env bash
set -e

ROOT="core-template-part1"

echo "Erzeuge Projektstruktur..."
rm -rf "$ROOT"
mkdir -p "$ROOT"

create_file() {
  local path="$1"
  mkdir -p "$(dirname "$path")"
  cat > "$path"
}

############################################
# build.gradle.kts
############################################

create_file "$ROOT/core/template/api/build.gradle.kts" <<'EOF'
plugins {
    alias(libs.plugins.kotlin.android.library)
}

android {
    namespace = "com.mcside.core.template.api"

    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
EOF

create_file "$ROOT/core/template/data/build.gradle.kts" <<'EOF'
plugins {
    alias(libs.plugins.kotlin.android.library)
}

android {
    namespace = "com.mcside.core.template.data"

    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:template:api"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
EOF

create_file "$ROOT/core/template/impl/build.gradle.kts" <<'EOF'
plugins {
    alias(libs.plugins.kotlin.android.library)
}

android {
    namespace = "com.mcside.core.template.impl"

    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:template:api"))
    implementation(project(":core:template:data"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
EOF


BASE="$ROOT/core/template/api/src/main/kotlin/com/mcside/core/template/api"

############################################
# Models
############################################

create_file "$BASE/model/ProjectCreationConfig.kt" <<'EOF'
package com.mcside.core.template.api.model

import java.io.File

/**
 * Konfiguration für die Erstellung eines neuen Projektes.
 */
data class ProjectCreationConfig(
    val projectName: String,
    val packageName: String,
    val targetDirectory: File,
    val templateId: String,
    val version: String
)
EOF

create_file "$BASE/model/TemplateMetadata.kt" <<'EOF'
package com.mcside.core.template.api.model

/**
 * Beschreibt Metadaten eines Templates.
 */
data class TemplateMetadata(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val thumbnailPath: String?
)
EOF

############################################
# Provider
############################################

create_file "$BASE/provider/ProjectLocationProvider.kt" <<'EOF'
package com.mcside.core.template.api.provider

import java.io.File

/**
 * Liefert Standard-Speicherorte für neue Projekte.
 */
interface ProjectLocationProvider {

    /**
     * Liefert das Standardverzeichnis für neue Projekte.
     */
    suspend fun getDefaultProjectDirectory(): File
}
EOF

############################################
# Services
############################################

create_file "$BASE/service/TemplateManager.kt" <<'EOF'
package com.mcside.core.template.api.service

import com.mcside.core.template.api.model.ProjectCreationConfig
import kotlinx.coroutines.flow.Flow

/**
 * Verantwortlich für Download, Entpacken
 * und Generierung neuer Projekte.
 */
interface TemplateManager {

    /**
     * Erstellt ein Projekt aus einem Template.
     */
    fun createProject(
        config: ProjectCreationConfig
    ): Flow<Int>
}
EOF

create_file "$BASE/service/TemplateQueryService.kt" <<'EOF'
package com.mcside.core.template.api.service

import com.mcside.core.template.api.model.TemplateMetadata

/**
 * Liefert verfügbare Templates.
 */
interface TemplateQueryService {

    /**
     * Lädt alle Templates.
     */
    suspend fun getAvailableTemplates(): List<TemplateMetadata>

    /**
     * Lädt ein einzelnes Template.
     */
    suspend fun getTemplate(
        templateId: String
    ): TemplateMetadata
}
EOF

############################################
# Exceptions
############################################

create_file "$BASE/exception/TemplateException.kt" <<'EOF'
package com.mcside.core.template.api.exception

/**
 * Basisklasse aller Template-Fehler.
 */
sealed class TemplateException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
EOF

create_file "$BASE/exception/TemplateDownloadException.kt" <<'EOF'
package com.mcside.core.template.api.exception

/**
 * Fehler beim Download.
 */
class TemplateDownloadException(
    message: String,
    cause: Throwable? = null
) : TemplateException(message, cause)
EOF

create_file "$BASE/exception/TemplateExtractionException.kt" <<'EOF'
package com.mcside.core.template.api.exception

/**
 * Fehler beim Entpacken.
 */
class TemplateExtractionException(
    message: String,
    cause: Throwable? = null
) : TemplateException(message, cause)
EOF

create_file "$BASE/exception/TemplateValidationException.kt" <<'EOF'
package com.mcside.core.template.api.exception

/**
 * Validierungsfehler.
 */
class TemplateValidationException(
    message: String,
    cause: Throwable? = null
) : TemplateException(message, cause)
EOF

create_file "$BASE/exception/TemplateNotFoundException.kt" <<'EOF'
package com.mcside.core.template.api.exception

/**
 * Template nicht gefunden.
 */
class TemplateNotFoundException(
    templateId: String
) : TemplateException(
    "Template nicht gefunden: $templateId"
)
EOF


############################################
# ZIP
############################################

echo "Erzeuge ZIP..."

if command -v zip >/dev/null 2>&1; then
    zip -rq core-template-part1.zip "$ROOT"
    echo "Fertig: core-template-part1.zip"
else
    echo "zip nicht installiert."
    echo "Projektstruktur wurde trotzdem erzeugt:"
    echo "$ROOT"
fi