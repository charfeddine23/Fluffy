package com.example

import com.example.data.model.FabMenuStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FabMenuStyleTest {

    @Test
    fun `entries contain original plus four custom styles`() {
        assertEquals(5, FabMenuStyle.entries.size)
        assertEquals(4, FabMenuStyle.customStyles.size)
        assertFalse(FabMenuStyle.ORIGINAL.isCustomEnabled)
        assertTrue(FabMenuStyle.customStyles.all { it.isCustomEnabled })
    }

    @Test
    fun `fromId resolves known ids case-insensitively and falls back to constellation`() {
        assertEquals(FabMenuStyle.ORIGINAL, FabMenuStyle.fromId("original"))
        assertEquals(FabMenuStyle.CONSTELLATION, FabMenuStyle.fromId("CONSTELLATION"))
        assertEquals(FabMenuStyle.VERTICAL_STACK, FabMenuStyle.fromId("vertical_stack"))
        assertEquals(FabMenuStyle.ORBITAL_ARC, FabMenuStyle.fromId("orbital_arc"))
        assertEquals(FabMenuStyle.DIAMOND_MOSAIC, FabMenuStyle.fromId("diamond_mosaic"))
        assertEquals(FabMenuStyle.VERTICAL_STACK, FabMenuStyle.fromId(null))
        assertEquals(FabMenuStyle.VERTICAL_STACK, FabMenuStyle.fromId("unknown_style"))
    }
}
