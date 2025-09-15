package com.mobileide.templates.data.previews

import com.mobileide.templates.model.Template
import com.mobileide.templates.model.TemplateFile

fun getComposeWithNavigationTemplate(): Template {
    return Template(
        id = "compose_with_navigation",
        name = "Compose with Navigation",
        description = "Creates a Jetpack Compose app with a pre-configured navigation graph for multiple screens.",
        category = "Compose",
        files = listOf(
            TemplateFile(
                path = "app/src/main/kotlin/${'$'}{packagePath}/MainActivity.kt",
                content = """
                    package ${'$'}{packageName}

                    import android.os.Bundle
                    import androidx.activity.ComponentActivity
                    import androidx.activity.compose.setContent
                    import androidx.compose.foundation.layout.Box
                    import androidx.compose.foundation.layout.fillMaxSize
                    import androidx.compose.material3.Button
                    import androidx.compose.material3.MaterialTheme
                    import androidx.compose.material3.Surface
                    import androidx.compose.material3.Text
                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Alignment
                    import androidx.compose.ui.Modifier
                    import androidx.navigation.NavController
                    import androidx.navigation.compose.NavHost
                    import androidx.navigation.compose.composable
                    import androidx.navigation.compose.rememberNavController
                    import ${'$'}{packageName}.ui.theme.${'$'}{projectName}Theme

                    class MainActivity : ComponentActivity() {
                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            setContent {
                                ${'$'}{projectName}Theme {
                                    AppNavigation()
                                }
                            }
                        }
                    }

                    @Composable
                    fun AppNavigation() {
                        val navController = rememberNavController()
                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") { HomeScreen(navController) }
                            composable("details") { DetailsScreen(navController) }
                        }
                    }

                    @Composable
                    fun HomeScreen(navController: NavController) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            Box(contentAlignment = Alignment.Center) {
                                Button(onClick = { navController.navigate("details") }) {
                                    Text("Go to Details")
                                }
                            }
                        }
                    }

                    @Composable
                    fun DetailsScreen(navController: NavController) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            Box(contentAlignment = Alignment.Center) {
                                Button(onClick = { navController.popBackStack() }) {
                                    Text("Go Back")
                                }
                            }
                        }
                    }
                """.trimIndent()
            ),
            // Weitere Dateien wie build.gradle.kts (mit navigation-compose Abhängigkeit), etc.
        )
    )
}
