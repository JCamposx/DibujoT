package edu.robotics.dibujot.ui.gallery

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.robotics.dibujot.R
import edu.robotics.dibujot.data.DrawingItem
import edu.robotics.dibujot.data.DrawingRepository
import edu.robotics.dibujot.ui.send.SendActivity

class GalleryActivity : AppCompatActivity() {

    private val adapter = GalleryAdapter(onClick = ::onItemClick)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        setContentView(R.layout.activity_gallery)

        val statusBarBg = findViewById<View>(R.id.status_bar_bg)
        ViewCompat.setOnApplyWindowInsetsListener(statusBarBg) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.layoutParams.height = statusBarHeight
            v.requestLayout()
            insets
        }

        val recyclerView: RecyclerView = findViewById(R.id.rv_gallery)
        // Use 3 columns in landscape for better use of wider screen space
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val columns = if (isLandscape) COLUMN_COUNT_LANDSCAPE else COLUMN_COUNT_PORTRAIT
        recyclerView.layoutManager = GridLayoutManager(this, columns)
        recyclerView.adapter = adapter

        adapter.submitList(DrawingRepository.getAll())
    }

    private fun onItemClick(item: DrawingItem) {
        val intent = Intent(this, SendActivity::class.java).apply {
            putExtra(SendActivity.EXTRA_DRAWING_ITEM, item)
        }
        startActivity(intent)
    }

    companion object {
        private const val COLUMN_COUNT_PORTRAIT = 2
        private const val COLUMN_COUNT_LANDSCAPE = 3
    }
}
