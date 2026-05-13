package com.example.dibujot.data

import com.example.dibujot.R

object DrawingRepository {

    fun getAll(): List<DrawingItem> = ALL_ITEMS

    // Visible for testing: can be overridden in tests via reflection or via the
    // factory function below.
    internal val ALL_ITEMS: List<DrawingItem> = listOf(
        DrawingItem(id = 1, name = "Espiral",   imageResId = R.drawable.drawing_spiral,   gcodeAssetPath = "gcode/drawing_spiral.gcode"),
        DrawingItem(id = 2, name = "Estrella",  imageResId = R.drawable.drawing_star,     gcodeAssetPath = "gcode/drawing_star.gcode"),
        DrawingItem(id = 3, name = "Círculo",   imageResId = R.drawable.drawing_circle,   gcodeAssetPath = "gcode/drawing_circle.gcode"),
        DrawingItem(id = 4, name = "Cuadrado",  imageResId = R.drawable.drawing_square,   gcodeAssetPath = "gcode/drawing_square.gcode"),
        DrawingItem(id = 5, name = "Triángulo", imageResId = R.drawable.drawing_triangle, gcodeAssetPath = "gcode/drawing_triangle.gcode"),
        DrawingItem(id = 6, name = "Hexágono",  imageResId = R.drawable.drawing_hexagon,  gcodeAssetPath = "gcode/drawing_hexagon.gcode"),
        DrawingItem(id = 7, name = "Rombo",     imageResId = R.drawable.drawing_rhombus,  gcodeAssetPath = "gcode/drawing_rhombus.gcode"),
        DrawingItem(id = 8, name = "Flor",      imageResId = R.drawable.drawing_flower,   gcodeAssetPath = "gcode/drawing_flower.gcode")
    )
}
