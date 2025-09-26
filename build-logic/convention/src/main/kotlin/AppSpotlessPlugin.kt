import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AppSpotlessPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run {
        apply<SpotlessPlugin>()

        configure<SpotlessExtension> {
            
            kotlin {
            target("**/*.kt")
            targetExclude("**/build/**/*.kt")
            ktlint()

            // Look for the first line that doesn't have a block comment (assumed to be the license)
            licenseHeaderFile(rootProject.file("config/spotless/copyright.kt"), "(^(?![\\/ ]\\*).*$)")
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            targetExclude("**/build/**/*.gradle.kts")
            ktlint()

            // Look for the first line that doesn't have a block comment (assumed to be the license)
            licenseHeaderFile(rootProject.file("config/spotless/copyright.kts"), "(^(?![\\/ ]\\*).*$)")
        }
        java {
            target("**/*.java")
            targetExclude("**/build/**/*.java")

            // Use the default importOrder configuration
            importOrder()

            // Cleanthat will refactor your code, but it may break your style: apply it before your formatter
            cleanthat()

            // Use google-java-format
            googleJavaFormat()

            // Fix formatting of type annotations
            formatAnnotations()

            // Look for the first line that doesn't have a block comment (assumed to be the license)
            licenseHeaderFile(rootProject.file("config/spotless/copyright.java"), "(^(?![\\/ ]\\*).*$)")
        }
        groovyGradle {
            target("**/*.gradle")
            targetExclude("**/build/**/*.gradle")

            // Look for the first line that doesn't have a block comment (assumed to be the license)
            licenseHeaderFile(rootProject.file("config/spotless/copyright.gradle"), "(^(?![\\/ ]\\*).*$)")
        }
        format("xml") {
            target("**/*.xml")
            targetExclude("**/build/**/*.xml")

            // Look for the first XML tag that isn't a comment (<!--) or the xml declaration (<?xml)
            licenseHeaderFile(rootProject.file("config/spotless/copyright.xml"), "(<[^!?])")
        }
    }
    /*
    afterEvaluate {
        tasks.named("preBuild") {
            dependsOn("spotlessApply")
        }
    }
    */
}