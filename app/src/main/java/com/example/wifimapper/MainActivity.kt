package com.example.wifimapper

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.wifimapper.databinding.ActivityMainBinding
import com.example.wifimapper.databinding.LayoutSettingsSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var store: ReadingStore
    private lateinit var scanner: WifiScanner
    private var readings = mutableListOf<WifiReading>()

    // Pending tap location, held while we request runtime permissions / scan.
    private var pendingTap: Pair<Float, Float>? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                binding.heatmapView.floorPlan = bmp
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.all { it }
        if (granted) {
            pendingTap?.let { (x, y) -> performScanAt(x, y) }
        } else {
            Toast.makeText(
                this,
                "Location permission is required by Android to read WiFi scan results.",
                Toast.LENGTH_LONG
            ).show()
        }
        pendingTap = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load dark mode preference before super.onCreate
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = ReadingStore(this)
        scanner = WifiScanner(this)
        readings = store.loadAll()
        binding.heatmapView.readings = readings
        
        updateStatus()

        binding.loadMapButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.clearButton.setOnClickListener {
            readings.clear()
            store.saveAll(readings)
            binding.heatmapView.readings = readings
            updateStatus()
        }

        binding.exportButton.setOnClickListener {
            val file = store.exportReadable(readings)
            Toast.makeText(
                this,
                "Exported to app's private folder:\n${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.settingsButton.setOnClickListener {
            showSettingsSheet()
        }

        binding.arFab.setOnClickListener {
            val intent = android.content.Intent(this, ARActivity::class.java)
            startActivity(intent)
        }

        binding.heatmapView.onTapListener = { x, y ->
            ensurePermissionsThenScan(x, y)
        }
    }

    private fun showSettingsSheet() {
        val sheetBinding = LayoutSettingsSheetBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheetBinding.root)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        
        // Sync sheet UI with current state
        sheetBinding.heatmapSwitch.isChecked = binding.heatmapView.showHeatmap
        sheetBinding.blankMapSwitch.isChecked = binding.heatmapView.useBlankMap
        sheetBinding.darkModeSwitch.isChecked = prefs.getBoolean("dark_mode", false)

        sheetBinding.heatmapSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.heatmapView.showHeatmap = isChecked
        }

        sheetBinding.blankMapSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.heatmapView.useBlankMap = isChecked
            updateStatus()
        }

        sheetBinding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        sheetBinding.resetViewButton.setOnClickListener {
            binding.heatmapView.resetZoom()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun ensurePermissionsThenScan(x: Float, y: Float) {
        val needed = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        ).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isEmpty()) {
            performScanAt(x, y)
        } else {
            pendingTap = x to y
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun performScanAt(xFraction: Float, yFraction: Float) {
        scanner.requestScan()
        binding.heatmapView.postDelayed({
            val summary = scanner.readCurrentResults()
            val reading = WifiReading(
                xFraction = xFraction,
                yFraction = yFraction,
                avgRssiDbm = summary.avgRssiDbm,
                strongestSsid = summary.strongestSsid,
                networkCount = summary.networkCount,
                timestampMillis = System.currentTimeMillis()
            )
            readings.add(reading)
            store.saveAll(readings)
            binding.heatmapView.readings = readings
            updateStatus()
        }, 1500)
    }

    private fun updateStatus() {
        val modeText = if (binding.heatmapView.floorPlan == null && !binding.heatmapView.useBlankMap) {
            "Load a floor plan or enable Blank Map mode."
        } else {
            "Tap map to log reading."
        }
        binding.statusText.text = "$modeText (${readings.size} points)"
        binding.statusText.visibility = if (modeText.isNotEmpty()) View.VISIBLE else View.GONE
    }
}
