package com.example.dibujot.data

import android.content.res.AssetManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.ByteArrayInputStream
import java.io.IOException

class AssetGcodeLoaderTest {

    @Test
    fun `load returns lines from asset`() {
        val content = "G21\nG90\nG0 X0 Y0\n"
        val assetManager = mock(AssetManager::class.java)
        `when`(assetManager.open("gcode/drawing_01.gcode"))
            .thenReturn(ByteArrayInputStream(content.toByteArray()))

        val loader = AssetGcodeLoader(assetManager)
        val lines = loader.load("gcode/drawing_01.gcode")

        assertNotNull(lines)
        assertEquals(3, lines.size)
        assertEquals("G21", lines[0])
        assertEquals("G90", lines[1])
        assertEquals("G0 X0 Y0", lines[2])
    }

    @Test(expected = IOException::class)
    fun `load throws IOException for missing asset`() {
        val assetManager = mock(AssetManager::class.java)
        `when`(assetManager.open("gcode/missing.gcode"))
            .thenThrow(IOException("File not found"))

        val loader = AssetGcodeLoader(assetManager)
        loader.load("gcode/missing.gcode")
    }

    @Test
    fun `load returns all lines from multi-line asset`() {
        val content = "line1\nline2\nline3\nline4\nline5\n"
        val assetManager = mock(AssetManager::class.java)
        `when`(assetManager.open("gcode/drawing_02.gcode"))
            .thenReturn(ByteArrayInputStream(content.toByteArray()))

        val loader = AssetGcodeLoader(assetManager)
        val lines = loader.load("gcode/drawing_02.gcode")

        assertEquals(5, lines.size)
        assertTrue(lines.contains("line3"))
    }
}
