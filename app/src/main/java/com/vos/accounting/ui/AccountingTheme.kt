package com.vos.accounting.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 表示应用外观可选的明暗模式。
 */
enum class AccountingThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * 为记账应用提供统一主题：跟随系统配色时使用动态 Monet 配色，关闭时恢复固定品牌配色。
 */
@Composable
fun AccountingTheme(
    themeMode: AccountingThemeMode,
    followSystemColor: Boolean,
    content: @Composable () -> Unit,
) {
    if (followSystemColor) {
        val colorSchemeMode = when (themeMode) {
            AccountingThemeMode.SYSTEM -> ColorSchemeMode.MonetSystem
            AccountingThemeMode.LIGHT -> ColorSchemeMode.MonetLight
            AccountingThemeMode.DARK -> ColorSchemeMode.MonetDark
        }
        val controller = remember(colorSchemeMode) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                paletteStyle = ThemePaletteStyle.TonalSpot,
                colorSpec = ThemeColorSpec.Spec2025,
            )
        }
        MiuixTheme(controller = controller, content = content)
    } else {
        val dark = when (themeMode) {
            AccountingThemeMode.SYSTEM -> isSystemInDarkTheme()
            AccountingThemeMode.LIGHT -> false
            AccountingThemeMode.DARK -> true
        }
        MiuixTheme(
            colors = if (dark) darkColorScheme() else lightColorScheme(),
            content = content,
        )
    }
}
