package com.chispa.engine.core

import com.badlogic.gdx.math.MathUtils
import kotlin.math.abs

class Marco(val bloques: List<Bloque>, var indice: Int, var repeticiones: Int)

class Hilo(
    private val objeto: Objeto,
    val origen: List<Bloque>,
    private val ctx: Contexto
) {

    private val pila = ArrayList<Marco>()
    private var espera = 0f
    var terminado = false
        private set

    init {
        pila.add(Marco(origen, 0, 1))
    }

    fun avanzar(dt: Float) {
        if (terminado) return
        if (espera > 0f) {
            espera -= dt
            if (espera > 0f) return
        }
        var limite = 1000
        while (limite > 0) {
            limite--
            if (pila.isEmpty()) {
                terminado = true
                return
            }
            val m = pila[pila.size - 1]
            if (m.indice >= m.bloques.size) {
                if (m.repeticiones < 0) {
                    m.indice = 0
                    return
                }
                m.repeticiones--
                if (m.repeticiones > 0) {
                    m.indice = 0
                    return
                }
                pila.removeAt(pila.size - 1)
                continue
            }
            val b = m.bloques[m.indice]
            m.indice++
            if (ejecutar(b)) return
        }
    }

    private fun valor(b: Bloque, nombre: String): Float {
        val e = b.sub[nombre]
        return if (e != null) calc(e) else b.n(nombre)
    }

    private fun ejecutar(b: Bloque): Boolean {
        when (b.tipo) {
            "mover" -> {
                val r = objeto.rotacion * MathUtils.degreesToRadians
                val pasos = valor(b, "pasos")
                objeto.x += MathUtils.cos(r) * pasos
                objeto.y += MathUtils.sin(r) * pasos
            }
            "ir_a" -> {
                objeto.x = valor(b, "x")
                objeto.y = valor(b, "y")
            }
            "rotar" -> objeto.rotacion += valor(b, "grados")
            "esperar" -> {
                espera = valor(b, "segundos")
                return espera > 0f
            }
            "repetir" -> {
                val veces = valor(b, "veces").toInt()
                if (veces > 0) pila.add(Marco(b.cuerpo, 0, veces))
            }
            "por_siempre" -> pila.add(Marco(b.cuerpo, 0, -1))
            "asignar" -> ctx.variables[b.t("variable")] = valor(b, "valor")
            "cambiar" -> {
                val k = b.t("variable")
                ctx.variables[k] = (ctx.variables[k] ?: 0f) + valor(b, "valor")
            }
            "si" -> {
                val rama = if (valor(b, "condicion") != 0f) b.cuerpo else b.sino
                if (rama.isNotEmpty()) pila.add(Marco(rama, 0, 1))
            }
            "esperar_hasta" -> {
                if (valor(b, "condicion") == 0f) {
                    pila[pila.size - 1].indice--
                    return true
                }
            }
            "mostrar_texto" -> ctx.mensaje = b.t("texto")
            "borrar_texto" -> ctx.mensaje = ""
            "detener_script" -> pila.clear()
        }
        return false
    }

    private fun bool(c: Boolean): Float = if (c) 1f else 0f

    private fun calc(e: Bloque): Float {
        return when (e.tipo) {
            "var" -> ctx.variables[e.t("nombre")] ?: 0f
            "suma" -> valor(e, "a") + valor(e, "b")
            "resta" -> valor(e, "a") - valor(e, "b")
            "mult" -> valor(e, "a") * valor(e, "b")
            "div" -> {
                val d = valor(e, "b")
                if (d == 0f) 0f else valor(e, "a") / d
            }
            "mayor" -> bool(valor(e, "a") > valor(e, "b"))
            "menor" -> bool(valor(e, "a") < valor(e, "b"))
            "igual" -> bool(valor(e, "a") == valor(e, "b"))
            "y" -> bool(valor(e, "a") != 0f && valor(e, "b") != 0f)
            "o" -> bool(valor(e, "a") != 0f || valor(e, "b") != 0f)
            "no" -> bool(valor(e, "a") == 0f)
            "azar" -> {
                val lo = valor(e, "min")
                val hi = valor(e, "max")
                lo + ctx.azar.nextFloat() * (hi - lo)
            }
            "tocando_pantalla" -> bool(ctx.tocando)
            "toque_x" -> ctx.toqueX
            "toque_y" -> ctx.toqueY
            "temporizador" -> ctx.tiempo
            "tocando_objeto" -> bool(tocaA(e.t("objeto")))
            else -> 0f
        }
    }

    private fun caja(o: Objeto): FloatArray {
        val r = o.rotacion * MathUtils.degreesToRadians
        val c = abs(MathUtils.cos(r))
        val s = abs(MathUtils.sin(r))
        val hw = o.anchoBase * o.escala / 2f
        val hh = o.altoBase * o.escala / 2f
        val ex = hw * c + hh * s
        val ey = hw * s + hh * c
        return floatArrayOf(o.x - ex, o.y - ey, o.x + ex, o.y + ey)
    }

    private fun tocaA(nombre: String): Boolean {
        val a = caja(objeto)
        for (o in ctx.objetos) {
            if (o !== objeto && o.visible && o.nombre == nombre) {
                val b = caja(o)
                if (a[0] < b[2] && a[2] > b[0] && a[1] < b[3] && a[3] > b[1]) return true
            }
        }
        return false
    }
}
