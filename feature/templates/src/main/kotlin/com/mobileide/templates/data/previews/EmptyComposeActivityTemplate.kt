package com.mobileide.templates.data.previews

import com.mobileide.templates.model.Template
import com.mobileide.templates.model.TemplateFile

fun getEmptyComposeActivityTemplate(): Template {
    return Template(
        id = "empty_compose_activity",
        name = "Empty Compose Activity",
        description = "Creates a new project with a single, empty Jetpack Compose activity.",
        category = "Compose",
        files = listOf(
            TemplateFile(
                path = "app/src/main/AndroidManifest.xml",
                content = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                        package="${'$'}{packageName}">

                        <application
                            android:allowBackup="true"
                            android:icon="@mipmap/ic_launcher"
                            android:label="@string/app_name"
                            android:roundIcon="@mipmap/ic_launcher_round"
                            android:supportsRtl="true"
                            android:theme="@style/Theme.${'$'}{projectName}">
                            <activity
                                android:name=".MainActivity"
                                android:exported="true"
                                android:label="@string/app_name"
                                android:theme="@style/Theme.${'$'}{projectName}">
                                <intent-filter>
                                    <action android:name="android.intent.action.MAIN" />
                                    <category android:name="android.intent.category.LAUNCHER" />
                                </intent-filter>
                            </activity>
                        </application>
                    </manifest>
                """.trimIndent()
            ),
            TemplateFile(
                path = "app/src/main/kotlin/${'$'}{packagePath}/MainActivity.kt",
                content = """
                    package ${'$'}{packageName}

                    import android.os.Bundle
                    import androidx.activity.ComponentActivity
                    import androidx.activity.compose.setContent
                    import androidx.compose.foundation.layout.fillMaxSize
                    import androidx.compose.material3.MaterialTheme
                    import androidx.compose.material3.Surface
                    import androidx.compose.material3.Text
                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier
                    import androidx.compose.ui.tooling.preview.Preview
                    import ${'$'}{packageName}.ui.theme.${'$'}{projectName}Theme

                    class MainActivity : ComponentActivity() {
                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            setContent {
                                ${'$'}{projectName}Theme {
                                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                        Greeting("Android")
                                    }
                                }
                            }
                        }
                    }

                    @Composable
                    fun Greeting(name: String, modifier: Modifier = Modifier) {
                        Text(
                            text = "Hello ${'$'}name!",
                            modifier = modifier
                        )
                    }

                    @Preview(showBackground = true)
                    @Composable
                    fun GreetingPreview() {
                        ${'$'}{projectName}Theme {
                            Greeting("Android")
                        }
                    }
                """.trimIndent()
            ),
            // Weitere Dateien wie build.gradle.kts, Theme.kt etc.
        )
    )
}
