package com.mobileide.sidepanel

import androidx.compose.runtime.Composable
import com.mobileide.sidepanel.presentation.SidePanelScreen

@Composable
fun SidePanel(onFileSelected: (String) -> Unit) {
    SidePanelScreen(onFileSelected = onFileSelected)
}
