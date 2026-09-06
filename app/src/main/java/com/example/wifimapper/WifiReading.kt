package com.example.wifimapper

import org.json.JSONObject

/**
 * One logged point on the floor plan: where the user tapped (as a fraction
 * of image width/height, so it scales regardless of screen size), plus the
 * WiFi signal strength measured there.
 */
data class WifiReading(
    val xFraction: Float,
    val yFraction: Float,
    val avgRssiDbm: Int,
    val strongestSsid: String,
    val networkCount: Int,
    val timestampMillis: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("x", xFraction)
        put("y", yFraction)
        put("avgRssiDbm", avgRssiDbm)
        put("strongestSsid", strongestSsid)
        put("networkCount", networkCount)
        put("timestampMillis", timestampMillis)
    }

    companion object {
        fun fromJson(o: JSONObject): WifiReading = WifiReading(
            xFraction = o.getDouble("x").toFloat(),
            yFraction = o.getDouble("y").toFloat(),
            avgRssiDbm = o.getInt("avgRssiDbm"),
            strongestSsid = o.getString("strongestSsid"),
            networkCount = o.getInt("networkCount"),
            timestampMillis = o.getLong("timestampMillis")
        )
    }
}

/**
 * Converts a signal strength in dBm to a 0..1 "quality" score for coloring.
 * -30 dBm = excellent (right next to router), -90 dBm = essentially unusable.
 */
fun rssiToQuality(dbm: Int): Float {
    val clamped = dbm.coerceIn(-90, -30)
    return (clamped + 90) / 60f
}
