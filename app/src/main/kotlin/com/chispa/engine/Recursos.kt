package com.chispa.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture

class Recursos(private val base: FileHandle) {

    private val texturas = HashMap<String, Texture>()
    private val sonidos = HashMap<String, Sound>()
    private val fallidos = HashSet<String>()

    fun textura(nombre: String?): Texture? {
        if (nombre == null || nombre in fallidos) return null
        var t = texturas[nombre]
        if (t == null) {
            try {
                t = Texture(base.child("assets/imagenes/" + nombre))
                t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
                texturas[nombre] = t
            } catch (ex: Exception) {
                fallidos.add(nombre)
                return null
            }
        }
        return t
    }

    fun reproducir(lista: MutableList<String>) {
        for (n in lista) {
            if (n in fallidos) continue
            var s = sonidos[n]
            if (s == null) {
                try {
                    s = Gdx.audio.newSound(base.child("assets/sonidos/" + n))
                    sonidos[n] = s
                } catch (ex: Exception) {
                    fallidos.add(n)
                }
            }
            s?.play()
        }
        lista.clear()
    }

    fun liberar() {
        for (t in texturas.values) t.dispose()
        for (s in sonidos.values) s.dispose()
    }
}
