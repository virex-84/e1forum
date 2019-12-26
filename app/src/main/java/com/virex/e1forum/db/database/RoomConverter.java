package com.virex.e1forum.db.database;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Конвертер типов для Room
 */
public class RoomConverter {
    @TypeConverter
    public Map<String, String> stringToMap(String value) {
        if (value == null) {
            return (null);
        }
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        return gson.fromJson(value, type);
    }

    @TypeConverter
    public String mapToString(Map<String, String> value) {
        if (value == null) {
            return (null);
        }
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        return gson.toJson(value, type);
    }
}
