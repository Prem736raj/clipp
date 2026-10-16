package com.example.data

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun fromString(value: String): List<String> {
        val jsonArray = try { JSONArray(value) } catch (e: Exception) { JSONArray() }
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return JSONArray(list).toString()
    }
}
