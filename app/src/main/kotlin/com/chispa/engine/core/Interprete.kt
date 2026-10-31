package com.chispa.engine.core

import com.badlogic.gdx.math.MathUtils
import kotlin.math.abs

class Marco(val bloques: List<Bloque>, var indice: Int, var repeticiones: Int)

class Hilo(
    private val objeto: Objeto,
    val origen: List<Bloque>,
    private val vars: MutableMap<String, Float>
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

    private fun ejecutar(b: Bloque): Boolean {
        when (b.tipo) {
            "mover" -> {
                val r = objeto.rotacion * MathUtils.degreesToRadians
                objeto.x += MathUtils.cos(r) * b.n("pasos")
                objeto.y += MathUtils.sin(r) * b.n("pasos")
            }
            "ir_a" -> {
                objeto.x = b.n("x")
                objeto.y = b.n("y")
            }
            "rotar" -> objeto.rotacion += b.n("grados")
            "esperar" -> {
                espera = b.n("segundos")
                return espera > 0f
            }
            "repetir" -> {
                val veces = b.n("veces").toInt()
                if (veces > 0) pila.add(Marco(b.cuerpo, 0, veces))
            }
            "por_siempre" -> pila.add(Marco(b.cuerpo, 0, -1))
            "asignar" -> vars[b.t("variable")] = b.n("valor")
            "cambiar" -> {
                val k = b.t("variable")
                vars[k] = (vars[k] ?: 0f) + b.n("valor")
            }
        }
        return false
    }
}

class Interprete(private val escena: Escena, inicial: Map<String, Float>) {

    private val hilos = ArrayList<Hilo>()
    private val variables = LinkedHashMap<String, Float>()
    private var acumulador = 0f

    init {
        variables.putAll(inicial)
        for (o in escena.objetos) {
            for (s in o.scripts) {
                if (s.evento == "al_iniciar") hilos.add(Hilo(o, s.bloques, variables))
            }
        }
    }

    fun actualizar(dt: Float) {
        acumulador += dt
        if (acumulador > 0.25f) acumulador = 0.25f
        while (acumulador >= PASO) {
            for (h in hilos) h.avanzar(PASO)
            acumulador -= PASO
        }
        hilos.removeAll { it.terminado }
    }

    companion object {
        const val PASO = 1f / 60f
    }

    fun alTocar(x: Float, y: Float) {
        for (o in escena.objetos) {
            for (s in o.scripts) {
                if (s.evento == "al_tocar_pantalla") lanzar(o, s)
            }
        }
        for (i in escena.objetos.indices.reversed()) {
            val o = escena.objetos[i]
            if (o.visible && contiene(o, x, y)) {
                for (s in o.scripts) {
                    if (s.evento == "al_tocar_objeto") lanzar(o, s)
                }
                break
            }
        }
    }

    private fun lanzar(o: Objeto, s: Script) {
        hilos.removeAll { it.origen === s.bloques }
        hilos.add(Hilo(o, s.bloques, variables))
    }

    private fun contiene(o: Objeto, x: Float, y: Float): Boolean {
        val r = -o.rotacion * MathUtils.degreesToRadians
        val dx = x - o.x
        val dy = y - o.y
        val lx = dx * MathUtils.cos(r) - dy * MathUtils.sin(r)
        val ly = dx * MathUtils.sin(r) + dy * MathUtils.cos(r)
        return abs(lx) <= o.anchoBase * o.escala / 2f && abs(ly) <= o.altoBase * o.escala / 2f
    }

    fun textoVariables(): String {
        val sb = StringBuilder()
        for ((k, v) in variables) {
            if (sb.isNotEmpty()) sb.append("\n")
            sb.append(k).append(": ").append(if (v % 1f == 0f) v.toInt().toString() else v.toString())
        }
        return sb.toString()
    }
}
