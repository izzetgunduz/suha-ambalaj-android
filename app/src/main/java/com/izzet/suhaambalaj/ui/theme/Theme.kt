package com.izzet.suhaambalaj.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Bizim Asil B2B Renklerimiz
private val LightColorScheme = lightColorScheme(
    primary = ObsidianNavy,
    secondary = MetallicCyan,
    background = PearlGrey,
    surface = PureWhite,
    onPrimary = PureWhite,
    onBackground = ObsidianNavy,
    onSurface = ObsidianNavy
)

// Hata vermemesi için Dark temayı da bizim renklere bağlıyoruz
private val DarkColorScheme = darkColorScheme(
    primary = ObsidianNavy,
    secondary = MetallicCyan,
    background = PearlGrey,
    surface = PureWhite,
    onPrimary = PureWhite,
    onBackground = ObsidianNavy,
    onSurface = ObsidianNavy
)

@Composable
fun SuhaAmbalajTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Saha ve depo uygulamasında okunabilirliği artırmak için cihazı her zaman bizim açık temamıza zorluyoruz
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Telefonun en üstündeki saat/şarj göstergesi çubuğunu bizim %30 lacivert yapıyoruz
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}