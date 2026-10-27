package com.example.data

import android.content.Context
import android.content.SharedPreferences

enum class AppLanguage(val code: String, val displayName: String) {
    SIMPLIFIED_CHINESE("zh-CN", "简体中文"),
    TRADITIONAL_CHINESE("zh-TW", "繁體中文"),
    ENGLISH("en", "English")
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("跟随系统"),
    LIGHT("明亮模式"),
    DARK("暗黑模式")
}

data class CssTheme(
    val id: String,
    val name: String,
    val previewColor: Long,
    val cssContent: String
)

class ThemeSettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("super_markdown_settings", Context.MODE_PRIVATE)

    companion object {
        val PRESET_CSS_THEMES = listOf(
            CssTheme(
                id = "modern_clean",
                name = "默认现代 (Modern Clean)",
                previewColor = 0xFF2563EB,
                cssContent = """
                    /* 默认现代风 */
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", sans-serif;
                    }
                    h1 { color: #1E3A8A; border-bottom: 2px solid #3B82F6; }
                    h2 { color: #1D4ED8; }
                    blockquote { border-left-color: #3B82F6; background: rgba(59, 130, 246, 0.06); }
                """.trimIndent()
            ),
            CssTheme(
                id = "warm_parchment",
                name = "暖木书香 (Parchment)",
                previewColor = 0xFFB45309,
                cssContent = """
                    /* 羊皮书香风 */
                    body {
                        background-color: #FBF7EE !important;
                        color: #382C1E !important;
                        font-family: "Georgia", "Songti SC", "SimSun", serif;
                        line-height: 1.85 !important;
                    }
                    h1, h2, h3 { color: #6D3D14 !important; font-family: "Songti SC", serif; }
                    h1 { border-bottom: 2px solid #D4A373 !important; }
                    blockquote {
                        border-left-color: #B45309 !important;
                        background: #F4EAD4 !important;
                        color: #5C4033 !important;
                    }
                    code { background-color: #ECE0CA !important; color: #8A4B08 !important; }
                """.trimIndent()
            ),
            CssTheme(
                id = "midnight_cyber",
                name = "赛博黑曜 (Midnight Cyber)",
                previewColor = 0xFF10B981,
                cssContent = """
                    /* 赛博黑曜风 */
                    body {
                        background-color: #0B0F19 !important;
                        color: #E2E8F0 !important;
                        font-family: "JetBrains Mono", Consolas, monospace, sans-serif;
                    }
                    h1 { color: #34D399 !important; border-bottom: 2px solid #059669 !important; text-shadow: 0 0 8px rgba(52, 211, 153, 0.4); }
                    h2 { color: #6EE7B7 !important; }
                    a { color: #38BDF8 !important; }
                    .wikilink { color: #A78BFA !important; background: rgba(167, 139, 250, 0.15) !important; border-color: #7C3AED !important; }
                    blockquote { border-left-color: #10B981 !important; background: #111C2D !important; }
                    code { background-color: #1E293B !important; color: #F43F5E !important; }
                """.trimIndent()
            ),
            CssTheme(
                id = "academic_serif",
                name = "典雅学术 (Academic Serif)",
                previewColor = 0xFF475569,
                cssContent = """
                    /* 典雅学术排版 */
                    body {
                        font-family: "Times New Roman", "Baskerville", "Source Serif Pro", "Songti SC", serif;
                        text-align: justify;
                        line-height: 1.8 !important;
                    }
                    h1, h2, h3 { font-family: "Times New Roman", serif; font-weight: normal; letter-spacing: 0.5px; }
                    h1 { text-align: center; border-bottom: 1px double #94A3B8; padding-bottom: 12px; }
                    blockquote { font-style: italic; border-left: 3px solid #64748B; background: transparent; }
                """.trimIndent()
            ),
            CssTheme(
                id = "custom_css",
                name = "自定义 CSS (Custom User CSS)",
                previewColor = 0xFF8B5CF6,
                cssContent = ""
            )
        )
    }

    fun getLanguage(): AppLanguage {
        val code = prefs.getString("app_language", AppLanguage.SIMPLIFIED_CHINESE.code)
        return AppLanguage.values().firstOrNull { it.code == code } ?: AppLanguage.SIMPLIFIED_CHINESE
    }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString("app_language", lang.code).apply()
    }

    fun getThemeMode(): ThemeMode {
        val name = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun getSelectedCssThemeId(): String {
        return prefs.getString("selected_css_theme_id", "modern_clean") ?: "modern_clean"
    }

    fun setSelectedCssThemeId(id: String) {
        prefs.edit().putString("selected_css_theme_id", id).apply()
    }

    fun getCustomCss(): String {
        return prefs.getString("custom_user_css", """
            /* 在此编写您的自定义 CSS 样式 */
            body {
                /* font-family: serif; */
            }
            h1 {
                /* color: #e11d48; */
            }
        """.trimIndent()) ?: ""
    }

    fun setCustomCss(css: String) {
        prefs.edit().putString("custom_user_css", css).apply()
    }

    fun getEffectiveCss(): String {
        val selectedId = getSelectedCssThemeId()
        if (selectedId == "custom_css") {
            return getCustomCss()
        }
        val theme = PRESET_CSS_THEMES.firstOrNull { it.id == selectedId }
        return theme?.cssContent ?: ""
    }

    fun getFontSizeSp(): Int {
        return prefs.getInt("font_size_sp", 16)
    }

    fun setFontSizeSp(sp: Int) {
        prefs.edit().putInt("font_size_sp", sp).apply()
    }

    fun getLastReadCollectionId(): String? {
        return prefs.getString("last_read_collection_id", null)
    }

    fun setLastReadCollectionId(id: String) {
        prefs.edit().putString("last_read_collection_id", id).apply()
    }

    fun getLastReadDocPath(collectionId: String): String? {
        return prefs.getString("last_read_doc_$collectionId", null)
    }

    fun setLastReadDocPath(collectionId: String, docPath: String) {
        prefs.edit().putString("last_read_doc_$collectionId", docPath).apply()
    }

    fun isAutoBackupEnabled(): Boolean {
        return prefs.getBoolean("auto_backup_enabled", true)
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_backup_enabled", enabled).apply()
    }
}
