package com.example.data.model

enum class FabMenuStyle(val id: String) {
    ORIGINAL("original"),
    CONSTELLATION("constellation"),
    VERTICAL_STACK("vertical_stack"),
    ORBITAL_ARC("orbital_arc"),
    DIAMOND_MOSAIC("diamond_mosaic");

    val isCustomEnabled: Boolean
        get() = this != ORIGINAL

    companion object {
        val customStyles: List<FabMenuStyle>
            get() = listOf(CONSTELLATION, VERTICAL_STACK, ORBITAL_ARC, DIAMOND_MOSAIC)

        fun fromId(id: String?): FabMenuStyle {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CONSTELLATION
        }
    }
}
