package com.bloodpressure.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ===== 青绿小清新配色 =====

// 主色系 - 健康青绿
val Primary = Color(0xFF009688)           // 青绿
val PrimaryDark = Color(0xFF00695C)       // 深青绿
val PrimaryLight = Color(0xFFB2DFDB)      // 浅青绿

// 辅助色系
val Secondary = Color(0xFF4DB6AC)          // 浅青绿
val SecondaryDark = Color(0xFF00796B)     // 青绿
val SecondaryLight = Color(0xFFE0F2F1)    // 极浅青绿

// 点缀色 - 珊瑚橙
val Accent = Color(0xFFFF8A65)            // 珊瑚橙
val AccentLight = Color(0xFFFFCCBC)       // 浅珊瑚

// 背景与表面
val AppBackground = Color(0xFFF7F8FA)     // 暖灰白
val SurfaceWhite = Color(0xFFFFFFFF)      // 纯白卡片
val SurfaceVariant = Color(0xFFF0F2F5)    // 浅灰

// 文字
val TextPrimary = Color(0xFF1A1A2E)       // 深灰黑
val TextSecondary = Color(0xFF888888)     // 次要文字
val TextHint = Color(0xFFB0B8C1)         // 提示文字

// 语义色
val SuccessGreen = Color(0xFF81C784)      // 成功-绿
val WarningOrange = Color(0xFFFFB74D)     // 警告-橙
val DangerRed = Color(0xFFE57373)         // 危险-红

// 血压状态色
val BpNormal = Color(0xFF81C784)           // 正常-柔绿
val BpElevated = Color(0xFFFFB74D)         // 临界-柔橙
val BpHigh = Color(0xFFE57373)            // 偏高-柔红

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = SecondaryDark,
    tertiary = Accent,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
    outline = Color(0xFFE8E5E0),
    outlineVariant = Color(0xFFF0F0F0),
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.Black,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = PrimaryLight,
    secondary = SecondaryLight,
    onSecondary = Color.Black,
    secondaryContainer = SecondaryDark,
    onSecondaryContainer = SecondaryLight,
    tertiary = AccentLight,
    background = Color(0xFF1A1A2E),
    onBackground = Color(0xFFE8E5F5),
    surface = Color(0xFF252540),
    onSurface = Color(0xFFE8E5F5),
    surfaceVariant = Color(0xFF33334D),
    onSurfaceVariant = Color(0xFFB0ADBE),
    error = Color(0xFFEF9A9A),
    errorContainer = Color(0xFF5D2020),
    outline = Color(0xFF444462),
    outlineVariant = Color(0xFF33334D),
)

@Composable
fun BloodPressureAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
