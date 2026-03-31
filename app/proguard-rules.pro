# MobileIDE ProGuard rules

# Sora Editor
-keep class io.github.rosemoe.sora.** { *; }
-dontwarn io.github.rosemoe.sora.**

# TextMate4j / tm4e
-keep class org.eclipse.tm4e.** { *; }
-dontwarn org.eclipse.tm4e.**

# DataStore
-keep class androidx.datastore.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Termux view
-keep class com.termux.view.** { *; }
-dontwarn com.termux.view.**

# JNI Native library for terminal
-keepclasseswithmembernames class com.mobileide.app.terminal.** {
    native <methods>;
}
