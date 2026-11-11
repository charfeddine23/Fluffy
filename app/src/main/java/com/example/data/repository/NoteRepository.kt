package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.model.ChecklistItem
import com.example.data.model.GlassTint
import com.example.data.model.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getNote(id: Long): Flow<NoteEntity?> = noteDao.getNoteById(id)

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    suspend fun insertOrUpdate(note: NoteEntity): Long {
        return if (note.id == 0L) {
            noteDao.insertNote(note)
        } else {
            noteDao.updateNote(note)
            note.id
        }
    }

    suspend fun deleteNote(note: NoteEntity) = noteDao.deleteNote(note)

    suspend fun deleteById(id: Long) = noteDao.deleteById(id)

    suspend fun togglePin(note: NoteEntity) {
        noteDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleFavorite(note: NoteEntity) {
        noteDao.updateNote(note.copy(isFavorite = !note.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun populateInitialDataIfEmpty() {
        val existing = noteDao.getAllNotes().first()
        if (existing.isEmpty()) {
            val sampleNotes = listOf(
                NoteEntity(
                    title = "مرحباً بك في Fluffy ✨",
                    content = "تطبيق الملاحظات المصمم بأحدث تقنيات الزجاج السائل Liquid Glass مع التشتت اللوني وانكسار الضوء وشبه الشفافية الضبابية Frosted Glass.\n\nجرّب تبديل الوضع الليلي والنهاري وتغيير ألوان الزجاج لتجربة بصرية فريدة!",
                    category = "أفكار",
                    colorPreset = GlassTint.FROST.id,
                    isPinned = true,
                    isFavorite = true,
                    checklistJson = ChecklistItem.listToJson(
                        listOf(
                            ChecklistItem(text = "استكشاف الزجاج السائل Liquid Glass", isDone = true),
                            ChecklistItem(text = "تجربة التشتت اللوني Chromatic Dispersion", isDone = true),
                            ChecklistItem(text = "تدوين ملاحظتك الأولى والتحويل بين الوضعين", isDone = false)
                        )
                    ),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                NoteEntity(
                    title = "قائمة مهام الأسبوع 🎯",
                    content = "قائمة سريعة لمتابعة أولويات العمل والتطوير الشخصي مع مؤشر إنجاز زجاجي تفاعلي.",
                    category = "مهام",
                    colorPreset = GlassTint.CYAN.id,
                    isPinned = true,
                    isFavorite = false,
                    checklistJson = ChecklistItem.listToJson(
                        listOf(
                            ChecklistItem(text = "مراجعة تصميم الواجهات الزجاجية", isDone = true),
                            ChecklistItem(text = "إضافة ملاحظة صوتية جديدة", isDone = false),
                            ChecklistItem(text = "تنظيم الأفكار حسب الأقسام الملونة", isDone = false)
                        )
                    ),
                    createdAt = System.currentTimeMillis() - 3600000,
                    updatedAt = System.currentTimeMillis() - 3600000
                ),
                NoteEntity(
                    title = "خاطرة إبداعية: انعكاس الضوء 🌈",
                    content = "عندما يمر الضوء الأبيض عبر منشور زجاجي مشتت، يتحلل إلى أطيافه الساحرة. هكذا تولد الأفكار الرائعة عندما نتأملها من زوايا مختلفة.",
                    category = "شخصي",
                    colorPreset = GlassTint.ROSE.id,
                    isPinned = false,
                    isFavorite = true,
                    audioDurationSec = 42,
                    createdAt = System.currentTimeMillis() - 7200000,
                    updatedAt = System.currentTimeMillis() - 7200000
                ),
                NoteEntity(
                    title = "Liquid Glass Architecture 💎",
                    content = "Key aesthetic components:\n• Frosted blurred backdrop with floating aura orbs\n• Iridescent chromatic dispersion border\n• Inner shadow specular rim & ambient occlusion\n• Dynamic light & dark mode refraction",
                    category = "عمل",
                    colorPreset = GlassTint.AMETHYST.id,
                    isPinned = false,
                    isFavorite = false,
                    createdAt = System.currentTimeMillis() - 14400000,
                    updatedAt = System.currentTimeMillis() - 14400000
                )
            )
            sampleNotes.forEach { noteDao.insertNote(it) }
        }
    }
}
