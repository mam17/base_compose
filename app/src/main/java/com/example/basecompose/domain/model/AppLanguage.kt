package com.example.basecompose.domain.model

/**
 * Ngôn ngữ app hỗ trợ. Tên hiển thị bằng chính ngôn ngữ đó nên không cần dịch.
 * Thêm ngôn ngữ: thêm entry ở đây + thư mục `res/values-<code>/strings.xml`.
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val flag: String,
) {
    ENGLISH("en", "English", "🇺🇸"),
    VIETNAMESE("vi", "Tiếng Việt", "🇻🇳");

    companion object {
        val DEFAULT = ENGLISH

        fun fromCode(code: String?): AppLanguage? = entries.firstOrNull { it.code == code }
    }
}
