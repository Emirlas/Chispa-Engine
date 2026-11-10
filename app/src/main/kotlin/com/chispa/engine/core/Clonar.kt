package com.chispa.engine.core

fun Objeto.clonar(): Objeto {
    val c = Objeto(
        id = id + "_c" + System.nanoTime(),
        nombre = nombre,
        color = color,
        imagen = imagen,
        disfraces = disfraces,
        disfraz = disfraz,
        texto = texto,
        tamTexto = tamTexto,
        alinTexto = alinTexto,
        estiloTexto = estiloTexto,
        fuenteTexto = fuenteTexto,
        anchoBase = anchoBase,
        altoBase = altoBase,
        x = x,
        y = y,
        escala = escala,
        rotacion = rotacion,
        visible = visible,
        capa = capa,
        scripts = scripts
    )
    c.esClon = true
    return c
}
