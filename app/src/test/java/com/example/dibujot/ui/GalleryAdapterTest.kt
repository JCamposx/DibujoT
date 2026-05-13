package com.example.dibujot.ui

import android.os.SystemClock
import com.example.dibujot.data.DrawingItem
import com.example.dibujot.ui.gallery.GalleryAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GalleryAdapterTest {

    private fun makeItems(count: Int): List<DrawingItem> = (1..count).map { i ->
        DrawingItem(id = i, name = "Dibujo $i", imageResId = 0x7f080000 + i, gcodeAssetPath = "gcode/drawing_0$i.gcode")
    }

    @Test
    fun `adapter is non-null after construction`() {
        val adapter = GalleryAdapter(onClick = {})
        assertNotNull(adapter)
    }

    @Test
    fun `getItemCount returns zero before any list submitted`() {
        val adapter = GalleryAdapter(onClick = {})
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemId returns stable id equal to DrawingItem id`() {
        val adapter = GalleryAdapter(onClick = {})
        adapter.setHasStableIds(true)

        val latch = CountDownLatch(1)
        val items = makeItems(3)
        adapter.submitList(items) { latch.countDown() }
        latch.await(2, TimeUnit.SECONDS)

        assertEquals(1L, adapter.getItemId(0))
        assertEquals(2L, adapter.getItemId(1))
        assertEquals(3L, adapter.getItemId(2))
    }

    @Test
    fun `getItemCount returns 8 after submitting 8 items`() {
        val adapter = GalleryAdapter(onClick = {})

        val latch = CountDownLatch(1)
        adapter.submitList(makeItems(8)) { latch.countDown() }
        latch.await(2, TimeUnit.SECONDS)

        assertEquals(8, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns zero after submitting empty list`() {
        val adapter = GalleryAdapter(onClick = {})

        val latch = CountDownLatch(1)
        adapter.submitList(emptyList()) { latch.countDown() }
        latch.await(2, TimeUnit.SECONDS)

        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `single tap fires onClick exactly once`() {
        var clickCount = 0
        val adapter = GalleryAdapter(onClick = { clickCount++ })
        adapter.onItemClicked(makeItems(1).first(), SystemClock.elapsedRealtime())
        assertEquals(1, clickCount)
    }

    @Test
    fun `rapid double-tap within 500ms fires onClick only once`() {
        var clickCount = 0
        val adapter = GalleryAdapter(onClick = { clickCount++ })
        val item = makeItems(1).first()
        val now = SystemClock.elapsedRealtime()
        adapter.onItemClicked(item, now)
        adapter.onItemClicked(item, now + 100L) // 100ms later — within guard window
        assertEquals(1, clickCount)
    }

    @Test
    fun `tap after 500ms guard fires onClick again`() {
        var clickCount = 0
        val adapter = GalleryAdapter(onClick = { clickCount++ })
        val item = makeItems(1).first()
        val now = SystemClock.elapsedRealtime()
        adapter.onItemClicked(item, now)
        adapter.onItemClicked(item, now + 600L) // 600ms later — outside guard window
        assertEquals(2, clickCount)
    }
}

