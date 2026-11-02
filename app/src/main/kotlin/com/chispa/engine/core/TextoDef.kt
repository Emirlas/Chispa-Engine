package com.chispa.engine.core

import com.badlogic.gdx.graphics.Color

data class TextoDef(
    val texto: String,
    val x: Float,
    val y: Float,
    val color: Color,
    val tam: Float,
    val alin: String,
    val estilo: String,
    val fuente: String,
    val ancho: Float
)
