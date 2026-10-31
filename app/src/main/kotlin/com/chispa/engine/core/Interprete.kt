package com.chispa.engine.core

import com.badlogic.gdx.math.MathUtils
import kotlin.math.abs

class Interprete(private val escena: Escena, inicial: Map<String, Float>) {

    private val ctx = Contexto(escena.objetos)
    private val hilos = ArrayList<Hilo>()
    private var acumulador = 0f
    private var pendiente = false

    init {
        ctx.variables.putAll(inicial)
        for (o in escena.objetos) {
            for (s in o.scripts) {
                if (s.evento == "al_iniciar") hilos.add(Hilo(o, s.bloques, ctx))
            }
        }
    }

    fun actualizar(dt: Float) {
        ctx.tocando = pendiente
        pendiente = false
        ctx.tiempo += dt
        acumulador += dt
        if (acumulador > 0.25f) acumulador = 0.25f
        while (acumulador >= PASO) {
            for (h in hilos) h.avanzar(PASO)
            acumulador -= PASO
        }
        hilos.removeAll { it.terminado }
    }

    fun apoyar(x: Float, y: Float, nuevo: Boolean) {
        pendiente = true
        ctx.toqueX = x
        ctx.toqueY = y
        if (nuevo) alTocar(x, y)
    }

    fun mensaje(): String = ctx.mensaje

    fun sonidos(): MutableList<String> = ctx.sonidos

    companion object {
        const val PASO = 1f / 60f
    }

    private fun alTocar(x: Float, y: Float) {
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
        hilos.add(Hilo(o, s.bloques, ctx))
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
        for ((k, v) in ctx.variables) {
            if (sb.isNotEmpty()) sb.append("\n")
            sb.append(k).append(": ").append(if (v % 1f == 0f) v.toInt().toString() else v.toString())
        }
        return sb.toString()
    }
}
