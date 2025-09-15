package com.mobileide.languages.servers

import android.content.Context
import com.mobileide.languages.LanguageServerDefinition
import javax.inject.Inject

class KotlinLanguageServer @Inject constructor() : LanguageServerDefinition {
    override val languageId: String = "kotlin"
    override val fileExtensions: List<String> = listOf("kt", "kts")

    override fun createStartCommand(context: Context): List<String> {
        // HIER IST DIE LOGIK ERFORDERLICH:
        // 1. Kopiere die "kotlin-language-server"-Binärdatei aus den app/src/main/assets in den Cache-Speicher der App.
        // 2. Mache die Datei mit `file.setExecutable(true)` ausführbar.
        // 3. Gib den Pfad zur ausführbaren Datei zurück.
        // Beispiel-Implementierung (unvollständig):
        // val serverFile = context.assets.open("kotlin-language-server")....
        // return listOf(serverFile.absolutePath)
        
        // Platzhalter:
        return listOf("/data/data/com.mobileide/cache/kotlin-language-server")
    }
}
