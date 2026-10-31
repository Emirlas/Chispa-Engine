package com.chispa.engine.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue

object CargadorProyecto {

    private val lector = JsonReader()

    fun cargarProyecto(ruta: String): Proyecto {
        val raiz: JsonValue = lector.parse(Gdx.files.internal(ruta))
        val pantalla: JsonValue? = raiz.get("pantalla")
        return Proyecto(
            formato = raiz.getInt("formato", 1),
            tipo = raiz.getString("tipo", "2D"),
            nombre = raiz.getString("nombre", "Sin nombre"),
            ancho = pantalla?.getFloat("ancho", 1280f) ?: 1280f,
            alto = pantalla?.getFloat("alto", 720f) ?: 720f,
            orientacion = pantalla?.getString("orientacion", "horizontal") ?: "horizontal",
            escenaInicial = raiz.getString("escenaInicial", "escena_1"),
            variables = LectorBloques.leerVariables(raiz.get("variablesGlobales"))
        )
    }

    fun cargarEscena(ruta: String): Escena {
        val raiz: JsonValue = lector.parse(Gdx.files.internal(ruta))
        val fondo: JsonValue? = raiz.get("fondo")
        val colorFondo = Color.valueOf(fondo?.getString("color", "#000000") ?: "#000000")

        val objetos = ArrayList<Objeto>()
        val lista: JsonValue? = raiz.get("objetos")
        if (lista != null) {
            var o: JsonValue? = lista.child
            while (o != null) {
                objetos.add(leerObjeto(o))
                o = o.next
            }
        }
        objetos.sortBy { it.capa }

        return Escena(
            id = raiz.getString("id", "escena"),
            nombre = raiz.getString("nombre", "Escena"),
            fondo = colorFondo,
            objetos = objetos
        )
    }

    private fun leerObjeto(o: JsonValue): Objeto {
        val tamano: JsonValue? = o.get("tamano")
        return Objeto(
            id = o.getString("id", "obj"),
            nombre = o.getString("nombre", "Objeto"),
            color = Color.valueOf(o.getString("color", "#FFFFFF")),
            anchoBase = tamano?.getFloat("ancho", 64f) ?: 64f,
            altoBase = tamano?.getFloat("alto", 64f) ?: 64f,
            x = o.getFloat("x", 0f),
            y = o.getFloat("y", 0f),
            escala = o.getFloat("escala", 1f),
            rotacion = o.getFloat("rotacion", 0f),
            visible = o.getBoolean("visible", true),
            capa = o.getInt("capa", 0),
            scripts = LectorBloques.leerScripts(o.get("scripts"))
        )
    }
}
