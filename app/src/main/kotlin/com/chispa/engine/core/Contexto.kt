package com.chispa.engine.core

class Contexto(val objetos: List<Objeto>) {
    val variables = LinkedHashMap<String, Float>()
    val azar = java.util.Random()
    var mensaje = ""
    var tiempo = 0f
    var tocando = false
    var toqueX = 0f
    var toqueY = 0f
}
