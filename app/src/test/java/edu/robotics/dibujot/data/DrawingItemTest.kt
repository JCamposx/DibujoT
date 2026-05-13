package edu.robotics.dibujot.data

import android.os.Parcel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DrawingItemTest {

    @Test
    fun `parcelable round-trip preserves all fields`() {
        val original = DrawingItem(
            id = 3,
            name = "Espiral",
            imageResId = 0x7f080001,
            gcodeAssetPath = "gcode/drawing_03.gcode"
        )

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = DrawingItem.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.imageResId, restored.imageResId)
        assertEquals(original.gcodeAssetPath, restored.gcodeAssetPath)
    }

    @Test
    fun `parcelable round-trip with different item preserves all fields`() {
        val original = DrawingItem(
            id = 7,
            name = "Estrella",
            imageResId = 0x7f080005,
            gcodeAssetPath = "gcode/drawing_07.gcode"
        )

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = DrawingItem.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        assertEquals(7, restored.id)
        assertEquals("Estrella", restored.name)
        assertEquals(0x7f080005, restored.imageResId)
        assertEquals("gcode/drawing_07.gcode", restored.gcodeAssetPath)
    }
}
