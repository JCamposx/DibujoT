package com.example.dibujot.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class DrawingRepositoryTest {

    @Test
    fun `getAll returns exactly 8 items`() {
        val items = DrawingRepository.getAll()
        assertEquals(8, items.size)
    }

    @Test
    fun `all items have non-null non-empty gcodeAssetPath`() {
        val items = DrawingRepository.getAll()
        items.forEach { item ->
            assertNotNull(item.gcodeAssetPath)
            assertFalse("Item ${item.id} has empty gcodeAssetPath", item.gcodeAssetPath.isEmpty())
        }
    }

    @Test
    fun `all items have non-empty name`() {
        val items = DrawingRepository.getAll()
        items.forEach { item ->
            assertFalse("Item ${item.id} has empty name", item.name.isEmpty())
        }
    }

    @Test
    fun `first item is named Espiral`() {
        val items = DrawingRepository.getAll()
        assertEquals("Espiral", items[0].name)
    }

    @Test
    fun `all items have unique ids`() {
        val items = DrawingRepository.getAll()
        val ids = items.map { it.id }.toSet()
        assertEquals(8, ids.size)
    }

    @Test
    fun `gcodeAssetPaths follow expected naming pattern`() {
        val items = DrawingRepository.getAll()
        items.forEach { item ->
            assert(item.gcodeAssetPath.startsWith("gcode/")) {
                "Item ${item.id} path '${item.gcodeAssetPath}' must start with 'gcode/'"
            }
            assert(item.gcodeAssetPath.endsWith(".gcode")) {
                "Item ${item.id} path '${item.gcodeAssetPath}' must end with '.gcode'"
            }
        }
    }

    @Test
    fun `getAll returns different items on successive calls`() {
        val first = DrawingRepository.getAll()
        val second = DrawingRepository.getAll()
        assertEquals(first.size, second.size)
        first.forEachIndexed { index, item ->
            assertEquals(item.id, second[index].id)
            assertEquals(item.gcodeAssetPath, second[index].gcodeAssetPath)
        }
    }
}
