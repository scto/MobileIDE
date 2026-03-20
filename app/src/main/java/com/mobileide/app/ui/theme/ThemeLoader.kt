package com.mobileide.app.ui.theme

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Properties

private const val TAG = "ThemeLoader"

// ─────────────────────────────────────────────────────────────────────────────
// Runtime registry
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Live list of all Material3 themes — built-in themes first, then user-installed.
 * Updated by [updateM3Themes]; consumed by the theme picker and [MobileIdeTheme].
 */
val m3Themes = mutableListOf<ThemeHolder>().also { it.addAll(inbuiltM3Themes) }

// ─────────────────────────────────────────────────────────────────────────────
// Storage
// ─────────────────────────────────────────────────────────────────────────────

/** Directory where serialised [ThemeConfig] files are stored. */
fun Context.m3ThemeDir(): File =
    File(filesDir, "m3_themes").also { it.mkdirs() }

// ─────────────────────────────────────────────────────────────────────────────
// Install from JSON file
// ─────────────────────────────────────────────────────────────────────────────

/** Parse a `.json` theme file and install it. Call from a background coroutine. */
suspend fun installM3ThemeFromFile(context: Context, file: File) {
    loadM3ConfigFromJson(file)?.installTheme(context)
}

/** Deserialise a [ThemeConfig] from a JSON file. Returns null on parse error. */
suspend fun loadM3ConfigFromJson(file: File): ThemeConfig? =
    withContext(Dispatchers.IO) {
        runCatching {
            val gson = GsonBuilder()
                .excludeFieldsWithModifiers(java.lang.reflect.Modifier.STATIC)
                .create()
            gson.fromJson(file.readText(), ThemeConfig::class.java)
        }.getOrElse { e ->
            Log.e(TAG, "Failed to parse theme JSON '${file.name}': ${e.message}")
            null
        }
    }

/** Validate, optionally warn about version mismatch, then persist the theme. */
suspend fun ThemeConfig.installTheme(context: Context) = withContext(Dispatchers.IO) {
    if (id == null)   { showAlert(context, "Installation failed", "Theme id is missing.");   return@withContext }
    if (name == null) { showAlert(context, "Installation failed", "Theme name is missing.");  return@withContext }
    if (targetVersion == null) { showAlert(context, "Installation failed", "targetVersion is missing."); return@withContext }

    val currentCode = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
    }.getOrDefault(-1L)

    if (targetVersion.toLong() != currentCode) {
        withContext(Dispatchers.Main) {
            AlertDialog.Builder(context)
                .setTitle("Version mismatch")
                .setMessage(
                    "This theme targets versionCode $targetVersion " +
                    "(installed: $currentCode). It may look incorrect.\n\nInstall anyway?"
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Install") { _, _ ->
                    finishInstall(context, name)
                    updateM3Themes(context)
                }
                .show()
        }
        return@withContext
    }

    finishInstall(context, name)
    updateM3Themes(context)
}

private fun ThemeConfig.finishInstall(context: Context, name: String) {
    runCatching {
        val dest = File(context.m3ThemeDir(), name)
        ObjectOutputStream(FileOutputStream(dest)).use { it.writeObject(this) }
        Log.i(TAG, "Theme '$name' installed")
    }.onFailure { Log.e(TAG, "Failed to save theme '$name'", it) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Load user themes
// ─────────────────────────────────────────────────────────────────────────────

/** Rebuild [m3Themes] from built-ins + all serialised configs in the theme directory. */
fun updateM3Themes(context: Context) {
    m3Themes.clear()
    m3Themes.addAll(inbuiltM3Themes)
    loadUserM3Themes(context)
}

fun loadUserM3Themes(context: Context) {
    context.m3ThemeDir().listFiles()?.forEach { file ->
        runCatching {
            ObjectInputStream(FileInputStream(file)).use { ois ->
                val cfg = ois.readObject() as? ThemeConfig
                if (cfg != null) m3Themes.add(cfg.build())
            }
        }.onFailure {
            Log.w(TAG, "Removing corrupt theme file '${file.name}'")
            file.delete()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Build ThemeHolder from ThemeConfig
// ─────────────────────────────────────────────────────────────────────────────

fun ThemeConfig.build(): ThemeHolder {
    fun Map<String, String>.toProperties(): Properties =
        Properties().also { p -> forEach { (k, v) -> p[k] = v } }

    return ThemeHolder(
        id                  = id!!,
        name                = name!!,
        inheritBase         = inheritBase ?: true,
        lightScheme         = light?.buildColorScheme(isDark = false) ?: blueberry.lightScheme,
        darkScheme          = dark?.buildColorScheme(isDark = true)   ?: blueberry.darkScheme,
        lightTerminalColors = light?.terminalColors?.toProperties() ?: Properties(),
        darkTerminalColors  = dark?.terminalColors?.toProperties()  ?: Properties(),
        lightEditorColors   = mapEditorColorScheme(light?.editorColors),
        darkEditorColors    = mapEditorColorScheme(dark?.editorColors),
        lightTokenColors    = light?.tokenColors.toTokenColorArray(),
        darkTokenColors     = dark?.tokenColors.toTokenColorArray(),
    )
}

/** Normalise tokenColors (JsonObject shorthand or TextMate JsonArray) to TextMate JsonArray. */
private fun JsonElement?.toTokenColorArray(): JsonArray {
    if (this == null || isJsonNull) return JsonArray()
    return when {
        isJsonArray  -> asJsonArray
        isJsonObject -> JsonArray().also { out ->
            asJsonObject.entrySet().forEach { (scope, colorEl) ->
                if (!colorEl.isJsonPrimitive) { Log.w(TAG, "Invalid token value for '$scope'"); return@forEach }
                out.add(JsonObject().apply {
                    addProperty("scope", scope)
                    add("settings", JsonObject().apply { addProperty("foreground", colorEl.asString) })
                })
            }
        }
        else -> JsonArray()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Build ColorScheme from ThemePalette
// ─────────────────────────────────────────────────────────────────────────────

fun ThemePalette.buildColorScheme(isDark: Boolean): ColorScheme {
    val base = if (isDark) blueberry.darkScheme else blueberry.lightScheme
    fun String?.c(fb: Color) = this?.toColorSafe() ?: fb
    val b = baseColors
    return if (isDark) {
        darkColorScheme(
            primary              = b?.primary.c(base.primary),
            onPrimary            = b?.onPrimary.c(base.onPrimary),
            primaryContainer     = b?.primaryContainer.c(base.primaryContainer),
            onPrimaryContainer   = b?.onPrimaryContainer.c(base.onPrimaryContainer),
            secondary            = b?.secondary.c(base.secondary),
            onSecondary          = b?.onSecondary.c(base.onSecondary),
            secondaryContainer   = b?.secondaryContainer.c(base.secondaryContainer),
            onSecondaryContainer = b?.onSecondaryContainer.c(base.onSecondaryContainer),
            tertiary             = b?.tertiary.c(base.tertiary),
            onTertiary           = b?.onTertiary.c(base.onTertiary),
            tertiaryContainer    = b?.tertiaryContainer.c(base.tertiaryContainer),
            onTertiaryContainer  = b?.onTertiaryContainer.c(base.onTertiaryContainer),
            error                = b?.error.c(base.error),
            onError              = b?.onError.c(base.onError),
            errorContainer       = b?.errorContainer.c(base.errorContainer),
            onErrorContainer     = b?.onErrorContainer.c(base.onErrorContainer),
            background           = b?.background.c(base.background),
            onBackground         = b?.onBackground.c(base.onBackground),
            surface              = b?.surface.c(base.surface),
            onSurface            = b?.onSurface.c(base.onSurface),
            surfaceVariant       = b?.surfaceVariant.c(base.surfaceVariant),
            onSurfaceVariant     = b?.onSurfaceVariant.c(base.onSurfaceVariant),
            outline              = b?.outline.c(base.outline),
            outlineVariant       = b?.outlineVariant.c(base.outlineVariant),
            scrim                = b?.scrim.c(base.scrim),
            inverseSurface       = b?.inverseSurface.c(base.inverseSurface),
            inverseOnSurface     = b?.inverseOnSurface.c(base.inverseOnSurface),
            inversePrimary       = b?.inversePrimary.c(base.inversePrimary),
            surfaceTint          = b?.surfaceTint.c(base.surfaceTint),
            surfaceDim           = b?.surfaceDim.c(base.surfaceDim),
            surfaceBright        = b?.surfaceBright.c(base.surfaceBright),
            surfaceContainerLowest  = b?.surfaceContainerLowest.c(base.surfaceContainerLowest),
            surfaceContainerLow     = b?.surfaceContainerLow.c(base.surfaceContainerLow),
            surfaceContainer        = b?.surfaceContainer.c(base.surfaceContainer),
            surfaceContainerHigh    = b?.surfaceContainerHigh.c(base.surfaceContainerHigh),
            surfaceContainerHighest = b?.surfaceContainerHighest.c(base.surfaceContainerHighest),
        )
    } else {
        lightColorScheme(
            primary              = b?.primary.c(base.primary),
            onPrimary            = b?.onPrimary.c(base.onPrimary),
            primaryContainer     = b?.primaryContainer.c(base.primaryContainer),
            onPrimaryContainer   = b?.onPrimaryContainer.c(base.onPrimaryContainer),
            secondary            = b?.secondary.c(base.secondary),
            onSecondary          = b?.onSecondary.c(base.onSecondary),
            secondaryContainer   = b?.secondaryContainer.c(base.secondaryContainer),
            onSecondaryContainer = b?.onSecondaryContainer.c(base.onSecondaryContainer),
            tertiary             = b?.tertiary.c(base.tertiary),
            onTertiary           = b?.onTertiary.c(base.onTertiary),
            tertiaryContainer    = b?.tertiaryContainer.c(base.tertiaryContainer),
            onTertiaryContainer  = b?.onTertiaryContainer.c(base.onTertiaryContainer),
            error                = b?.error.c(base.error),
            onError              = b?.onError.c(base.onError),
            errorContainer       = b?.errorContainer.c(base.errorContainer),
            onErrorContainer     = b?.onErrorContainer.c(base.onErrorContainer),
            background           = b?.background.c(base.background),
            onBackground         = b?.onBackground.c(base.onBackground),
            surface              = b?.surface.c(base.surface),
            onSurface            = b?.onSurface.c(base.onSurface),
            surfaceVariant       = b?.surfaceVariant.c(base.surfaceVariant),
            onSurfaceVariant     = b?.onSurfaceVariant.c(base.onSurfaceVariant),
            outline              = b?.outline.c(base.outline),
            outlineVariant       = b?.outlineVariant.c(base.outlineVariant),
            scrim                = b?.scrim.c(base.scrim),
            inverseSurface       = b?.inverseSurface.c(base.inverseSurface),
            inverseOnSurface     = b?.inverseOnSurface.c(base.inverseOnSurface),
            inversePrimary       = b?.inversePrimary.c(base.inversePrimary),
            surfaceTint          = b?.surfaceTint.c(base.surfaceTint),
            surfaceDim           = b?.surfaceDim.c(base.surfaceDim),
            surfaceBright        = b?.surfaceBright.c(base.surfaceBright),
            surfaceContainerLowest  = b?.surfaceContainerLowest.c(base.surfaceContainerLowest),
            surfaceContainerLow     = b?.surfaceContainerLow.c(base.surfaceContainerLow),
            surfaceContainer        = b?.surfaceContainer.c(base.surfaceContainer),
            surfaceContainerHigh    = b?.surfaceContainerHigh.c(base.surfaceContainerHigh),
            surfaceContainerHighest = b?.surfaceContainerHighest.c(base.surfaceContainerHighest),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun String.toColorSafe(): Color =
    runCatching { Color(toColorInt()) }.getOrElse { Color.Unspecified }

private suspend fun showAlert(context: Context, title: String, msg: String) {
    withContext(Dispatchers.Main) {
        AlertDialog.Builder(context)
            .setTitle(title).setMessage(msg)
            .setPositiveButton("OK", null).show()
    }
}
