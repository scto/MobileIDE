/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.scto.mobile.ide.ui.welcome

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scto.mobile.ide.R
import kotlin.math.cos
import kotlin.math.sin

// --- Background component: fix the issue of light background color being too bright ---
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun WelcomeBackground(
    currentTheme: ThemeColor?,
    isDarkTheme: Boolean,
    monetPrimary: Color? = null, // New: Primary color in Monet mode
    monetTertiary: Color? = null, // New: Accent color in Monet mode
) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    // 1. Determine background base color
    val baseBg =
        if (currentTheme != null) {
            if (isDarkTheme) currentTheme.dark.background else currentTheme.light.background
        } else {
            // Monet / Follow system
            if (isDarkTheme) colorScheme.surface
            else colorScheme.surfaceContainerLowest // Use the brightest background for light mode
        }

    // 2. Determine light blob color
    // Logic: If there is a selected theme, use the theme color. If it is Monet, use the passed dynamic color or default
    // color.
    val spec = if (isDarkTheme) currentTheme?.dark else currentTheme?.light
    val rawPrimary = spec?.primary ?: monetPrimary ?: colorScheme.primary
    val rawAccent = spec?.accent ?: monetTertiary ?: colorScheme.tertiary

    // 3. Critical fix: Visibility of light blobs in light mode
    // In light mode, simple alpha 0.05 is invisible.
    // Trick: Make the light blob color slightly darker (compositeOver Gray), then give a higher alpha.
    val blobAlpha = if (isDarkTheme) 0.15f else 0.12f

    // In light mode, make the light blob color slightly "heavier", otherwise it looks like a dirty smudge on a white
    // background
    val effectivePrimary = if (isDarkTheme) rawPrimary else rawPrimary.compositeOver(Color.Gray)
    val effectiveAccent = if (isDarkTheme) rawAccent else rawAccent.compositeOver(Color.Gray)

    // Animation transition
    val animBg by animateColorAsState(baseBg, tween(600), label = "bg")
    val animPrimary by animateColorAsState(effectivePrimary, tween(600), label = "prim")
    val animAccent by animateColorAsState(effectiveAccent, tween(600), label = "acc")

    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")

    val t1 by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart),
            label = "t1",
        )
    val t2 by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
            label = "t2",
        )

    Box(modifier = Modifier.fillMaxSize().background(animBg)) {
        Canvas(
            modifier =
                Modifier.fillMaxSize()
                    .blur(100.dp) // Blur radius
                    .graphicsLayer { alpha = 1f }
        ) {
            val offset1 =
                Offset(
                    x = screenWidth * 0.5f + (screenWidth * 0.35f) * cos(t1),
                    y = screenHeight * 0.4f + (screenHeight * 0.3f) * sin(t1),
                )
            val offset2 =
                Offset(
                    x = screenWidth * 0.5f - (screenWidth * 0.35f) * cos(t2),
                    y = screenHeight * 0.6f - (screenHeight * 0.3f) * sin(t2),
                )

            drawCircle(color = animPrimary.copy(alpha = blobAlpha), center = offset1, radius = screenWidth * 0.6f)
            drawCircle(color = animAccent.copy(alpha = blobAlpha), center = offset2, radius = screenWidth * 0.5f)
        }
    }
}

// --- Permission card ---
@Composable
internal fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit,
) {
    val borderColor by
        animateColorAsState(
            if (isGranted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            label = "border",
        )
    // Container color: Give a slightly translucent background when unauthorized, making it visible on different base
    // colors
    val containerColor by
        animateColorAsState(
            if (isGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            label = "container",
        )

    Surface(
        onClick = { if (!isGranted) onRequest() },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color =
                    if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint =
                            if (isGranted) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Note: Do not hardcode colors here, rely on LocalContentColor
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    // Use LocalContentColor and add transparency
                    color = LocalContentColor.current.copy(alpha = 0.7f),
                    lineHeight = 16.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (isGranted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.content_desc_permission_granted),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Button(
                    onClick = onRequest,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.height(36.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Text(stringResource(R.string.action_enable), fontSize = 13.sp)
                }
            }
        }
    }
}

// --- Theme preview card ---

@Composable
internal fun ThemePreviewCard(theme: ThemeColor, isSelected: Boolean, isDarkTheme: Boolean, onClick: () -> Unit) {
    // 1. Selected state animation (scaling)
    val scale by
        animateFloatAsState(
            targetValue = if (isSelected) 1.1f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "scale",
        )
    val borderWidth by animateDpAsState(if (isSelected) 3.dp else 0.dp, label = "borderW")

    // 2. Color smooth transition animation (Critical! Solves stiffness issue)
    // Determine target color based on passed isDarkTheme, and apply 500ms animation
    val targetSpec = if (isDarkTheme) theme.dark else theme.light

    val animBgColor by animateColorAsState(targetSpec.background, tween(500), label = "bgColor")
    val animPrimaryColor by animateColorAsState(targetSpec.primary, tween(500), label = "primColor")
    val animSurfaceColor by animateColorAsState(targetSpec.surface, tween(500), label = "surfColor")
    val animBorderColor by animateColorAsState(targetSpec.primary, tween(500), label = "borderColor")

    // 3. "Two blobs contact" position animation
    // We define the positions of two states, when modes switch, generate a dynamic feel through color transition +
    // position fine-tuning
    // Designed here as: blobs slightly apart in Light mode, slightly compact in Dark mode, or vice versa
    val circleOffsetOne by
        animateDpAsState(
            targetValue = if (isDarkTheme) 12.dp else 8.dp, // Move position slightly
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "offset1",
        )
    val circleOffsetTwo by
        animateDpAsState(
            targetValue = if (isDarkTheme) 12.dp else 18.dp,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "offset2",
        )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale)) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(80.dp, 100.dp),
            shape = RoundedCornerShape(20.dp),
            color = animBgColor, // Use animated color
            border = BorderStroke(borderWidth, animBorderColor), // Use animated color
            shadowElevation = if (isSelected) 8.dp else 2.dp,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Upper blob (Primary)
                Box(
                    modifier =
                        Modifier
                            // Use animated Offset to achieve movement effect
                            .offset(x = circleOffsetOne, y = -circleOffsetOne)
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(animPrimaryColor.copy(alpha = 0.8f))
                )

                // Lower blob (Surface/Accent)
                Box(
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            // Use animated Offset to achieve movement effect
                            .offset(x = circleOffsetTwo, y = circleOffsetTwo)
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(animSurfaceColor)
                )

                // Checkmark for selection (keep centered)
                if (isSelected) {
                    Box(
                        modifier =
                            Modifier.align(Alignment.Center)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isDarkTheme) Color.Black else Color.White)
                                .border(1.dp, animPrimaryColor, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Check, null, tint = animPrimaryColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = theme.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

// --- Custom theme card ---
@Composable
internal fun CustomThemeCard(isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isSelected) 1.1f else 1f, label = "scale")
    val borderWidth by animateDpAsState(if (isSelected) 3.dp else 0.dp, label = "borderW")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale)) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(80.dp, 100.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = BorderStroke(borderWidth, MaterialTheme.colorScheme.primary),
            shadowElevation = if (isSelected) 6.dp else 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Palette,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.welcome_custom_theme),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

// --- Bottom navigation bar ---
@Composable
internal fun WelcomeBottomBar(
    pagerState: androidx.compose.foundation.pager.PagerState,
    activeColor: Color,
    onBack: () -> Unit,
    onNext: () -> Unit,
    isLastPage: Boolean,
) {
    // Force navigation bar icon color to ensure visibility
    val iconColor = LocalContentColor.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onBack, enabled = pagerState.currentPage > 0, modifier = Modifier.size(56.dp)) {
            if (pagerState.currentPage > 0) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = iconColor,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(pagerState.pageCount) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val width by animateDpAsState(if (isSelected) 24.dp else 8.dp, label = "w")
                val color by
                    animateColorAsState(if (isSelected) activeColor else iconColor.copy(alpha = 0.3f), label = "c")
                Box(modifier = Modifier.padding(4.dp).height(6.dp).width(width).clip(CircleShape).background(color))
            }
        }

        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = if (isLastPage) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.action_next),
                tint = activeColor,
            )
        }
    }
}
