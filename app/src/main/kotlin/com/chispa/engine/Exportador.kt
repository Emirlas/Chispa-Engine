package com.chispa.engine

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.android.apksig.ApkSigner
import java.io.File
import java.io.OutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.text.Normalizer
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.security.auth.x500.X500Principal

private class Contador(private val o: OutputStream) : OutputStream() {
    var n = 0L

    override fun write(b: Int) {
        o.write(b)
        n++
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        o.write(b, off, len)
        n += len
    }

    override fun flush() {
        o.flush()
    }

    override fun close() {
        o.close()
    }
}

private class Escritor(salida: OutputStream) {
    private val contador = Contador(salida)
    private val zip = ZipOutputStream(contador)

    private fun relleno(posicion: Long, largoNombre: Int, alin: Int): ByteArray {
        val inicio = posicion + 30 + largoNombre
        var pad = ((alin - (inicio % alin)) % alin).toInt()
        if (pad == 0) return ByteArray(0)
        while (pad < 6) pad += alin
        val extra = ByteArray(pad)
        extra[0] = 0x35.toByte()
        extra[1] = 0xD9.toByte()
        val tam = pad - 4
        extra[2] = (tam and 0xFF).toByte()
        extra[3] = ((tam shr 8) and 0xFF).toByte()
        return extra
    }

    fun agregar(nombre: String, datos: ByteArray, almacenar: Boolean, alin: Int) {
        zip.closeEntry()
        val e = ZipEntry(nombre)
        if (almacenar) {
            e.method = ZipEntry.STORED
            e.size = datos.size.toLong()
            e.compressedSize = datos.size.toLong()
            val crc = CRC32()
            crc.update(datos)
            e.crc = crc.value
            if (alin > 1) e.extra = relleno(contador.n, nombre.toByteArray(Charsets.UTF_8).size, alin)
        } else {
            e.method = ZipEntry.DEFLATED
        }
        zip.putNextEntry(e)
        zip.write(datos)
    }

    fun cerrar() {
        zip.finish()
        zip.close()
    }
}

object Exportador {

    private const val PAQUETE_BASE = "com.chispa.engine"
    private val ETIQUETA_BASE = "Chispa Engine".padEnd(32)

    fun exportar(ctx: Context, proyecto: String, nombreJuego: String): String {
        val limpio = limpiar(nombreJuego)
        val paquete = "com.chispa." + identificador(proyecto)
        val apk = File(ctx.applicationInfo.sourceDir)
        val dirProy = File(ctx.filesDir, "proyectos/" + proyecto)
        val sinFirmar = File(ctx.cacheDir, "sin_firmar.apk")
        val firmado = File(ctx.cacheDir, "firmado.apk")
        sinFirmar.delete()
        firmado.delete()
        armar(apk, dirProy, sinFirmar, paquete, limpio)
        firmar(sinFirmar, firmado)
        val base = limpio.replace(Regex("[^A-Za-z0-9 _.-]"), "_").trim()
        val nombreArchivo = (if (base.isEmpty()) "juego" else base) + ".apk"
        val donde = guardar(ctx, firmado, nombreArchivo)
        sinFirmar.delete()
        firmado.delete()
        return "Listo. Se guardo " + nombreArchivo + " en " + donde + ". Abrilo desde la app Archivos para instalarlo (Android puede pedir permiso para instalar apps desconocidas)."
    }

    private fun identificador(nombre: String): String {
        val h = (nombre.hashCode().toLong() and 0xFFFFFFFFL).toString(36)
        return "j" + h.padStart(5, '0').takeLast(5)
    }

    private fun limpiar(nombre: String): String {
        val sin = Normalizer.normalize(nombre, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
        val ascii = sin.map { if (it.code in 32..126) it else '_' }.joinToString("")
        return ascii.trim().take(32)
    }

    private fun esFirma(n: String): Boolean {
        if (!n.startsWith("META-INF/")) return false
        return n == "META-INF/MANIFEST.MF" || n.endsWith(".SF") || n.endsWith(".RSA") || n.endsWith(".DSA") || n.endsWith(".EC")
    }

    private fun armar(apk: File, proyecto: File, salida: File, paquete: String, etiqueta: String) {
        val esc = Escritor(salida.outputStream().buffered())
        val origen = ZipFile(apk)
        val cambios = HashMap<String, String>()
        cambios[PAQUETE_BASE] = paquete
        cambios[ETIQUETA_BASE] = etiqueta.padEnd(32)
        cambios["Chispa Engine"] = etiqueta.padEnd(13).take(13)
        val entradas = origen.entries()
        while (entradas.hasMoreElements()) {
            val e = entradas.nextElement()
            val n = e.name
            if (e.isDirectory || esFirma(n)) continue
            var datos = origen.getInputStream(e).use { it.readBytes() }
            if (n == "AndroidManifest.xml") datos = Axml.parchear(datos, cambios)
            val almacenado = e.method == ZipEntry.STORED
            val alin = if (!almacenado) 0 else if (n.endsWith(".so")) 16384 else 4
            esc.agregar(n, datos, almacenado, alin)
        }
        origen.close()
        agregarCarpeta(esc, proyecto, proyecto)
        esc.cerrar()
    }

    private fun agregarCarpeta(esc: Escritor, raiz: File, actual: File) {
        val hijos = actual.listFiles() ?: return
        for (f in hijos) {
            if (f.isDirectory) {
                agregarCarpeta(esc, raiz, f)
            } else {
                val rel = f.relativeTo(raiz).path.replace(File.separatorChar, '/')
                val n = f.name.lowercase()
                val audio = n.endsWith(".wav") || n.endsWith(".mp3") || n.endsWith(".ogg")
                esc.agregar("assets/juego/" + rel, f.readBytes(), audio, if (audio) 4 else 0)
            }
        }
    }

    private fun firmar(entrada: File, salida: File) {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val alias = "chispa_exportador"
        if (!ks.containsAlias(alias)) {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS)
                .setCertificateSubject(X500Principal("CN=Chispa Engine"))
                .setCertificateSerialNumber(BigInteger.ONE)
                .build()
            kpg.initialize(spec)
            kpg.generateKeyPair()
        }
        val clave = ks.getKey(alias, null) as PrivateKey
        val cert = ks.getCertificate(alias) as X509Certificate
        val firmante = ApkSigner.SignerConfig.Builder("CHISPA", clave, listOf(cert)).build()
        ApkSigner.Builder(listOf(firmante))
            .setInputApk(entrada)
            .setOutputApk(salida)
            .setMinSdkVersion(26)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(false)
            .setV4SigningEnabled(false)
            .build()
            .sign()
    }

    private fun guardar(ctx: Context, archivo: File, nombre: String): String {
        if (Build.VERSION.SDK_INT >= 29) {
            val v = ContentValues()
            v.put(MediaStore.Downloads.DISPLAY_NAME, nombre)
            v.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
            v.put(MediaStore.Downloads.IS_PENDING, 1)
            val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
                ?: throw IllegalStateException("No se pudo crear el archivo en Descargas")
            ctx.contentResolver.openOutputStream(uri)?.use { s -> archivo.inputStream().use { it.copyTo(s) } }
            v.clear()
            v.put(MediaStore.Downloads.IS_PENDING, 0)
            ctx.contentResolver.update(uri, v, null, null)
            return "Descargas"
        }
        val destino = File(ctx.getExternalFilesDir(null), nombre)
        archivo.copyTo(destino, true)
        return destino.path
    }
}
