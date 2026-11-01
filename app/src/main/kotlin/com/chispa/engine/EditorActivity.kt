package com.chispa.engine

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
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
        botones.addView(boton("Jugar") { jugar() })
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
            elegirImagen()
        }
        caja.addView(bImagen)
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
                campoImagen?.setText(nombre)
            }
        } catch (ex: Exception) {
            campoImagen?.setText("")
        }
    }
}
