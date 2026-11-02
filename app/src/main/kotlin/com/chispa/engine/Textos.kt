package com.chispa.engine

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.Align
import com.chispa.engine.core.TextoDef

class Textos {

    private val basica = BitmapFont()
    private val propias = HashMap<String, BitmapFont>()
    private val generadores = HashMap<String, FreeTypeFontGenerator>()
    private val fallidas = HashSet<String>()
    private val capa = GlyphLayout()

    init {
        basica.setUseIntegerPositions(false)
        basica.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    }

    private fun fuente(base: FileHandle, nombre: String, tam: Float): BitmapFont {
        if (nombre.isEmpty() || nombre in fallidas) return basica
        val px = maxOf(8, tam.toInt())
        val clave = nombre + "@" + px
        val existente = propias[clave]
        if (existente != null) return existente
        try {
            var gen = generadores[nombre]
            if (gen == null) {
                gen = FreeTypeFontGenerator(base.child("assets/fuentes/" + nombre))
                generadores[nombre] = gen
            }
            val p = FreeTypeFontGenerator.FreeTypeFontParameter()
            p.size = px
            p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "\u00e1\u00e9\u00ed\u00f3\u00fa\u00fc\u00f1\u00c1\u00c9\u00cd\u00d3\u00da\u00dc\u00d1\u00bf\u00a1"
            val f = gen.generateFont(p)
            f.setUseIntegerPositions(false)
            propias[clave] = f
            return f
        } catch (ex: Exception) {
            fallidas.add(nombre)
            return basica
        }
    }

    fun dibujar(batch: SpriteBatch, base: FileHandle, t: TextoDef) {
        val f = fuente(base, t.fuente, t.tam)
        if (f === basica) basica.data.setScale(t.tam / 15f)
        val d = t.tam / 20f
        when (t.estilo) {
            "sombra" -> pasada(batch, f, t, Color(0f, 0f, 0f, 0.7f), d, -d)
            "contorno" -> {
                pasada(batch, f, t, Color.BLACK, d, 0f)
                pasada(batch, f, t, Color.BLACK, -d, 0f)
                pasada(batch, f, t, Color.BLACK, 0f, d)
                pasada(batch, f, t, Color.BLACK, 0f, -d)
            }
        }
        pasada(batch, f, t, t.color, 0f, 0f)
        if (t.estilo == "negrita") pasada(batch, f, t, t.color, d, 0f)
    }

    private fun pasada(batch: SpriteBatch, f: BitmapFont, t: TextoDef, color: Color, dx: Float, dy: Float) {
        val halign = when (t.alin) {
            "izquierda" -> Align.left
            "derecha" -> Align.right
            else -> Align.center
        }
        val conAncho = t.ancho > 0f
        capa.setText(f, t.texto, color, if (conAncho) t.ancho else 0f, halign, conAncho)
        val izq = if (conAncho) t.x - t.ancho / 2f else when (t.alin) {
            "izquierda" -> t.x
            "derecha" -> t.x - capa.width
            else -> t.x - capa.width / 2f
        }
        f.draw(batch, capa, izq + dx, t.y + capa.height / 2f + dy)
    }

    fun liberar() {
        basica.dispose()
        for (f in propias.values) f.dispose()
        for (g in generadores.values) g.dispose()
    }
}
