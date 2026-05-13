package edu.robotics.dibujot.data

interface GcodeLoader {
    fun load(assetPath: String): List<String>
}
