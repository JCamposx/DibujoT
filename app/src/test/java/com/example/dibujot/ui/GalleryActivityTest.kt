package com.example.dibujot.ui

import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ActivityScenario
import com.example.dibujot.data.DrawingItem
import com.example.dibujot.ui.gallery.GalleryActivity
import com.example.dibujot.ui.gallery.GalleryAdapter
import com.example.dibujot.ui.send.SendActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import androidx.recyclerview.widget.RecyclerView
import com.example.dibujot.R

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GalleryActivityTest {

    @Test
    fun `RecyclerView shows 8 items on launch`() {
        ActivityScenario.launch(GalleryActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val rv = activity.findViewById<RecyclerView>(R.id.rv_gallery)
                val adapter = rv.adapter!!
                // submitList is async; drain the main looper to let AsyncListDiffer complete
                shadowOf(Looper.getMainLooper()).idle()
                assertEquals(8, adapter.itemCount)
            }
        }
    }

    @Test
    fun `clicking item starts SendActivity with DrawingItem extra`() {
        ActivityScenario.launch(GalleryActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val rv = activity.findViewById<RecyclerView>(R.id.rv_gallery)
                val adapter = rv.adapter as GalleryAdapter
                // Drain async list differ
                shadowOf(Looper.getMainLooper()).idle()
                assertEquals(8, adapter.itemCount)

                // Trigger click via adapter's testable entry point
                adapter.simulateClick(0)

                val shadow = shadowOf(activity)
                val nextIntent: Intent? = shadow.nextStartedActivity
                assertNotNull("Expected SendActivity to be started", nextIntent)
                assertEquals(
                    SendActivity::class.java.name,
                    nextIntent!!.component?.className
                )
                @Suppress("DEPRECATION")
                val extra = nextIntent.getParcelableExtra<DrawingItem>(SendActivity.EXTRA_DRAWING_ITEM)
                assertNotNull("Expected DrawingItem extra", extra)
            }
        }
    }
}
