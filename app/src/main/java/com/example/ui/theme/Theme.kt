package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val CarbonDarkColorScheme = darkColorScheme(
    primary = PrimaryLavender,
    secondary = AccentMagenta,
    tertiary = AccentAmber,
    background = BgCarbon,
    surface = SurfaceSlate,
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme for a premium video editing visual environment
    dynamicColor: Boolean = false, // Use our handcrafted cinematic scheme to look professional
    content: @Composable () -> Unit,
) {
    val colorScheme = CarbonDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
