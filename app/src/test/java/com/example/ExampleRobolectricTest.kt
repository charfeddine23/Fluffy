package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ChecklistItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Fluffy", appName)
  }

  @Test
  fun `checklist serialization roundtrip`() {
    val items = listOf(
        ChecklistItem(id = "1", text = "Liquid Glass UI", isDone = true),
        ChecklistItem(id = "2", text = "Chromatic Dispersion", isDone = false)
    )
    val json = ChecklistItem.listToJson(items)
    val restored = ChecklistItem.jsonToList(json)
    assertEquals(2, restored.size)
    assertEquals("Liquid Glass UI", restored[0].text)
    assertTrue(restored[0].isDone)
    assertEquals("Chromatic Dispersion", restored[1].text)
  }
}
