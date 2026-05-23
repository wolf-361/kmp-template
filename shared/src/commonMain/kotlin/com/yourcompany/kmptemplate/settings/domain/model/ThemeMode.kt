package com.yourcompany.kmptemplate.settings.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.find { it.name == key } ?: SYSTEM
    }
}
