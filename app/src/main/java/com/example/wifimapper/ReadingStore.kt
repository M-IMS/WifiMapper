package com.example.wifimapper

import android.content.Context
import org.json.JSONArray
import java.io.File

/**
 * All data lives in this app's private internal storage
 * (/data/data/com.example.wifimapper/files/readings.json on the device).
 * This directory is sandboxed by Android to this app only — no other app
 * can read it, and nothing here is ever sent over the network.
 */
class ReadingStore(private val context: Context) {

    private val file: File
        get() = File(context.filesDir, "readings.json")

    fun loadAll(): MutableList<WifiReading> {
        if (!file.exists()) return mutableListOf()
        val text = file.readText()
        if (text.isBlank()) return mutableListOf()
        val arr = JSONArray(text)
        val out = mutableListOf<WifiReading>()
        for (i in 0 until arr.length()) {
            out.add(WifiReading.fromJson(arr.getJSONObject(i)))
        }
        return out
    }

    fun saveAll(readings: List<WifiReading>) {
        val arr = JSONArray()
        readings.forEach { arr.put(it.toJson()) }
        file.writeText(arr.toString())
    }

    fun clear() {
        if (file.exists()) file.delete()
    }

    /** Exports a human-readable copy into the app's external files dir,
     *  still private to this app, but visible via a file manager / USB
     *  if the user wants to move it themselves. Nothing is uploaded anywhere. */
    fun exportReadable(readings: List<WifiReading>): File {
        val exportDir = context.getExternalFilesDir(null) ?: context.filesDir
        val outFile = File(exportDir, "wifi_map_export_${System.currentTimeMillis()}.json")
        val arr = JSONArray()
        readings.forEach { arr.put(it.toJson()) }
        outFile.writeText(arr.toString(2))
        return outFile
    }
}
