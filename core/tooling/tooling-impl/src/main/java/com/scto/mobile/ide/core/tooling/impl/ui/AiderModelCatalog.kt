package com.scto.mobile.ide.core.tooling.impl.ui

enum class AiderProvider(val displayName: String, val secretFileName: String) {
    GEMINI_FLASH("Gemini Flash", ".gemini_api_key.secrets"),
    GEMINI_PRO("Gemini Pro", ".gemini_api_key.secrets"),
    OPENAI("OpenAI", ".openai_api_key.secrets"),
    DEEPSEEK("DeepSeek", ".deepseek_api_key.secrets"),
    CLAUDE("Claude (Anthropic)", ".anthropic_api_key.secrets"),
}

enum class AiderChatMode(val flagName: String, val displayName: String, val description: String) {
    AUTO("auto", "Auto (Empfohlen)", "Launcher-Empfehlung (Standard)"),
    CODE("code", "Code", "Direktes Coden (am besten für Flash)"),
    ARCHITECT("architect", "Architect", "Planen & Coden (am besten für Pro/Komplexes)"),
    ASK("ask", "Ask", "Nur Fragen stellen (keine Dateiänderungen)"),
    HELP("help", "Help", "Fragen zur Bedienung von Aider"),
}

data class AiderModelSpec(
    val id: String,
    val name: String,
    val provider: AiderProvider,
    val description: String,
    val isArchitectDefault: Boolean = false,
    val isBrowserSupported: Boolean = false
)

object AiderModelCatalog {

    val geminiFlashModels = listOf(
        AiderModelSpec("gemini/gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite", AiderProvider.GEMINI_FLASH, "Lite Preview (1M Tokens Context, extrem schnell)"),
        AiderModelSpec("gemini/gemini-3.1-flash-live-preview", "Gemini 3.1 Flash Live", AiderProvider.GEMINI_FLASH, "Live Interaction (1M Tokens Context)"),
        AiderModelSpec("gemini/gemini-3-flash-preview", "Gemini 3 Flash Preview", AiderProvider.GEMINI_FLASH, "Early Access V3 (1M Tokens Context)"),
        AiderModelSpec("gemini/gemini-2.5-flash", "Gemini 2.5 Flash", AiderProvider.GEMINI_FLASH, "Top-Empfehlung für Alltag & Speed! (1M Tokens Context)"),
        AiderModelSpec("gemini/gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", AiderProvider.GEMINI_FLASH, "Stable Lite (Effizient, ressourcenschonend)"),
        AiderModelSpec("gemini/gemini-2.5-flash-lite-preview-06-17", "Gemini 2.5 Flash Lite (06-17)", AiderProvider.GEMINI_FLASH, "Build 06-17"),
        AiderModelSpec("gemini/gemini-2.5-flash-lite-preview-09-2025", "Gemini 2.5 Flash Lite (09-25)", AiderProvider.GEMINI_FLASH, "Build 09-25"),
        AiderModelSpec("gemini/gemini-2.5-flash-preview-09-2025", "Gemini 2.5 Flash Preview (09-25)", AiderProvider.GEMINI_FLASH, "Preview"),
        AiderModelSpec("gemini/gemini-2.5-computer-use-preview-10-2025", "Gemini 2.5 Computer Use", AiderProvider.GEMINI_FLASH, "Computer Use (Browser Mode)", isBrowserSupported = true),
        AiderModelSpec("gemini/gemini-2.0-flash", "Gemini 2.0 Flash", AiderProvider.GEMINI_FLASH, "V2.0 Standard Flash"),
        AiderModelSpec("gemini/gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite", AiderProvider.GEMINI_FLASH, "V2.0 Lite"),
        AiderModelSpec("gemini/gemini-1.5-flash", "Gemini 1.5 Flash", AiderProvider.GEMINI_FLASH, "V1.5 Stable")
    )

    val geminiProModels = listOf(
        AiderModelSpec("gemini/gemini-3.1-pro-preview", "Gemini 3.1 Pro Preview", AiderProvider.GEMINI_PRO, "Ultra-Intelligence V3.1 (Architect)", isArchitectDefault = true),
        AiderModelSpec("gemini/gemini-3-pro-preview", "Gemini 3 Pro Preview", AiderProvider.GEMINI_PRO, "Early Access V3 Pro", isArchitectDefault = true),
        AiderModelSpec("gemini/gemini-2.5-pro", "Gemini 2.5 Pro", AiderProvider.GEMINI_PRO, "DIE BESTE WAHL FÜR KOTLIN & KOMPLEXE ARCHITEKTUR!", isArchitectDefault = true),
        AiderModelSpec("gemini/gemini-2.0-pro-exp-02-05", "Gemini 2.0 Pro Exp", AiderProvider.GEMINI_PRO, "Experimental Pro 2.0", isArchitectDefault = true),
        AiderModelSpec("gemini/gemini-1.5-pro", "Gemini 1.5 Pro", AiderProvider.GEMINI_PRO, "Stable Pro 1.5", isArchitectDefault = true)
    )

    val openAiModels = listOf(
        AiderModelSpec("o3-mini", "OpenAI o3-mini", AiderProvider.OPENAI, "Reasoning-Modell für komplexe Mathe & Logik", isArchitectDefault = true),
        AiderModelSpec("gpt-4o", "GPT-4o", AiderProvider.OPENAI, "Flaggschiff-Modell von OpenAI"),
        AiderModelSpec("gpt-4o-mini", "GPT-4o-mini", AiderProvider.OPENAI, "Schnelles & günstiges Modell für kleine Edits")
    )

    val deepSeekModels = listOf(
        AiderModelSpec("deepseek/deepseek-chat", "DeepSeek V3 (Chat)", AiderProvider.DEEPSEEK, "Extrem stark im Coden & günstig"),
        AiderModelSpec("deepseek/deepseek-reasoner", "DeepSeek R1 (Reasoner)", AiderProvider.DEEPSEEK, "Deep Reasoning Modell für Algorithmen", isArchitectDefault = true)
    )

    val claudeModels = listOf(
        AiderModelSpec("anthropic/claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", AiderProvider.CLAUDE, "Referenzmodell für präzises Code-Refactoring"),
        AiderModelSpec("anthropic/claude-3-5-haiku-20241022", "Claude 3.5 Haiku", AiderProvider.CLAUDE, "Sehr schnelles Claude-Modell"),
        AiderModelSpec("anthropic/claude-3-opus-20240229", "Claude 3 Opus", AiderProvider.CLAUDE, "Maximales Reasoning", isArchitectDefault = true)
    )

    fun getModelsForProvider(provider: AiderProvider): List<AiderModelSpec> {
        return when (provider) {
            AiderProvider.GEMINI_FLASH -> geminiFlashModels
            AiderProvider.GEMINI_PRO -> geminiProModels
            AiderProvider.OPENAI -> openAiModels
            AiderProvider.DEEPSEEK -> deepSeekModels
            AiderProvider.CLAUDE -> claudeModels
        }
    }

    fun getAllModels(): List<AiderModelSpec> {
        return geminiFlashModels + geminiProModels + openAiModels + deepSeekModels + claudeModels
    }
}
