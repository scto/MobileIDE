// Dieses Template ist als eigenständiges Projekt gedacht.
// Es wird davon ausgegangen, dass das 'plugin-api'-Artefakt in einem Maven-Repository (z.B. Maven Central oder ein privates) veröffentlicht wurde.

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Fügen Sie hier Ihr Firmen-Repository hinzu, falls erforderlich
        // maven { url = uri("https.your.repo.url") }
    }
}

rootProject.name = "your-plugin-name"
