package com.example.wifimapper

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.wifimapper.databinding.ActivityArBinding
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.node.ViewNode
import com.google.ar.sceneform.rendering.ViewRenderable

class ARActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArBinding
    private lateinit var scanner: WifiScanner
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (!cameraGranted || !locationGranted) {
            Toast.makeText(this, "Camera and Location permissions are required for AR mode", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scanner = WifiScanner(this)

        val neededPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        val missingPermissions = neededPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(neededPermissions)
        }

        binding.closeArButton.setOnClickListener { finish() }

        binding.logArButton.setOnClickListener {
            logPoint()
        }

        setupArScene()
    }

    private fun setupArScene() {
        binding.sceneView.apply {
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            planeRenderer.isVisible = true
            
            onArFrame = { frame ->
                val camera = frame.camera
                if (camera.trackingState == TrackingState.TRACKING) {
                    binding.arStatusText.text = getString(R.string.ar_tracking_ready)
                } else {
                    binding.arStatusText.text = getString(R.string.ar_tracking_searching)
                }
            }
        }
    }

    private fun logPoint() {
        val sceneView = binding.sceneView
        val frame = sceneView.currentFrame ?: return
        
        if (frame.camera.trackingState != TrackingState.TRACKING) {
            Toast.makeText(this, "Wait for tracking to initialize", Toast.LENGTH_SHORT).show()
            return
        }

        // In SceneView 0.10.0, the ARCore session is accessed via arSession
        val session = sceneView.arSession ?: return

        // 1. Perform WiFi scan
        scanner.requestScan()
        val results = scanner.readCurrentResults()
        
        // 2. Map quality to color (Green for good, Red for bad)
        val quality = rssiToQuality(results.avgRssiDbm)
        val colorInt = qualityToColor(quality)

        // 3. Create an AR anchor at the camera's current pose
        val cameraPose = frame.camera.pose
        val anchor = session.createAnchor(cameraPose)
        
        // 4. Attach a visual indicator
        // Since procedural SphereNode can be version-dependent, we use a ViewNode with a 
        // colored TextView. This provides a clear, customizable 3D indicator with the signal value.
        val arNode = ArNode(sceneView.engine, anchor)
        val viewNode = ViewNode(sceneView.engine)
        
        // Create a circular colored TextView with the RSSI value
        val size = (48 * resources.displayMetrics.density).toInt()
        val signalView = android.widget.TextView(this).apply {
            layoutParams = ViewGroup.LayoutParams(size, size)
            text = "${results.avgRssiDbm}"
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colorInt)
                setStroke(4, Color.WHITE)
            }
            background = drawable
        }

        ViewRenderable.builder()
            .setView(this, signalView)
            .build()
            .thenAccept { renderable ->
                viewNode.setRenderable(renderable)
            }
        
        arNode.addChild(viewNode)
        sceneView.addChild(arNode)
        
        Toast.makeText(this, "Logged: ${results.avgRssiDbm} dBm at this location", Toast.LENGTH_SHORT).show()
    }

    private fun rssiToQuality(rssi: Int): Float {
        // RSSI values typically range from -100 (worst) to -30 (best)
        val minRssi = -100f
        val maxRssi = -30f
        return ((rssi.toFloat() - minRssi) / (maxRssi - minRssi)).coerceIn(0f, 1f)
    }

    private fun qualityToColor(quality: Float): Int {
        // Hue 0 = Red (Bad), Hue 120 = Green (Good)
        val hue = quality * 120f
        return Color.HSVToColor(floatArrayOf(hue, 0.8f, 0.9f))
    }
}
