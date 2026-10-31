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

class Bloque(val tipo: String, val num: Map<String, Float>, val cuerpo: List<Bloque>) {
    fun n(nombre: String): Float = num[nombre] ?: 0f
}

class Script(val evento: String, val bloques: List<Bloque>)

class Objeto(
    val id: String,
    val nombre: String,
    val color: Color,
    val anchoBase: Float,
    val altoBase: Float,
    var x: Float,
    var y: Float,
    val escala: Float,
    var rotacion: Float,
    val visible: Boolean,
    val capa: Int,
    val scripts: List<Script>
)

class Escena(
    val id: String,
    val nombre: String,
    val fondo: Color,
    val objetos: List<Objeto>
)
