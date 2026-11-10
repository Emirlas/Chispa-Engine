package com.chispa.engine

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class EditorActivity : Activity() {

    private lateinit var nombreProyecto: String
    private lateinit var dir: File
    private lateinit var proyecto: JSONObject
    private lateinit var escena: JSONObject
    private var escenaId = ""
    private lateinit var titulo: TextView
    private lateinit var lista: LinearLayout
    private var campoImagen: EditText? = null
    private var spinnerFuente: Spinner? = null
    private var textoFuentes: TextView? = null
    private var anexar = false
    private var textoSonidos: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nombreProyecto = intent.getStringExtra("proyecto") ?: "Demo"
        dir = File(filesDir, "proyectos/" + nombreProyecto)
        proyecto = JSONObject(File(dir, "proyecto.json").readText())
        escenaId = proyecto.optString("escenaInicial", "escena_1")
        cargarEscena()

        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setBackgroundColor(Color.parseColor("#1B2A41"))
        raiz.setPadding(32, 64, 32, 32)

        val cab = TextView(this)
        cab.text = "Editando: " + nombreProyecto
        cab.textSize = 22f
        cab.setTextColor(Color.parseColor("#FF8C1A"))
        raiz.addView(cab)

        titulo = TextView(this)
        titulo.textSize = 16f
        titulo.setTextColor(Color.WHITE)
        titulo.setPadding(0, 8, 0, 8)
        raiz.addView(titulo)

        val botones = LinearLayout(this)
        botones.orientation = LinearLayout.HORIZONTAL
        botones.addView(boton("Escenas") { elegirEscena() })
        botones.addView(boton("+ Objeto") { nuevoObjeto() })
        botones.addView(boton("Ajustes") { ajustes() })
        botones.addView(boton("Jugar") { jugar() })
        val botones2 = LinearLayout(this)
        botones2.orientation = LinearLayout.HORIZONTAL
        botones2.addView(boton("Vista de la escena (arrastrar objetos)") { abrirVista() })
        raiz.addView(botones2)
        val botones3 = LinearLayout(this)
        botones3.addView(boton("Exportar APK (juego instalable)") { exportar() })
        raiz.addView(botones3)
        raiz.addView(botones)

        val scroll = ScrollView(this)
        lista = LinearLayout(this)
        lista.orientation = LinearLayout.VERTICAL
        scroll.addView(lista)
        raiz.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(raiz)
        refrescar()
    }

    private fun boton(texto: String, accion: () -> Unit): Button {
        val b = Button(this)
        b.text = texto
        b.setOnClickListener { accion() }
        return b
    }

    private fun cargarEscena() {
        val f = File(dir, "escenas/" + escenaId + ".json")
        escena = if (f.exists()) JSONObject(f.readText()) else nuevaEscenaJson(escenaId)
        if (!escena.has("objetos")) escena.put("objetos", JSONArray())
    }

    private fun nuevaEscenaJson(id: String): JSONObject {
        val e = JSONObject()
        e.put("id", id)
        e.put("nombre", id)
        e.put("fondo", JSONObject().put("color", "#1B2A41"))
        e.put("objetos", JSONArray())
        return e
    }

    private fun guardarEscena() {
        File(dir, "escenas").mkdirs()
        File(dir, "escenas/" + escenaId + ".json").writeText(escena.toString(2))
    }

    private fun guardarProyecto() {
        File(dir, "proyecto.json").writeText(proyecto.toString(2))
    }

    private fun escenasDisponibles(): List<String> {
        val archivos = File(dir, "escenas").listFiles() ?: return emptyList()
        return archivos.filter { it.name.endsWith(".json") }.map { it.name.removeSuffix(".json") }.sorted()
    }

    private fun num(d: Double): String = if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()

    private fun leer(e: EditText, defecto: Double): Double = e.text.toString().toDoubleOrNull() ?: defecto

    private fun refrescar() {
        titulo.text = "Escena: " + escenaId
        lista.removeAllViews()
        val objetos = escena.getJSONArray("objetos")
        if (objetos.length() == 0) {
            val vacio = TextView(this)
            vacio.text = "Esta escena no tiene objetos. Toca + Objeto."
            vacio.setTextColor(Color.LTGRAY)
            vacio.setPadding(0, 32, 0, 0)
            lista.addView(vacio)
        }
        for (i in 0 until objetos.length()) {
            lista.addView(fila(i, objetos.getJSONObject(i).optString("nombre", "Objeto")))
        }
    }

    private fun fila(i: Int, nombre: String): View {
        val f = LinearLayout(this)
        f.orientation = LinearLayout.HORIZONTAL
        f.gravity = Gravity.CENTER_VERTICAL
        f.setPadding(0, 12, 0, 12)
        val t = TextView(this)
        t.text = nombre
        t.textSize = 18f
        t.setTextColor(Color.WHITE)
        f.addView(t, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        f.addView(boton("Editar") { editarObjeto(i) })
        f.addView(boton("Scripts") { abrirScripts(i) })
        f.addView(boton("Borrar") { borrarObjeto(i, nombre) })
        return f
    }

    private fun nuevoObjeto() {
        val o = JSONObject()
        val n = escena.getJSONArray("objetos").length() + 1
        o.put("id", "obj_" + System.currentTimeMillis())
        o.put("nombre", "Objeto " + n)
        o.put("color", "#FF8C1A")
        o.put("tamano", JSONObject().put("ancho", 100).put("alto", 100))
        o.put("x", 0)
        o.put("y", 0)
        o.put("escala", 1.0)
        o.put("rotacion", 0)
        o.put("visible", true)
        o.put("capa", 1)
        o.put("scripts", JSONArray())
        escena.getJSONArray("objetos").put(o)
        guardarEscena()
        refrescar()
    }

    private fun borrarObjeto(i: Int, nombre: String) {
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Borrar objeto")
            .setMessage("Se va a borrar " + nombre + ". Seguro?")
            .setPositiveButton("Borrar") { _, _ ->
                escena.getJSONArray("objetos").remove(i)
                guardarEscena()
                refrescar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun jugar() {
        guardarEscena()
        guardarProyecto()
        val intento = Intent(this, AndroidLauncher::class.java)
        intento.putExtra("proyecto", nombreProyecto)
        startActivity(intento)
    }

    private fun elegirEscena() {
        val ids = escenasDisponibles()
        val items = ArrayList<String>(ids)
        items.add("+ Nueva escena")
        items.add("Usar esta escena como inicial")
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Escenas")
            .setItems(items.toTypedArray()) { _, cual ->
                if (cual < ids.size) {
                    escenaId = ids[cual]
                    cargarEscena()
                    refrescar()
                } else if (cual == ids.size) {
                    nuevaEscena()
                } else {
                    proyecto.put("escenaInicial", escenaId)
                    guardarProyecto()
                }
            }
            .show()
    }

    private fun nuevaEscena() {
        val campo = EditText(this)
        campo.hint = "Nombre de la escena"
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Nueva escena")
            .setView(campo)
            .setPositiveButton("Crear") { _, _ ->
                var base = campo.text.toString().trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")
                if (base.isEmpty()) base = "escena"
                var id = base
                var n = 2
                while (File(dir, "escenas/" + id + ".json").exists()) {
                    id = base + "_" + n
                    n++
                }
                guardarEscena()
                escenaId = id
                escena = nuevaEscenaJson(id)
                guardarEscena()
                refrescar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun campo(padre: LinearLayout, etiqueta: String, valor: String, numerico: Boolean): EditText {
        val t = TextView(this)
        t.text = etiqueta
        t.setTextColor(Color.LTGRAY)
        padre.addView(t)
        val e = EditText(this)
        e.setText(valor)
        e.setTextColor(Color.WHITE)
        if (numerico) {
            e.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        padre.addView(e)
        return e
    }

    private fun editarObjeto(i: Int) {
        val o = escena.getJSONArray("objetos").getJSONObject(i)
        val tam = o.optJSONObject("tamano")
        val caja = LinearLayout(this)
        caja.orientation = LinearLayout.VERTICAL
        caja.setPadding(48, 16, 48, 0)
        val nombre = campo(caja, "Nombre", o.optString("nombre", ""), false)
        val x = campo(caja, "X (0 = centro)", num(o.optDouble("x", 0.0)), true)
        val y = campo(caja, "Y (0 = centro, arriba es +)", num(o.optDouble("y", 0.0)), true)
        val ancho = campo(caja, "Ancho", num(tam?.optDouble("ancho", 100.0) ?: 100.0), true)
        val alto = campo(caja, "Alto", num(tam?.optDouble("alto", 100.0) ?: 100.0), true)
        val escala = campo(caja, "Escala", num(o.optDouble("escala", 1.0)), true)
        val rot = campo(caja, "Rotacion (grados)", num(o.optDouble("rotacion", 0.0)), true)
        val capa = campo(caja, "Capa (mayor = adelante)", num(o.optDouble("capa", 1.0)), true)
        val color = campo(caja, "Color (#RRGGBB)", o.optString("color", "#FFFFFF"), false)
        val imagen = campo(caja, "Imagen (archivo)", o.optString("imagen", ""), false)
        val bImagen = boton("Elegir imagen de la galeria") {
            campoImagen = imagen
            anexar = false
            elegirImagen()
        }
        caja.addView(bImagen)
        val disf = campo(caja, "Disfraces (animacion): archivos separados por coma", disfracesTexto(o), false)
        caja.addView(boton("+ Agregar disfraz desde la galeria") {
            campoImagen = disf
            anexar = true
            elegirImagen()
        })
        val texto = campo(caja, "Texto (opcional, usa {variable})", o.optString("texto", ""), false)
        val tamT = campo(caja, "Tamano del texto", num(o.optDouble("tamTexto", 40.0)), true)
        val alinT = combo(caja, "Alineacion", listOf("izquierda", "centro", "derecha"), o.optString("alinTexto", "centro"))
        val estT = combo(caja, "Estilo", listOf("normal", "negrita", "sombra", "contorno"), o.optString("estiloTexto", "normal"))
        val etFuente = TextView(this)
        etFuente.text = "Fuente"
        etFuente.setTextColor(Color.LTGRAY)
        caja.addView(etFuente)
        val fueT = Spinner(this)
        ponerFuentes(fueT, o.optString("fuenteTexto", ""))
        spinnerFuente = fueT
        caja.addView(fueT)
        caja.addView(boton("+ Agregar fuente (.ttf)") { elegirFuente() })
        val visible = CheckBox(this)
        visible.text = "Visible"
        visible.setTextColor(Color.WHITE)
        visible.isChecked = o.optBoolean("visible", true)
        caja.addView(visible)
        val scroll = ScrollView(this)
        scroll.addView(caja)
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Objeto")
            .setView(scroll)
            .setPositiveButton("Guardar") { _, _ ->
                o.put("nombre", nombre.text.toString().trim().ifEmpty { "Objeto" })
                o.put("x", leer(x, 0.0))
                o.put("y", leer(y, 0.0))
                o.put("tamano", JSONObject().put("ancho", maxOf(1.0, leer(ancho, 100.0))).put("alto", maxOf(1.0, leer(alto, 100.0))))
                o.put("escala", maxOf(0.01, leer(escala, 1.0)))
                o.put("rotacion", leer(rot, 0.0))
                o.put("capa", leer(capa, 1.0).toInt())
                val c = color.text.toString().trim()
                if (Regex("^#[0-9A-Fa-f]{6}$").matches(c)) o.put("color", c)
                val im = imagen.text.toString().trim()
                if (im.isEmpty()) o.remove("imagen") else o.put("imagen", im)
                val ld = JSONArray()
                for (d in disf.text.toString().split(",")) {
                    val dd = d.trim()
                    if (dd.isNotEmpty()) ld.put(dd)
                }
                if (ld.length() == 0) o.remove("disfraces") else o.put("disfraces", ld)
                val tt = texto.text.toString()
                if (tt.isEmpty()) o.remove("texto") else o.put("texto", tt)
                o.put("tamTexto", maxOf(4.0, leer(tamT, 40.0)))
                o.put("alinTexto", alinT.selectedItem as String)
                o.put("estiloTexto", estT.selectedItem as String)
                val fs = fueT.selectedItem as String
                o.put("fuenteTexto", if (fs == "(predeterminada)") "" else fs)
                o.put("visible", visible.isChecked)
                guardarEscena()
                refrescar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun elegirImagen() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        i.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(i, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 || requestCode == 4) {
            cargarEscena()
            refrescar()
            return
        }
        if (requestCode == 5) {
            copiarSonido(data?.data)
            return
        }
        if (requestCode == 3) {
            copiarFuente(data?.data)
            return
        }
        val uri = data?.data
        if (requestCode != 1 || resultCode != RESULT_OK || uri == null) return
        try {
            val medida = BitmapFactory.Options()
            medida.inJustDecodeBounds = true
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medida) }
            var muestra = 1
            while (medida.outWidth / muestra > 1024 || medida.outHeight / muestra > 1024) muestra *= 2
            val real = BitmapFactory.Options()
            real.inSampleSize = muestra
            val bmp = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, real) }
            if (bmp != null) {
                val nombre = "img_" + System.currentTimeMillis() + ".png"
                val destino = File(dir, "assets/imagenes/" + nombre)
                destino.parentFile?.mkdirs()
                destino.outputStream().use { s -> bmp.compress(Bitmap.CompressFormat.PNG, 100, s) }
                val ci = campoImagen
                if (ci != null) {
                    if (anexar && ci.text.isNotEmpty()) ci.setText(ci.text.toString() + ", " + nombre) else ci.setText(nombre)
                }
            }
        } catch (ex: Exception) {
            campoImagen?.setText("")
        }
    }

    private fun abrirScripts(i: Int) {
        guardarEscena()
        val intento = Intent(this, ScriptsActivity::class.java)
        intento.putExtra("proyecto", nombreProyecto)
        intento.putExtra("escena", escenaId)
        intento.putExtra("indice", i)
        startActivityForResult(intento, 2)
    }

    private fun elegirFuente() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "*/*"
        i.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(i, 3)
    }

    private fun copiarFuente(uri: Uri?) {
        if (uri == null) return
        try {
            var nombre = "fuente_" + System.currentTimeMillis() + ".ttf"
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (col >= 0 && c.moveToFirst()) nombre = c.getString(col)
            }
            nombre = nombre.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val destino = File(dir, "assets/fuentes/" + nombre)
            destino.parentFile?.mkdirs()
            contentResolver.openInputStream(uri)?.use { e -> destino.outputStream().use { s -> e.copyTo(s) } }
            fuenteAgregada(nombre)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun fuentesDisponibles(): List<String> {
        val archivos = File(dir, "assets/fuentes").listFiles() ?: return emptyList()
        return archivos.filter { it.isFile }.map { it.name }.sorted()
    }

    private fun textoListaFuentes(): String {
        val l = fuentesDisponibles()
        return if (l.isEmpty()) "(ninguna todavia)" else l.joinToString("\n")
    }

    private fun ponerFuentes(s: Spinner, seleccion: String) {
        val items = ArrayList<String>()
        items.add("(predeterminada)")
        items.addAll(fuentesDisponibles())
        s.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        val i = items.indexOf(seleccion)
        s.setSelection(if (i >= 0) i else 0)
    }

    private fun combo(padre: LinearLayout, etiqueta: String, opciones: List<String>, actual: String): Spinner {
        val t = TextView(this)
        t.text = etiqueta
        t.setTextColor(Color.LTGRAY)
        padre.addView(t)
        val s = Spinner(this)
        s.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opciones)
        val i = opciones.indexOf(actual)
        s.setSelection(if (i >= 0) i else 0)
        padre.addView(s)
        return s
    }

    private fun fuenteAgregada(nombre: String) {
        val s = spinnerFuente
        if (s != null) ponerFuentes(s, nombre)
        textoFuentes?.text = textoListaFuentes()
    }

    private fun ajustes() {
        val caja = LinearLayout(this)
        caja.orientation = LinearLayout.VERTICAL
        caja.setPadding(48, 16, 48, 0)
        val verVars = CheckBox(this)
        verVars.text = "Mostrar variables en pantalla"
        verVars.setTextColor(Color.WHITE)
        verVars.isChecked = proyecto.optBoolean("mostrarVariables", true)
        caja.addView(verVars)
        val verDebug = CheckBox(this)
        verDebug.text = "Mostrar informacion de depuracion"
        verDebug.setTextColor(Color.WHITE)
        verDebug.isChecked = proyecto.optBoolean("mostrarDebug", true)
        caja.addView(verDebug)
        val fondo = escena.optJSONObject("fondo")
        val colorFondo = campo(caja, "Color de fondo de esta escena (#RRGGBB)", fondo?.optString("color", "#1B2A41") ?: "#1B2A41", false)
        val etSon = TextView(this)
        etSon.text = "Sonidos del proyecto:"
        etSon.setTextColor(Color.LTGRAY)
        etSon.setPadding(0, 24, 0, 0)
        caja.addView(etSon)
        val vistaSon = TextView(this)
        vistaSon.setTextColor(Color.WHITE)
        vistaSon.text = textoListaSonidos()
        textoSonidos = vistaSon
        caja.addView(vistaSon)
        caja.addView(boton("+ Agregar sonido (.wav, .ogg, .mp3)") { elegirSonido() })
        val etiqueta = TextView(this)
        etiqueta.text = "Fuentes del proyecto:"
        etiqueta.setTextColor(Color.LTGRAY)
        etiqueta.setPadding(0, 24, 0, 0)
        caja.addView(etiqueta)
        val vistaFuentes = TextView(this)
        vistaFuentes.setTextColor(Color.WHITE)
        vistaFuentes.text = textoListaFuentes()
        textoFuentes = vistaFuentes
        caja.addView(vistaFuentes)
        caja.addView(boton("+ Agregar fuente (.ttf)") { elegirFuente() })
        val scroll = ScrollView(this)
        scroll.addView(caja)
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Ajustes del proyecto")
            .setView(scroll)
            .setPositiveButton("Guardar") { _, _ ->
                proyecto.put("mostrarVariables", verVars.isChecked)
                proyecto.put("mostrarDebug", verDebug.isChecked)
                val c = colorFondo.text.toString().trim()
                if (Regex("^#[0-9A-Fa-f]{6}$").matches(c)) {
                    val f = escena.optJSONObject("fondo") ?: JSONObject()
                    f.put("color", c)
                    escena.put("fondo", f)
                }
                guardarProyecto()
                guardarEscena()
                refrescar()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun abrirVista() {
        guardarEscena()
        val intento = Intent(this, VistaActivity::class.java)
        intento.putExtra("proyecto", nombreProyecto)
        intento.putExtra("escena", escenaId)
        startActivityForResult(intento, 4)
    }

    private fun exportar() {
        val campo = EditText(this)
        campo.setText(nombreProyecto)
        campo.setTextColor(Color.WHITE)
        campo.hint = "Nombre del juego (hasta 32 letras)"
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Exportar APK")
            .setMessage("Se genera un APK instalable con este juego y se guarda en Descargas. Puede tardar unos segundos.")
            .setView(campo)
            .setPositiveButton("Exportar") { _, _ ->
                guardarEscena()
                guardarProyecto()
                val nombre = campo.text.toString().trim().ifEmpty { nombreProyecto }
                val espera = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setMessage("Exportando, espera unos segundos...")
                    .setCancelable(false)
                    .show()
                Thread {
                    val mensaje = try {
                        Exportador.exportar(this, nombreProyecto, nombre)
                    } catch (ex: Throwable) {
                        "Error al exportar: " + ex.toString()
                    }
                    runOnUiThread {
                        espera.dismiss()
                        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                            .setTitle("Exportacion")
                            .setMessage(mensaje)
                            .setPositiveButton("Aceptar", null)
                            .show()
                    }
                }.start()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun disfracesTexto(o: JSONObject): String {
        val d = o.optJSONArray("disfraces") ?: return ""
        val l = ArrayList<String>()
        for (i in 0 until d.length()) l.add(d.optString(i, ""))
        return l.joinToString(", ")
    }

    private fun textoListaSonidos(): String {
        val l = File(dir, "assets/sonidos").listFiles()?.filter { it.isFile }?.map { it.name }?.sorted() ?: emptyList()
        return if (l.isEmpty()) "(ninguno todavia)" else l.joinToString("\n")
    }

    private fun elegirSonido() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "audio/*"
        i.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(i, 5)
    }

    private fun copiarSonido(uri: Uri?) {
        if (uri == null) return
        try {
            var nombre = "sonido_" + System.currentTimeMillis() + ".wav"
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (col >= 0 && c.moveToFirst()) nombre = c.getString(col)
            }
            nombre = nombre.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val destino = File(dir, "assets/sonidos/" + nombre)
            destino.parentFile?.mkdirs()
            contentResolver.openInputStream(uri)?.use { e -> destino.outputStream().use { s -> e.copyTo(s) } }
            textoSonidos?.text = textoListaSonidos()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}
