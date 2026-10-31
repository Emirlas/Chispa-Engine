package com.chispa.engine.core

import com.badlogic.gdx.utils.JsonValue

object LectorBloques {

    fun leerScripts(lista: JsonValue?): List<Script> {
        val r = ArrayList<Script>()
        var s: JsonValue? = lista?.child
        while (s != null) {
            val evento = s.get("evento")?.getString("tipo", "") ?: ""
            r.add(Script(evento, leerBloques(s.get("bloques"))))
            s = s.next
        }
        return r
    }

    private fun leerBloques(lista: JsonValue?): List<Bloque> {
        val r = ArrayList<Bloque>()
        var b: JsonValue? = lista?.child
        while (b != null) {
            r.add(leerBloque(b))
            b = b.next
        }
        return r
    }

    private fun leerBloque(j: JsonValue): Bloque {
        val num = HashMap<String, Float>()
        var c: JsonValue? = j.child
        while (c != null) {
            if (c.isNumber()) num[c.name()] = c.asFloat()
            c = c.next
        }
        return Bloque(j.getString("tipo", ""), num, leerBloques(j.get("cuerpo")))
    }
}
