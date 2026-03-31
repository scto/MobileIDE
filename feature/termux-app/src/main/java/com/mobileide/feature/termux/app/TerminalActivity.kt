package com.mobileide.feature.termux.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mobileide.feature.termux.shared.TerminalSession

class TerminalActivity : ComponentActivity() {

    private var currentSession: TerminalSession? = null

    companion object {
        init {
            System.loadLibrary("termux-app")
        }
    }

    private external fun stringFromJNI(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TerminalScreen("Terminal: ${stringFromJNI()}")
                }
            }
        }
    }
}

@Composable
fun TerminalScreen(message: String) {
    Text(text = message)
}
