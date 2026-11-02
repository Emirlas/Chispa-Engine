package com.chispa.engine

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ScriptsActivity : Activity() {

    private lateinit var dir: File
    private lateinit var escena: JSONObject
    private lateinit var objeto: JSONObject
    private lateinit var contenido: LinearLayout
    private var escenaId = ""

    private val tipos = arrayOf("mover", "ir_a", "rotar", "esperar", "repetir", "por_siempre", "asignar", "cambiar", "si", "esperar_hasta", "mostrar_texto", "borrar_texto", "reproducir_sonido", "ir_a_escena", "detener_script")
    private val nombres = arrayOf("Mover pasos", "Ir a x, y", "Rotar grados", "Esperar segundos", "Repetir veces", "Por siempre", "Asignar variable", "Cambiar variable", "Si / sino", "Esperar hasta que", "Mostrar texto", "Borrar texto", "Reproducir sonido", "Ir a escena", "Detener este script")
    private val condiciones = arrayOf("(dejar la condicion actual)", "Variable mayor que numero", "Variable menor que numero", "Variable igual a numero", "Tocando la pantalla", "Tocando objeto")
    private val numericos = mapOf(
        "mover" to listOf("pasos"), "ir_a" to listOf("x", "y"), "rotar" to listOf("grados"),
        "esperar" to listOf("segundos"), "repetir" to listOf("veces"),
        "asignar" to listOf("valor"), "cambiar" to listOf("valor")
    )
    private val textos = mapOf(
        "asignar" to listOf("variable"), "cambiar" to listOf("variable"),
        "mostrar_texto" to listOf("texto"), "reproducir_sonido" to listOf("sonido"),
        "ir_a_escena" to listOf("escena")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val proyecto = intent.getStringExtra("proyecto") ?: "Demo"
        escenaId = intent.getStringExtra("escena") ?: "escena_1"
        val indice = intent.getIntExtra("indice", 0)
        dir = File(filesDir, "proyectos/" + proyecto)
        escena = JSONObject(File(dir, "escenas/" + escenaId + ".json").readText())
        objeto = escena.getJSONArray("objetos").getJSONObject(indice)
        setResult(RESULT_OK)

        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setBackgroundColor(Color.parseColor("#1B2A41"))
        raiz.setPadding(24, 64, 24, 24)

        val cab = TextView(this)
        cab.text = "Scripts de: " + objeto.optString("nombre", "Objeto")
        cab.textSize = 22f
        cab.setTextColor(Color.parseColor("#FF8C1A"))
        raiz.addView(cab)

        val nuevo = Button(this)
        nuevo.text = "+ Script"
        nuevo.setOnClickListener { elegirEvento() }
        raiz.addView(nuevo)

        val scroll = ScrollView(this)
        contenido = LinearLayout(this)
        contenido.orientation = LinearLayout.VERTICAL
        scroll.addView(contenido)
        raiz.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(raiz)
        redibujar()
    }

    private fun guardar() {
        File(dir, "escenas/" + escenaId + ".json").writeText(escena.toString(2))
    }

    private fun lista(o: JSONObject, clave: String): JSONArray {
        var a = o.optJSONArray(clave)
        if (a == null) {
            a = JSONArray()
            o.put(clave, a)
        }
        return a
    }

    private fun num(d: Double): String = if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()

    private fun etiquetaEvento(t: String): String = when (t) {
        "al_iniciar" -> "Al iniciar el juego"
        "al_tocar_pantalla" -> "Al tocar la pantalla"
        "al_tocar_objeto" -> "Al tocar este objeto"
        else -> t
    }

    private fun expr(v: Any?): String {
        if (v is JSONObject) {
            val a = expr(v.opt("a"))
            val b = expr(v.opt("b"))
            return when (v.optString("tipo")) {
                "var" -> v.optString("nombre")
                "azar" -> "azar(" + expr(v.opt("min")) + ", " + expr(v.opt("max")) + ")"
                "suma" -> "(" + a + " + " + b + ")"
                "resta" -> "(" + a + " - " + b + ")"
                "mult" -> "(" + a + " x " + b + ")"
                "div" -> "(" + a + " / " + b + ")"
                "mayor" -> a + " > " + b
                "menor" -> a + " < " + b
                "igual" -> a + " = " + b
                "y" -> a + " y " + b
                "o" -> a + " o " + b
                "no" -> "no " + a
                "toque_x" -> "toque x"
                "toque_y" -> "toque y"
                "temporizador" -> "temporizador"
                "tocando_pantalla" -> "tocando la pantalla"
                "tocando_objeto" -> "tocando " + v.optString("objeto")
                else -> v.optString("tipo")
            }
        }
        if (v is Number) return num(v.toDouble())
        return if (v == null) "0" else v.toString()
    }

    private fun describir(b: JSONObject): String = when (b.optString("tipo")) {
        "mover" -> "Mover " + expr(b.opt("pasos")) + " pasos"
        "ir_a" -> "Ir a x: " + expr(b.opt("x")) + "  y: " + expr(b.opt("y"))
        "rotar" -> "Rotar " + expr(b.opt("grados")) + " grados"
        "esperar" -> "Esperar " + expr(b.opt("segundos")) + " seg"
        "repetir" -> "Repetir " + expr(b.opt("veces")) + " veces"
        "por_siempre" -> "Por siempre"
        "asignar" -> "Asignar " + b.optString("variable") + " = " + expr(b.opt("valor"))
        "cambiar" -> "Cambiar " + b.optString("variable") + " por " + expr(b.opt("valor"))
        "si" -> "Si " + expr(b.opt("condicion"))
        "esperar_hasta" -> "Esperar hasta que " + expr(b.opt("condicion"))
        "mostrar_texto" -> "Mostrar texto: " + b.optString("texto")
        "borrar_texto" -> "Borrar texto"
        "reproducir_sonido" -> "Sonido: " + b.optString("sonido")
        "ir_a_escena" -> "Ir a escena: " + b.optString("escena")
        "detener_script" -> "Detener este script"
        else -> b.optString("tipo")
    }

    private fun mini(texto: String, accion: () -> Unit): Button {
        val b = Button(this)
        b.text = texto
        b.minWidth = 0
        b.minimumWidth = 0
        b.setPadding(24, 0, 24, 0)
        b.setOnClickListener { accion() }
        return b
    }

    private fun redibujar() {
        contenido.removeAllViews()
        val scripts = lista(objeto, "scripts")
        if (scripts.length() == 0) {
            val vacio = TextView(this)
            vacio.text = "Este objeto no tiene scripts. Toca + Script."
            vacio.setTextColor(Color.LTGRAY)
            vacio.setPadding(0, 32, 0, 0)
            contenido.addView(vacio)
        }
        for (i in 0 until scripts.length()) {
            val s = scripts.getJSONObject(i)
            val cab = LinearLayout(this)
            cab.orientation = LinearLayout.HORIZONTAL
            cab.gravity = Gravity.CENTER_VERTICAL
            cab.setPadding(0, 32, 0, 8)
            val t = TextView(this)
            t.text = etiquetaEvento(s.optJSONObject("evento")?.optString("tipo") ?: "")
            t.textSize = 18f
            t.setTextColor(Color.parseColor("#FF8C1A"))
            cab.addView(t, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            cab.addView(mini("Borrar") { borrarScript(i) })
            contenido.addView(cab)
            val bloques = lista(s, "bloques")
            dibujar(bloques, 1)
            contenido.addView(botonAgregar(bloques, 1))
        }
    }

    private fun dibujar(bloques: JSONArray, nivel: Int) {
        for (i in 0 until bloques.length()) {
            val b = bloques.getJSONObject(i)
            contenido.addView(filaBloque(bloques, i, b, nivel))
            val tipo = b.optString("tipo")
            if (tipo == "repetir" || tipo == "por_siempre" || tipo == "si") {
                val cuerpo = lista(b, "cuerpo")
                dibujar(cuerpo, nivel + 1)
                contenido.addView(botonAgregar(cuerpo, nivel + 1))
            }
            if (tipo == "si") {
                val et = TextView(this)
                et.text = "Si no:"
                et.setTextColor(Color.LTGRAY)
                et.setPadding(nivel * 40, 8, 0, 0)
                contenido.addView(et)
                val sino = lista(b, "sino")
                dibujar(sino, nivel + 1)
                contenido.addView(botonAgregar(sino, nivel + 1))
            }
        }
    }

    private fun filaBloque(bloques: JSONArray, i: Int, b: JSONObject, nivel: Int): View {
        val f = LinearLayout(this)
        f.orientation = LinearLayout.HORIZONTAL
        f.gravity = Gravity.CENTER_VERTICAL
        f.setPadding(nivel * 40, 6, 0, 6)
        val t = TextView(this)
        t.text = describir(b)
        t.textSize = 16f
        t.setTextColor(Color.WHITE)
        t.setBackgroundColor(Color.parseColor("#2E4A6B"))
        t.setPadding(16, 20, 16, 20)
        t.setOnClickListener { editarBloque(b) }
        f.addView(t, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        f.addView(mini("^") { mover(bloques, i, -1) })
        f.addView(mini("v") { mover(bloques, i, 1) })
        f.addView(mini("X") { quitar(bloques, i) })
        return f
    }

    private fun botonAgregar(bloques: JSONArray, nivel: Int): View {
        val f = LinearLayout(this)
        f.setPadding(nivel * 40, 6, 0, 6)
        val b = Button(this)
        b.text = "+ Bloque"
        b.setOnClickListener { elegirBloque(bloques) }
        f.addView(b)
        return f
    }

    private fun mover(bloques: JSONArray, i: Int, d: Int) {
        val j = i + d
        if (j < 0 || j >= bloques.length()) return
        val a = bloques.get(i)
        val b = bloques.get(j)
        bloques.put(i, b)
        bloques.put(j, a)
        guardar()
        redibujar()
    }

    private fun quitar(bloques: JSONArray, i: Int) {
        bloques.remove(i)
        guardar()
        redibujar()
    }

    private fun borrarScript(i: Int) {
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Borrar script")
            .setMessage("Se va a borrar el script completo. Seguro?")
            .setPositiveButton("Borrar") { _, _ ->
                lista(objeto, "scripts").remove(i)
                guardar()
                redibujar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun elegirEvento() {
        val ids = arrayOf("al_iniciar", "al_tocar_pantalla", "al_tocar_objeto")
        val etiquetas = arrayOf("Al iniciar el juego", "Al tocar la pantalla", "Al tocar este objeto")
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Nuevo script: cuando...")
            .setItems(etiquetas) { _, cual ->
                val s = JSONObject()
                s.put("evento", JSONObject().put("tipo", ids[cual]))
                s.put("bloques", JSONArray())
                lista(objeto, "scripts").put(s)
                guardar()
                redibujar()
            }
            .show()
    }

    private fun elegirBloque(bloques: JSONArray) {
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Agregar bloque")
            .setItems(nombres) { _, cual ->
                bloques.put(nuevoBloque(tipos[cual]))
                guardar()
                redibujar()
            }
            .show()
    }

    private fun nuevoBloque(tipo: String): JSONObject {
        val b = JSONObject()
        b.put("tipo", tipo)
        when (tipo) {
            "mover" -> b.put("pasos", 10)
            "ir_a" -> {
                b.put("x", 0)
                b.put("y", 0)
            }
            "rotar" -> b.put("grados", 15)
            "esperar" -> b.put("segundos", 1)
            "repetir" -> {
                b.put("veces", 10)
                b.put("cuerpo", JSONArray())
            }
            "por_siempre" -> b.put("cuerpo", JSONArray())
            "asignar" -> {
                b.put("variable", "puntos")
                b.put("valor", 0)
            }
            "cambiar" -> {
                b.put("variable", "puntos")
                b.put("valor", 1)
            }
            "si" -> {
                b.put("condicion", JSONObject().put("tipo", "tocando_pantalla"))
                b.put("cuerpo", JSONArray())
                b.put("sino", JSONArray())
            }
            "esperar_hasta" -> b.put("condicion", JSONObject().put("tipo", "tocando_pantalla"))
            "mostrar_texto" -> b.put("texto", "Hola")
            "reproducir_sonido" -> b.put("sonido", "moneda.wav")
            "ir_a_escena" -> b.put("escena", "menu")
        }
        return b
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

    private fun construirCondicion(sel: Int, nombre: String, valor: Double): JSONObject? {
        if (sel == 0) return null
        if (sel == 4) return JSONObject().put("tipo", "tocando_pantalla")
        if (sel == 5) return JSONObject().put("tipo", "tocando_objeto").put("objeto", nombre)
        val op = if (sel == 1) "mayor" else if (sel == 2) "menor" else "igual"
        val a = JSONObject().put("tipo", "var").put("nombre", nombre)
        return JSONObject().put("tipo", op).put("a", a).put("b", valor)
    }

    private fun editarBloque(b: JSONObject) {
        val tipo = b.optString("tipo")
        val esCondicion = tipo == "si" || tipo == "esperar_hasta"
        val caja = LinearLayout(this)
        caja.orientation = LinearLayout.VERTICAL
        caja.setPadding(48, 16, 48, 0)
        val camposNum = LinkedHashMap<String, EditText>()
        val camposTxt = LinkedHashMap<String, EditText>()
        for (p in numericos[tipo] ?: emptyList()) {
            val v = b.opt(p)
            val calculado = v is JSONObject
            camposNum[p] = campo(caja, p, if (calculado) "[calculado]" else expr(v), !calculado)
        }
        for (p in textos[tipo] ?: emptyList()) {
            camposTxt[p] = campo(caja, p, b.optString(p), false)
        }
        val selector = Spinner(this)
        val cNombre = EditText(this)
        val cValor = EditText(this)
        if (esCondicion) {
            val aviso = TextView(this)
            aviso.text = "Condicion actual: " + expr(b.opt("condicion"))
            aviso.setTextColor(Color.LTGRAY)
            caja.addView(aviso)
            selector.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, condiciones)
            caja.addView(selector)
            val t1 = TextView(this)
            t1.text = "Variable u objeto"
            t1.setTextColor(Color.LTGRAY)
            caja.addView(t1)
            cNombre.setTextColor(Color.WHITE)
            caja.addView(cNombre)
            val t2 = TextView(this)
            t2.text = "Numero"
            t2.setTextColor(Color.LTGRAY)
            caja.addView(t2)
            cValor.setText("0")
            cValor.setTextColor(Color.WHITE)
            cValor.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            caja.addView(cValor)
        }
        val scroll = ScrollView(this)
        scroll.addView(caja)
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(nombres[maxOf(0, tipos.indexOf(tipo))])
            .setView(scroll)
            .setPositiveButton("Guardar") { _, _ ->
                for ((p, e) in camposNum) {
                    val d = e.text.toString().trim().toDoubleOrNull()
                    if (d != null) b.put(p, d)
                }
                for ((p, e) in camposTxt) b.put(p, e.text.toString())
                if (esCondicion) {
                    val c = construirCondicion(selector.selectedItemPosition, cNombre.text.toString().trim(), cValor.text.toString().toDoubleOrNull() ?: 0.0)
                    if (c != null) b.put("condicion", c)
                }
                guardar()
                redibujar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
