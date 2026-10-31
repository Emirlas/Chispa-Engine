package com.chispa.engine.core

import com.badlogic.gdx.utils.JsonValue

object LectorBloques {

    fun leerVariables(lista: JsonValue?): Map<String, Float> {
        val r = LinkedHashMap<String, Float>()
        var v: JsonValue? = lista?.child
        while (v != null) {
            r[v.getString("nombre", "")] = v.getFloat("valor", 0f)
            v = v.next
        }
        return r
    }

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
        val txt = HashMap<String, String>()
        val sub = HashMap<String, Bloque>()
        var c: JsonValue? = j.child
        while (c != null) {
            val nombre = c.name()
            if (c.isNumber()) {
                num[nombre] = c.asFloat()
            } else if (c.isString() && nombre != "tipo") {
                txt[nombre] = c.asString()
            } else if (c.isObject()) {
                sub[nombre] = leerBloque(c)
            }
            c = c.next
        }
        val tipo = j.getString("tipo", "")
        return Bloque(tipo, num, txt, sub, leerBloques(j.get("cuerpo")), leerBloques(j.get("sino")))
    }
}
