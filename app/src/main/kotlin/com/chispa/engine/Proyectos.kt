package com.chispa.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import org.json.JSONObject
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
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
            "\"escenaInicial\":\"escena_1\",\"variablesGlobales\":[],\"mostrarVariables\":false,\"mostrarDebug\":false}"
        )
        File(dir, "escenas/escena_1.json").writeText(
            "{\"id\":\"escena_1\",\"nombre\":\"Escena 1\",\"fondo\":{\"color\":\"#1B2A41\"},\"objetos\":[" +
            "{\"id\":\"obj_001\",\"nombre\":\"Objeto 1\",\"color\":\"#FF8C1A\"," +
            "\"tamano\":{\"ancho\":120,\"alto\":120},\"x\":0,\"y\":0,\"escala\":1.0," +
            "\"rotacion\":0,\"visible\":true,\"capa\":1,\"scripts\":[]}]}"
        )
        return nombre
    }

    private fun nombreLibre(ctx: Context, base: String): String {
        var n = base
        var i = 2
        while (File(raiz(ctx), n).exists()) {
            n = base + " " + i
            i++
        }
        return n
    }

    fun duplicar(ctx: Context, nombre: String): String {
        val destino = nombreLibre(ctx, nombre + " copia")
        File(raiz(ctx), nombre).copyRecursively(File(raiz(ctx), destino), true)
        return destino
    }

    fun crearDesdePlantilla(ctx: Context, nombreUsuario: String, plantilla: String): String {
        var base = nombreUsuario.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim()
        if (base.isEmpty()) base = "Proyecto"
        val nombre = nombreLibre(ctx, base)
        val dir = File(raiz(ctx), nombre)
        copiar(ctx, "plantillas/" + plantilla, dir)
        val f = File(dir, "proyecto.json")
        val j = JSONObject(f.readText())
        j.put("nombre", nombre)
        f.writeText(j.toString(2))
        return nombre
    }

    fun exportarZip(ctx: Context, nombre: String): Uri? {
        if (Build.VERSION.SDK_INT < 29) return null
        val origen = File(raiz(ctx), nombre)
        val v = ContentValues()
        v.put(MediaStore.Downloads.DISPLAY_NAME, nombre + ".chispa")
        v.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
        v.put(MediaStore.Downloads.IS_PENDING, 1)
        val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: return null
        ctx.contentResolver.openOutputStream(uri)?.use { s ->
            val z = ZipOutputStream(s)
            agregarZip(z, origen, origen)
            z.finish()
        }
        v.clear()
        v.put(MediaStore.Downloads.IS_PENDING, 0)
        ctx.contentResolver.update(uri, v, null, null)
        return uri
    }

    private fun agregarZip(z: ZipOutputStream, raiz: File, actual: File) {
        val hijos = actual.listFiles() ?: return
        for (f in hijos) {
            if (f.isDirectory) {
                agregarZip(z, raiz, f)
            } else {
                z.putNextEntry(ZipEntry(f.relativeTo(raiz).path.replace(File.separatorChar, '/')))
                z.write(f.readBytes())
                z.closeEntry()
            }
        }
    }

    fun importarZip(ctx: Context, uri: Uri): String {
        var base = "Importado"
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (col >= 0 && c.moveToFirst()) base = c.getString(col).substringBeforeLast('.').removeSuffix(".chispa")
        }
        base = base.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim()
        if (base.isEmpty()) base = "Importado"
        val nombre = nombreLibre(ctx, base)
        val dir = File(raiz(ctx), nombre)
        dir.mkdirs()
        try {
            ctx.contentResolver.openInputStream(uri)?.use { entrada ->
                val z = ZipInputStream(entrada)
                var e = z.nextEntry
                while (e != null) {
                    val destino = File(dir, e.name)
                    if (!destino.canonicalPath.startsWith(dir.canonicalPath + File.separator)) {
                        throw IllegalStateException("Archivo no valido")
                    }
                    if (e.isDirectory) {
                        destino.mkdirs()
                    } else {
                        destino.parentFile?.mkdirs()
                        destino.outputStream().use { s -> z.copyTo(s) }
                    }
                    e = z.nextEntry
                }
            }
            if (!File(dir, "proyecto.json").exists()) throw IllegalStateException("No es un proyecto de Chispa Engine")
        } catch (ex: Exception) {
            dir.deleteRecursively()
            throw ex
        }
        return nombre
    }
}
