package com.example.data.model

enum class NotesViewMode(val id: String) {
    GRID("grid"),
    LIST("list"),
    GALLERY("gallery"),
    MAGAZINE("magazine");

    val isGridLike: Boolean
        get() = this == GRID || this == GALLERY

    fun next(): NotesViewMode = when (this) {
        GRID -> LIST
        LIST -> GALLERY
        GALLERY -> MAGAZINE
        MAGAZINE -> GRID
    }

    companion object {
        fun fromId(id: String?, fallbackGrid: Boolean = true): NotesViewMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
                ?: if (fallbackGrid) GRID else LIST
        }
    }
}
