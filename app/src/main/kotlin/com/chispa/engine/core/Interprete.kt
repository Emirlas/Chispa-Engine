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
        despachar()
    }

    fun apoyar(x: Float, y: Float, nuevo: Boolean) {
        pendiente = true
        ctx.toqueX = x
        ctx.toqueY = y
        if (nuevo) alTocar(x, y)
    }

    fun reemplazar(texto: String): String {
        var t = texto
        for ((k, v) in ctx.variables) {
            t = t.replace("{" + k + "}", if (v % 1f == 0f) v.toInt().toString() else v.toString())
        }
        return t
    }

    fun textos(): List<TextoDef> {
        val r = ArrayList<TextoDef>()
        for (t in ctx.textos.values) r.add(t.copy(texto = reemplazar(t.texto)))
        return r
    }

    fun sonidos(): MutableList<String> = ctx.sonidos

    fun escenaPendiente(): String? = ctx.escenaPendiente

    fun variablesActuales(): Map<String, Float> = ctx.variables

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
        hilos.removeAll { it.origen === s.bloques && it.objeto === o }
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

    private val colisionando = HashMap<String, Boolean>()

    private fun despachar() {
        if (escena.objetos.any { it.eliminado }) {
            escena.objetos.removeAll { it.eliminado }
            hilos.removeAll { it.objeto.eliminado }
        }
        if (ctx.clones.isNotEmpty()) {
            val fuentes = ArrayList(ctx.clones)
            ctx.clones.clear()
            for (f in fuentes) {
                if (escena.objetos.size >= 400) break
                val c = f.clonar()
                escena.objetos.add(c)
                for (s in c.scripts) {
                    if (s.evento == "al_empezar_clon") hilos.add(Hilo(c, s.bloques, ctx))
                }
            }
            escena.objetos.sortBy { it.capa }
        }
        if (ctx.mensajes.isNotEmpty()) {
            val enviados = ArrayList(ctx.mensajes)
            ctx.mensajes.clear()
            for (m in enviados) {
                for (o in escena.objetos) {
                    for (s in o.scripts) {
                        if (s.evento == "al_recibir_mensaje" && s.param == m) lanzar(o, s)
                    }
                }
            }
        }
        for (o in escena.objetos) {
            for (s in o.scripts) {
                if (s.evento != "al_colisionar") continue
                var ahora = false
                for (otro in escena.objetos) {
                    if (otro !== o && o.visible && otro.visible && otro.nombre == s.param && Colision.toca(o, otro)) ahora = true
                }
                val clave = System.identityHashCode(s).toString() + "|" + System.identityHashCode(o)
                val antes = colisionando[clave] ?: false
                colisionando[clave] = ahora
                if (ahora && !antes) lanzar(o, s)
            }
        }
    }

    fun definirPantalla(a: Float, h: Float) {
        ctx.ancho = a
        ctx.alto = h
    }
}
