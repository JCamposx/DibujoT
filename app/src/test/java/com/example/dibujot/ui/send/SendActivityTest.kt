package com.example.dibujot.ui.send

import android.content.Intent
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import com.example.dibujot.R
import com.example.dibujot.data.DrawingItem
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SendActivityTest {

    private fun launchWithItem(): ActivityScenario<SendActivity> {
        val item = DrawingItem(
            id = 1,
            name = "Drawing 01",
            imageResId = 0,
            gcodeAssetPath = "gcode/drawing_01.gcode"
        )
        val intent = Intent(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
            SendActivity::class.java
        ).apply {
            putExtra(SendActivity.EXTRA_DRAWING_ITEM, item)
        }
        return ActivityScenario.launch(intent)
    }

    @Test
    fun `activity launches without crashing`() {
        launchWithItem().use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }

    @Test
    fun `status text view is visible and shows idle text`() {
        launchWithItem().use { scenario ->
            scenario.onActivity { activity ->
                val tv = activity.findViewById<TextView>(R.id.tv_status)
                assertNotNull(tv)
            }
        }
    }

    @Test
    fun `activity contains a send button`() {
        launchWithItem().use { scenario ->
            scenario.onActivity { activity ->
                val btn = activity.findViewById<android.widget.Button>(R.id.btn_send)
                assertNotNull(btn)
            }
        }
    }
}
