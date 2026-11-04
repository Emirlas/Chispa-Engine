package com.chispa.engine

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.io.File

class VistaActivity : Activity() {

    private lateinit var dir: File
    private lateinit var escena: JSONObject
    private var escenaId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val proyecto = intent.getStringExtra("proyecto") ?: "Demo"
        escenaId = intent.getStringExtra("escena") ?: "escena_1"
        dir = File(filesDir, "proyectos/" + proyecto)
        escena = JSONObject(File(dir, "escenas/" + escenaId + ".json").readText())
        setResult(RESULT_OK)

        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setBackgroundColor(Color.parseColor("#0F1722"))

        val info = TextView(this)
        info.setTextColor(Color.WHITE)
        info.setPadding(24, 12, 24, 8)
        info.text = descripcion(null)
        raiz.addView(info)

        val lienzo = LienzoView(this, dir, escena, { guardar() }, { o -> info.text = descripcion(o) })
        raiz.addView(lienzo, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val fila = LinearLayout(this)
        fila.orientation = LinearLayout.HORIZONTAL
        fila.addView(boton("Cuadricula") { lienzo.cuadricula = !lienzo.cuadricula })
        fila.addView(boton("Duplicar") { lienzo.duplicar() })
        fila.addView(boton("Borrar") { lienzo.borrar() })
        fila.addView(boton("Capa -") { lienzo.capa(-1) })
        fila.addView(boton("Capa +") { lienzo.capa(1) })
        fila.addView(boton("Volver") { finish() })
        raiz.addView(fila)
        setContentView(raiz)
    }

    private fun boton(texto: String, accion: () -> Unit): Button {
        val b = Button(this)
        b.text = texto
        b.setAllCaps(false)
        b.setOnClickListener { accion() }
        b.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        return b
    }

    private fun guardar() {
        File(dir, "escenas/" + escenaId + ".json").writeText(escena.toString(2))
    }

    private fun descripcion(o: JSONObject?): String {
        if (o == null) return "Toca un objeto para elegirlo y arrastralo. Circulo naranja: tamano. Circulo azul: rotar."
        return o.optString("nombre", "Objeto") +
            "   x=" + Math.round(o.optDouble("x", 0.0)) +
            "  y=" + Math.round(o.optDouble("y", 0.0)) +
            "  escala=" + String.format("%.2f", o.optDouble("escala", 1.0)) +
            "  rot=" + Math.round(o.optDouble("rotacion", 0.0)) +
            "  capa=" + o.optInt("capa", 0)
    }
}
