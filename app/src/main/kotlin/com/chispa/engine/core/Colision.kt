package com.chispa.engine.core

import com.badlogic.gdx.math.MathUtils
import kotlin.math.abs

object Colision {

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

    fun toca(a: Objeto, b: Objeto): Boolean {
        val p = caja(a)
        val q = caja(b)
        return p[0] < q[2] && p[2] > q[0] && p[1] < q[3] && p[3] > q[1]
    }
}
