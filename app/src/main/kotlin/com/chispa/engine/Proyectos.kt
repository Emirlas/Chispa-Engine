package com.chispa.engine

import android.content.Context
import java.io.File

object Proyectos {

    private fun raiz(ctx: Context): File {
        val d = File(ctx.filesDir, "proyectos")
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun listar(ctx: Context): List<String> {
        val hijos = raiz(ctx).listFiles() ?: return emptyList()
        return hijos.filter { it.isDirectory }.map { it.name }.sorted()
    }

    fun borrar(ctx: Context, nombre: String) {
        File(raiz(ctx), nombre).deleteRecursively()
    }

    fun prepararDemo(ctx: Context) {
        val destino = File(raiz(ctx), "Demo")
        if (destino.exists()) return
        copiar(ctx, "proyecto", destino)
    }

    private fun copiar(ctx: Context, origen: String, destino: File) {
        val hijos = ctx.assets.list(origen)
        if (hijos == null || hijos.isEmpty()) {
            try {
                destino.parentFile?.mkdirs()
                ctx.assets.open(origen).use { entrada ->
                    destino.outputStream().use { salida -> entrada.copyTo(salida) }
                }
            } catch (ex: Exception) {
                destino.mkdirs()
            }
        } else {
            destino.mkdirs()
            for (h in hijos) copiar(ctx, origen + "/" + h, File(destino, h))
        }
    }

    fun crear(ctx: Context, nombreUsuario: String): String {
        var base = nombreUsuario.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim()
        if (base.isEmpty()) base = "Proyecto"
        var nombre = base
        var n = 2
        while (File(raiz(ctx), nombre).exists()) {
            nombre = base + " " + n
            n++
        }
        val dir = File(raiz(ctx), nombre)
        File(dir, "escenas").mkdirs()
        File(dir, "assets/imagenes").mkdirs()
        File(dir, "assets/sonidos").mkdirs()
        File(dir, "proyecto.json").writeText(
            "{\"formato\":1,\"tipo\":\"2D\",\"nombre\":\"" + nombre + "\"," +
            "\"idPaquete\":\"com.chispa.juego\",\"version\":\"1.0\",\"motor\":\"0.1.0\"," +
            "\"pantalla\":{\"ancho\":1280,\"alto\":720,\"orientacion\":\"horizontal\"}," +
            "\"escenaInicial\":\"escena_1\",\"variablesGlobales\":[]}"
        )
        File(dir, "escenas/escena_1.json").writeText(
            "{\"id\":\"escena_1\",\"nombre\":\"Escena 1\",\"fondo\":{\"color\":\"#1B2A41\"},\"objetos\":[" +
            "{\"id\":\"obj_001\",\"nombre\":\"Objeto 1\",\"color\":\"#FF8C1A\"," +
            "\"tamano\":{\"ancho\":120,\"alto\":120},\"x\":0,\"y\":0,\"escala\":1.0," +
            "\"rotacion\":0,\"visible\":true,\"capa\":1,\"scripts\":[]}]}"
        )
        return nombre
    }
}
