package com.chispa.engine

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var lista: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Proyectos.prepararDemo(this)

        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setBackgroundColor(Color.parseColor("#1B2A41"))
        raiz.setPadding(32, 64, 32, 32)

        val titulo = TextView(this)
        titulo.text = "Chispa Engine"
        titulo.textSize = 28f
        titulo.setTextColor(Color.parseColor("#FF8C1A"))
        raiz.addView(titulo)

        val nuevo = Button(this)
        nuevo.text = "+ Nuevo proyecto"
        nuevo.setOnClickListener { pedirNombre() }
        raiz.addView(nuevo)

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

        val bJugar = Button(this)
        bJugar.text = "Jugar"
        bJugar.setOnClickListener { abrir(nombre) }
        f.addView(bJugar)

        val bBorrar = Button(this)
        bBorrar.text = "Borrar"
        bBorrar.setOnClickListener { confirmarBorrado(nombre) }
        f.addView(bBorrar)
        return f
    }

    private fun pedirNombre() {
        val campo = EditText(this)
        campo.inputType = InputType.TYPE_CLASS_TEXT
        campo.hint = "Nombre del proyecto"
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Nuevo proyecto")
            .setView(campo)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = Proyectos.crear(this, campo.text.toString())
                refrescar()
                abrir(nombre)
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
}
