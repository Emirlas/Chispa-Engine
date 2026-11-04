package com.chispa.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder

object Axml {

    fun parchear(datos: ByteArray, cambios: Map<String, String>): ByteArray {
        val bb = ByteBuffer.wrap(datos).order(ByteOrder.LITTLE_ENDIAN)
        var pos = bb.getShort(2).toInt() and 0xFFFF
        while (pos + 8 <= datos.size) {
            val tipo = bb.getShort(pos).toInt() and 0xFFFF
            val tam = bb.getInt(pos + 4)
            if (tipo == 0x0001) {
                tabla(datos, bb, pos, cambios)
                return datos
            }
            if (tam <= 0) break
            pos += tam
        }
        throw IllegalStateException("No se encontro la tabla de textos del manifest")
    }

    private fun tabla(datos: ByteArray, bb: ByteBuffer, pos: Int, cambios: Map<String, String>) {
        val cantidad = bb.getInt(pos + 8)
        val flags = bb.getInt(pos + 16)
        val inicio = pos + bb.getInt(pos + 20)
        val indice = pos + (bb.getShort(pos + 2).toInt() and 0xFFFF)
        val utf8 = (flags and 0x100) != 0
        for (i in 0 until cantidad) {
            val off = inicio + bb.getInt(indice + 4 * i)
            if (utf8) {
                var p = off
                val b0 = datos[p].toInt() and 0xFF
                p += if ((b0 and 0x80) != 0) 2 else 1
                val c0 = datos[p].toInt() and 0xFF
                val largo: Int
                if ((c0 and 0x80) != 0) {
                    largo = ((c0 and 0x7F) shl 8) or (datos[p + 1].toInt() and 0xFF)
                    p += 2
                } else {
                    largo = c0
                    p += 1
                }
                val texto = String(datos, p, largo, Charsets.UTF_8)
                val nuevo = cambios[texto]
                if (nuevo != null) {
                    val b = nuevo.toByteArray(Charsets.UTF_8)
                    if (nuevo.length == texto.length && b.size == largo) {
                        for (j in b.indices) datos[p + j] = b[j]
                    }
                }
            } else {
                var largo = bb.getShort(off).toInt() and 0xFFFF
                var p = off + 2
                if ((largo and 0x8000) != 0) {
                    largo = ((largo and 0x7FFF) shl 16) or (bb.getShort(p).toInt() and 0xFFFF)
                    p += 2
                }
                val sb = StringBuilder()
                for (j in 0 until largo) sb.append((bb.getShort(p + 2 * j).toInt() and 0xFFFF).toChar())
                val nuevo = cambios[sb.toString()]
                if (nuevo != null && nuevo.length == largo) {
                    for (j in 0 until largo) bb.putShort(p + 2 * j, nuevo[j].code.toShort())
                }
            }
        }
    }
}
