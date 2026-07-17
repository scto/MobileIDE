package com.scto.mobile.ide.files

enum class BuiltinFileType(val extensions: List<String>) {
    IMAGE(listOf("png", "jpg", "jpeg", "gif", "webp", "bmp")),
    AUDIO(listOf("mp3", "wav", "ogg", "aac", "flac")),
    VIDEO(listOf("mp4", "avi", "mkv", "webm", "mov")),
    ARCHIVE(listOf("zip", "tar", "gz", "bz2", "7z")),
    APK(listOf("apk")),
    EXECUTABLE(listOf("sh", "bin", "exe", "apk", "dex"));
}
