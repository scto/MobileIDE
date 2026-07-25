package com.scto.mobile.ide.core.terminal.exec

import android.content.Context
import android.util.Log

data class ProcessResult(
    val exitCode: Int,
    val output: String
)

object CommandLineToolsPrerequisites {

    suspend fun ensureCommandLineToolsPrerequisites(
        context: Context,
        distroName: String,
        executeInContainer: (command: String) -> ProcessResult
    ): Result<Unit> {
        val isAlpine = distroName.equals("alpine", ignoreCase = true)
        val isDebianOrUbuntu = distroName.equals("ubuntu", ignoreCase = true) || distroName.equals("debian", ignoreCase = true)

        if (!isAlpine && !isDebianOrUbuntu) {
            return Result.failure(IllegalStateException("Distribution konnte nicht erkannt werden: '$distroName'"))
        }

        Log.i("Prereqs", "Checking command line tools prerequisites for distro: $distroName")

        // 1. Vorab-Check: sind unzip/wget bereits vorhanden?
        val checkResult = executeInContainer("command -v unzip && command -v wget")
        if (checkResult.exitCode == 0) {
            Log.i("Prereqs", "Prerequisites (unzip, wget) are already installed.")
            return Result.success(Unit)
        }

        // 2. Distro-abhängiger Installationsbefehl
        val installCommand = if (isAlpine) {
            "apk add --no-cache unzip wget curl ca-certificates fontconfig gcompat"
        } else {
            "apt-get update -y && apt-get install -y unzip wget curl ca-certificates fontconfig"
        }

        Log.i("Prereqs", "Executing prerequisite install command: $installCommand")
        val result = executeInContainer(installCommand)
        return if (result.exitCode == 0) {
            Log.i("Prereqs", "Prerequisite installation succeeded.")
            Result.success(Unit)
        } else {
            val errorMsg = "Voraussetzungsinstallation fehlgeschlagen (Exit ${result.exitCode}): $installCommand\n${result.output}"
            Log.e("Prereqs", errorMsg)
            Result.failure(IllegalStateException(errorMsg))
        }
    }
}
