package com.chispa.engine

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.chispa.engine.core.CargadorProyecto
import com.chispa.engine.core.Escena
import com.chispa.engine.core.Interprete
import com.chispa.engine.core.Objeto
import com.chispa.engine.core.TextoDef
import com.chispa.engine.core.Proyecto

class ChispaGame(private val rutaBase: String, private val interno: Boolean) : ApplicationAdapter() {

    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var batch: SpriteBatch
    private lateinit var blanca: Texture
    private lateinit var fuente: BitmapFont

    private var proyecto: Proyecto? = null
    private var escena: Escena? = null
    private var error: String? = null
    private var interprete: Interprete? = null
    private val toque = Vector3()
    private lateinit var recursos: Recursos
    private lateinit var base: FileHandle
    private lateinit var textos: Textos

    override fun create() {
        batch = SpriteBatch()
        fuente = BitmapFont()
        fuente.data.setScale(2f)

        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        blanca = Texture(pixmap)
        pixmap.dispose()

        base = if (interno) Gdx.files.internal(rutaBase) else Gdx.files.local(rutaBase)
        recursos = Recursos(base)
        textos = Textos()
        var ancho = 1280f
        var alto = 720f
        try {
            val p = CargadorProyecto.cargarProyecto(base.child("proyecto.json"))
            val e = CargadorProyecto.cargarEscena(base.child("escenas/" + p.escenaInicial + ".json"))
            proyecto = p
            escena = e
            interprete = Interprete(e, p.variables)
            ancho = p.ancho
            alto = p.alto
        } catch (ex: Exception) {
            error = ex.toString()
        }

        camera = OrthographicCamera()
        viewport = FitViewport(ancho, alto, camera)
        camera.position.set(0f, 0f, 0f)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, false)
    }

    override fun render() {
        interprete?.actualizar(Gdx.graphics.deltaTime)
        interprete?.let { recursos.reproducir(it.sonidos()) }
        val pedido = interprete?.escenaPendiente()
        if (pedido != null) cambiarEscena(pedido)
        if (Gdx.input.isTouched) {
            toque.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            viewport.unproject(toque)
            interprete?.apoyar(toque.x, toque.y, Gdx.input.justTouched())
        }
        val e = escena
        val p = proyecto
        val msg = error
        val fondo: Color = e?.fondo ?: Color(0.3f, 0.05f, 0.05f, 1f)

        Gdx.gl.glClearColor(fondo.r, fondo.g, fondo.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()

        val medioAncho = viewport.worldWidth / 2f
        val medioAlto = viewport.worldHeight / 2f

        if (msg != null) {
            fuente.setColor(Color.WHITE)
            fuente.draw(
                batch, "Error al cargar el proyecto:\n" + msg,
                -medioAncho + 20f, medioAlto - 20f,
                viewport.worldWidth - 40f, Align.left, true
            )
        } else if (e != null && p != null) {
            for (o in e.objetos) {
                if (!o.visible) continue
                if (o.texto != null) {
                    dibujarTextoObjeto(o)
                    continue
                }
                val tx = recursos.textura(o.imagen)
                batch.setColor(if (tx == null) o.color else Color.WHITE)
                batch.draw(
                    tx ?: blanca,
                    o.x - o.anchoBase / 2f, o.y - o.altoBase / 2f,
                    o.anchoBase / 2f, o.altoBase / 2f,
                    o.anchoBase, o.altoBase,
                    o.escala, o.escala,
                    o.rotacion,
                    0, 0, tx?.width ?: 1, tx?.height ?: 1,
                    false, false
                )
            }
            batch.setColor(Color.WHITE)

            val visibles = e.objetos.count { it.visible }
            fuente.setColor(Color.WHITE)
            fuente.draw(
                batch,
                if (!p.mostrarDebug) "" else p.nombre + " - " + e.nombre + "\nObjetos: " + e.objetos.size + " (" + visibles + " visibles)",
                -medioAncho + 20f, medioAlto - 20f
            )
        }

        interprete?.let { ip ->
            for (td in ip.textos()) textos.dibujar(batch, base, td)
        }
        val tv = if (proyecto?.mostrarVariables == false) null else interprete?.textoVariables()
        if (!tv.isNullOrEmpty()) fuente.draw(batch, tv, medioAncho - 420f, medioAlto - 20f, 400f, Align.right, false)
        batch.end()
    }

    override fun dispose() {
        batch.dispose()
        blanca.dispose()
        fuente.dispose()
        recursos.liberar()
        textos.liberar()
    }

    private fun cambiarEscena(nombre: String) {
        val ip = interprete ?: return
        try {
            val nueva = CargadorProyecto.cargarEscena(base.child("escenas/" + nombre + ".json"))
            escena = nueva
            interprete = Interprete(nueva, ip.variablesActuales())
        } catch (ex: Exception) {
            error = "No se pudo abrir la escena " + nombre + ": " + ex.toString()
            interprete = null
        }
    }

    private fun dibujarTextoObjeto(o: Objeto) {
        val original = o.texto ?: ""
        val t = interprete?.reemplazar(original) ?: original
        textos.dibujar(batch, base, TextoDef(t, o.x, o.y, o.color, o.tamTexto * o.escala, o.alinTexto, o.estiloTexto, o.fuenteTexto, 0f))
    }
}
