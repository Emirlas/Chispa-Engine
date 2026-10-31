package com.chispa.engine.core

import com.badlogic.gdx.math.MathUtils

class Marco(val bloques: List<Bloque>, var indice: Int, var repeticiones: Int)

class Hilo(private val objeto: Objeto, bloques: List<Bloque>) {

    private val pila = ArrayList<Marco>()
    private var espera = 0f
    var terminado = false
        private set

    init {
        pila.add(Marco(bloques, 0, 1))
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
        }
        return false
    }
}

class Interprete(escena: Escena) {

    private val hilos = ArrayList<Hilo>()
    private var acumulador = 0f

    init {
        for (o in escena.objetos) {
            for (s in o.scripts) {
                if (s.evento == "al_iniciar") hilos.add(Hilo(o, s.bloques))
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
    }

    companion object {
        const val PASO = 1f / 60f
    }
}
