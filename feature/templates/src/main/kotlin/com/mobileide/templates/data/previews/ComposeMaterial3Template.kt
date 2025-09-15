package com.mobileide.templates.data.previews

import com.mobileide.templates.model.Template
import com.mobileide.templates.model.TemplateFile

fun getComposeMaterial3Template(): Template {
    return Template(
        id = "compose_m3_activity",
        name = "Empty Compose Activity (Material 3)",
        description = "Creates a Compose app with a TopAppBar and some example Material 3 components.",
        category = "Compose",
        files = listOf(
            TemplateFile(
                path = "app/src/main/kotlin/${'$'}{packagePath}/MainActivity.kt",
                content = """
                    package ${'$'}{packageName}

                    import android.os.Bundle
                    import androidx.activity.ComponentActivity
                    import androidx.activity.compose.setContent
                    import androidx.compose.foundation.layout.padding
                    import androidx.compose.material.icons.Icons
                    import androidx.compose.material.icons.filled.Menu
                    import androidx.compose.material3.*
                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier
                    import androidx.compose.ui.tooling.preview.Preview
                    import ${'$'}{packageName}.ui.theme.${'$'}{projectName}Theme

                    class MainActivity : ComponentActivity() {
                        @OptIn(ExperimentalMaterial3Api::class)
                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            setContent {
                                ${'$'}{projectName}Theme {
                                    Scaffold(
                                        topBar = {
                                            TopAppBar(
                                                title = { Text("${'$'}{projectName}") },
                                                navigationIcon = {
                                                    IconButton(onClick = { /*TODO*/ }) {
                                                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                                    }
                                                }
                                            )
                                        }
                                    ) { innerPadding ->
                                        Greeting("Android", modifier = Modifier.padding(innerPadding))
                                    }
                                }
                            }
                        }
                    }

                    @Composable
                    fun Greeting(name: String, modifier: Modifier = Modifier) {
                        Text(text = "Hello ${'$'}name!", modifier = modifier)
                    }

                    @Preview(showBackground = true)
                    @Composable
                    fun DefaultPreview() {
                        ${'$'}{projectName}Theme {
                            Greeting("Android")
                        }
                    }
                """.trimIndent()
            )
        )
    )
}
