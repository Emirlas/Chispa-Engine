package com.chispa.engine

import android.content.pm.ActivityInfo
import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import org.json.JSONObject
import java.io.File

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nombre = intent.getStringExtra("proyecto") ?: "Demo"
        val interno = intent.getBooleanExtra("interno", false)
        val vertical = esVertical(nombre, interno)
        requestedOrientation = if (vertical) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val config = AndroidApplicationConfiguration()
        config.useImmersiveMode = true
        config.useAccelerometer = false
        config.useCompass = false
        initialize(ChispaGame(if (interno) "juego" else "proyectos/" + nombre, interno), config)
    }

    private fun esVertical(nombre: String, interno: Boolean): Boolean {
        return try {
            val texto = if (interno) {
                assets.open("juego/proyecto.json").bufferedReader().use { it.readText() }
            } else {
                File(filesDir, "proyectos/" + nombre + "/proyecto.json").readText()
            }
            JSONObject(texto).optJSONObject("pantalla")?.optString("orientacion", "horizontal") == "vertical"
        } catch (ex: Exception) {
            false
        }
    }
}
