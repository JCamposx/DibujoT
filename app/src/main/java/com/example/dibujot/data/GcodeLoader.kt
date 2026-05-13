package com.example.dibujot.data

interface GcodeLoader {
    fun load(assetPath: String): List<String>
}
