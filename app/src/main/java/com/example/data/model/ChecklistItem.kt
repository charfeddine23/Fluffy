package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val isDone: Boolean = false
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("text", text)
            put("isDone", isDone)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): ChecklistItem {
            return ChecklistItem(
                id = obj.optString("id", UUID.randomUUID().toString()),
                text = obj.optString("text", ""),
                isDone = obj.optBoolean("isDone", false)
            )
        }

        fun listToJson(items: List<ChecklistItem>): String {
            val array = JSONArray()
            items.forEach { array.put(it.toJsonObject()) }
            return array.toString()
        }

        fun jsonToList(json: String): List<ChecklistItem> {
            if (json.isBlank()) return emptyList()
            return try {
                val array = JSONArray(json)
                val list = mutableListOf<ChecklistItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(fromJsonObject(obj))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
