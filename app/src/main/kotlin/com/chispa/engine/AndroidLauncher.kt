package com.chispa.engine

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration()
        config.useImmersiveMode = true
        config.useAccelerometer = false
        config.useCompass = false
        val nombre = intent.getStringExtra("proyecto") ?: "Demo"
        val interno = intent.getBooleanExtra("interno", false)
        initialize(ChispaGame(if (interno) "juego" else "proyectos/" + nombre, interno), config)
    }
}
