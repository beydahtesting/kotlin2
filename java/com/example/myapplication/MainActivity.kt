package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.myapplication.navigation.NavGraph
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.storage.ImageCache  // Import ImageCache
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Always clear cache when app starts
        ImageCache.clearCache(this)
        Log.d("MainActivity", "🗑️ Cache Cleared on App Start")

        // ✅ Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("MainActivity", "❌ Unable to load OpenCV!")
        } else {
            Log.d("MainActivity", "✅ OpenCV loaded successfully")
        }

        setContent {
            MyApplicationTheme {
                NavGraph(navController = androidx.navigation.compose.rememberNavController())
            }
        }
    }

    // ✅ Clears cache when the app is fully removed from memory
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_COMPLETE) {
            ImageCache.clearCache(this)
            Log.d("MainActivity", "🗑️ Cache Cleared on App Closure (Trim Memory)")
        }
    }

    // ✅ Clears cache when the activity is destroyed (for extra safety)
    override fun onDestroy() {
        super.onDestroy()
        ImageCache.clearCache(this)
        Log.d("MainActivity", "🗑️ Cache Cleared on Activity Destroy")
    }
}
