package dk.thrane.playground.site.api

enum class SnackTag(val title: String, val emoji: String) {
    BURGER("Burger", "\uD83C\uDF54"),
    GRAPES("Grapes", "\uD83C\uDF47"),
    WATERMELON("Watermelon", "\uD83C\uDF49");

    companion object {
        fun fromString(name: String): SnackTag? {
            return values().find { it.name == name }
        }
    }
}
