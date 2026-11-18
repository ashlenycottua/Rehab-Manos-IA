package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = OnTealPrimary,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = TerracottaSecondary,
    onSecondary = OnTerracottaSecondary,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = SageTertiary,
    onTertiary = OnSageTertiary,
    tertiaryContainer = SageContainer,
    onTertiaryContainer = OnSageContainer,
    background = IvoryBackground,
    onBackground = OnIvoryBackground,
    surface = WarmSurface,
    onSurface = OnWarmSurface,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = OnWarmSurfaceVariant,
    outline = SoftOutline,
    error = CoralError,
    errorContainer = CoralErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = TealDarkPrimary,
    onPrimary = OnTealDarkPrimary,
    primaryContainer = TealDarkContainer,
    onPrimaryContainer = OnTealDarkContainer,
    secondary = TerracottaDarkSecondary,
    onSecondary = OnTerracottaDarkSecondary,
    secondaryContainer = TerracottaDarkContainer,
    onSecondaryContainer = OnTerracottaDarkContainer,
    tertiary = MintAccent,
    onTertiary = OnTealDarkPrimary,
    tertiaryContainer = TealDarkContainer,
    onTertiaryContainer = OnTealDarkContainer,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,
    outline = DarkOutline,
    error = CoralErrorContainer
)

val RehabShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun RehabManosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = RehabShapes,
        content = content
    )
}
