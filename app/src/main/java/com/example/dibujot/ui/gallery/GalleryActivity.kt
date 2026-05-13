package com.example.dibujot.ui.gallery

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dibujot.R
import com.example.dibujot.data.DrawingItem
import com.example.dibujot.data.DrawingRepository
import com.example.dibujot.ui.send.SendActivity

class GalleryActivity : AppCompatActivity() {

    private val adapter = GalleryAdapter(onClick = ::onItemClick)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        setContentView(R.layout.activity_gallery)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        val recyclerView: RecyclerView = findViewById(R.id.rv_gallery)
        recyclerView.layoutManager = GridLayoutManager(this, COLUMN_COUNT)
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
        private const val COLUMN_COUNT = 2
    }
}
