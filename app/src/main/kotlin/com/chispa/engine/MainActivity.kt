package com.chispa.engine

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Toast
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var lista: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (esJuego()) {
            val i = Intent(this, AndroidLauncher::class.java)
            i.putExtra("interno", true)
            startActivity(i)
            finish()
            return
        }
        Proyectos.prepararDemo(this)

        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setBackgroundColor(Color.parseColor("#1B2A41"))
        raiz.setPadding(32, 64, 32, 32)

        val titulo = TextView(this)
        titulo.text = "Chispa Engine"
        titulo.textSize = 28f
        titulo.setTextColor(Color.parseColor("#FF8C1A"))
        val cabecera = LinearLayout(this)
        cabecera.orientation = LinearLayout.HORIZONTAL
        cabecera.gravity = Gravity.CENTER_VERTICAL
        val logo = ImageView(this)
        logo.setImageResource(R.drawable.ic_launcher_foreground)
        cabecera.addView(logo, LinearLayout.LayoutParams(200, 200))
        cabecera.addView(titulo)
        raiz.addView(cabecera)

        val nuevo = Button(this)
        nuevo.text = "+ Nuevo proyecto"
        nuevo.setOnClickListener { pedirNombre() }
        raiz.addView(nuevo)
        val importar = Button(this)
        importar.text = "Importar proyecto (.chispa)"
        importar.setOnClickListener { elegirArchivo() }
        raiz.addView(importar)

        val scroll = ScrollView(this)
        lista = LinearLayout(this)
        lista.orientation = LinearLayout.VERTICAL
        scroll.addView(lista)
        raiz.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(raiz)
    }

    override fun onResume() {
        super.onResume()
        refrescar()
    }

    private fun refrescar() {
        lista.removeAllViews()
        val nombres = Proyectos.listar(this)
        if (nombres.isEmpty()) {
            val vacio = TextView(this)
            vacio.text = "Todavia no hay proyectos."
            vacio.setTextColor(Color.LTGRAY)
            vacio.setPadding(0, 32, 0, 0)
            lista.addView(vacio)
        }
        for (nombre in nombres) lista.addView(fila(nombre))
    }



    private fun confirmarBorrado(nombre: String) {
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Borrar proyecto")
            .setMessage("Se va a borrar " + nombre + " para siempre. Seguro?")
            .setPositiveButton("Borrar") { _, _ ->
                Proyectos.borrar(this, nombre)
                refrescar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrir(nombre: String) {
        val i = Intent(this, AndroidLauncher::class.java)
        i.putExtra("proyecto", nombre)
        startActivity(i)
    }

    private fun editar(nombre: String) {
        val i = Intent(this, EditorActivity::class.java)
        i.putExtra("proyecto", nombre)
        startActivity(i)
    }

    private fun esJuego(): Boolean {
        return try {
            assets.open("juego/proyecto.json").close()
            true
        } catch (ex: Exception) {
            false
        }
    }

    private fun fila(nombre: String): View {
        val f = LinearLayout(this)
        f.orientation = LinearLayout.HORIZONTAL
        f.gravity = Gravity.CENTER_VERTICAL
        f.setPadding(0, 16, 0, 16)
        val t = TextView(this)
        t.text = nombre
        t.textSize = 20f
        t.setTextColor(Color.WHITE)
        f.addView(t, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val bEditar = Button(this)
        bEditar.text = "Editar"
        bEditar.setOnClickListener { editar(nombre) }
        f.addView(bEditar)
        val bJugar = Button(this)
        bJugar.text = "Jugar"
        bJugar.setOnClickListener { abrir(nombre) }
        f.addView(bJugar)
        val bMas = Button(this)
        bMas.text = "Mas"
        bMas.setOnClickListener { menuProyecto(nombre) }
        f.addView(bMas)
        return f
    }

    private fun menuProyecto(nombre: String) {
        val opciones = arrayOf("Compartir / guardar copia (.chispa)", "Duplicar", "Borrar")
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(nombre)
            .setItems(opciones) { _, cual ->
                if (cual == 0) {
                    compartir(nombre)
                } else if (cual == 1) {
                    Proyectos.duplicar(this, nombre)
                    refrescar()
                } else {
                    confirmarBorrado(nombre)
                }
            }
            .show()
    }

    private fun compartir(nombre: String) {
        try {
            val uri = Proyectos.exportarZip(this, nombre)
            if (uri == null) {
                Toast.makeText(this, "No se pudo crear el archivo", Toast.LENGTH_LONG).show()
                return
            }
            Toast.makeText(this, "Guardado en Descargas: " + nombre + ".chispa", Toast.LENGTH_LONG).show()
            val envio = Intent(Intent.ACTION_SEND)
            envio.type = "application/octet-stream"
            envio.putExtra(Intent.EXTRA_STREAM, uri)
            envio.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(envio, "Compartir proyecto"))
        } catch (ex: Exception) {
            Toast.makeText(this, "Error: " + ex.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun elegirArchivo() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "*/*"
        i.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(i, 7)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (requestCode != 7 || resultCode != RESULT_OK || uri == null) return
        try {
            val nombre = Proyectos.importarZip(this, uri)
            Toast.makeText(this, "Proyecto importado: " + nombre, Toast.LENGTH_LONG).show()
            refrescar()
        } catch (ex: Exception) {
            Toast.makeText(this, "No se pudo importar: " + ex.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun pedirNombre() {
        val caja = LinearLayout(this)
        caja.orientation = LinearLayout.VERTICAL
        caja.setPadding(48, 16, 48, 0)
        val campo = EditText(this)
        campo.inputType = InputType.TYPE_CLASS_TEXT
        campo.hint = "Nombre del proyecto"
        caja.addView(campo)
        val plantillas = plantillasDisponibles()
        val etiquetas = ArrayList<String>()
        etiquetas.add("En blanco")
        for (p in plantillas) etiquetas.add(etiquetaPlantilla(p))
        val selector = Spinner(this)
        selector.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, etiquetas)
        caja.addView(selector)
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Nuevo proyecto")
            .setView(caja)
            .setPositiveButton("Crear") { _, _ ->
                val pos = selector.selectedItemPosition
                val texto = campo.text.toString()
                val nombre = if (pos == 0) Proyectos.crear(this, texto) else Proyectos.crearDesdePlantilla(this, texto, plantillas[pos - 1])
                refrescar()
                abrir(nombre)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun plantillasDisponibles(): List<String> {
        val l = assets.list("plantillas") ?: return emptyList()
        return l.toList().sorted()
    }

    private fun etiquetaPlantilla(p: String): String = when (p) {
        "clicker" -> "Clicker: toca y suma puntos"
        "esquivar" -> "Esquivar obstaculos (vertical)"
        "pelota" -> "Pelota que rebota"
        else -> p
    }
}
