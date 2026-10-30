package com.chispa.engine.core

import com.badlogic.gdx.graphics.Color

class Proyecto(
    val formato: Int,
    val tipo: String,
    val nombre: String,
    val ancho: Float,
    val alto: Float,
    val orientacion: String,
    val escenaInicial: String
)

class Objeto(
    val id: String,
    val nombre: String,
    val color: Color,
    val anchoBase: Float,
    val altoBase: Float,
    val x: Float,
    val y: Float,
    val escala: Float,
    val rotacion: Float,
    val visible: Boolean,
    val capa: Int
)

class Escena(
    val id: String,
    val nombre: String,
    val fondo: Color,
    val objetos: List<Objeto>
)
