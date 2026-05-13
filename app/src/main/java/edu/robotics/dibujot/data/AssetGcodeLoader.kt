package edu.robotics.dibujot.data

import android.content.res.AssetManager
import java.io.IOException

class AssetGcodeLoader(private val assetManager: AssetManager) : GcodeLoader {

    @Throws(IOException::class)
    override fun load(assetPath: String): List<String> {
        return assetManager.open(assetPath).bufferedReader().readLines()
    }
}
