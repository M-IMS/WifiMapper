package com.example.wifimapper

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log

/**
 * Thin wrapper around Android's WifiManager for reading nearby access point
 * signal strengths. Note: Android throttles how often scans can be actively
 * requested (a handful per 2 minutes on Android 9+), so this mostly reads
 * whatever the system's last scan results were rather than forcing a fresh
 * scan every single tap. That's normal OS behavior, not a bug.
 */
class WifiScanner(context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    data class ScanSummary(
        val avgRssiDbm: Int,
        val strongestSsid: String,
        val networkCount: Int
    )

    /** Requests a new scan (subject to OS throttling) then reads results. */
    fun requestScan(): Boolean {
        return try {
            wifiManager.startScan()
        } catch (e: Exception) {
            Log.w("WifiScanner", "startScan failed: ${e.message}")
            false
        }
    }

    /** Reads the most recent scan results available to the system right now. */
    fun readCurrentResults(): ScanSummary {
        val results = try {
            wifiManager.scanResults
        } catch (e: SecurityException) {
            emptyList()
        }

        if (results.isEmpty()) {
            return ScanSummary(avgRssiDbm = -100, strongestSsid = "(none found)", networkCount = 0)
        }

        val avg = results.map { it.level }.average().toInt()
        val strongest = results.maxByOrNull { it.level }
        val ssid = strongest?.SSID?.takeIf { it.isNotBlank() } ?: "(hidden network)"

        return ScanSummary(
            avgRssiDbm = avg,
            strongestSsid = ssid,
            networkCount = results.size
        )
    }
}
