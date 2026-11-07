package com.chispa.engine

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
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
    private val claves = arrayOf("num", "var", "azar", "suma", "resta", "mult", "div", "toque_x", "toque_y", "temporizador", "mayor", "menor", "igual", "tocando_pantalla", "tocando_objeto", "y", "o", "no")
    private val etiquetasExpr = arrayOf("Numero", "Variable", "Azar entre A y B", "Suma A + B", "Resta A - B", "Multiplicacion A x B", "Division A / B", "Posicion X del toque", "Posicion Y del toque", "Temporizador (segundos)", "Condicion: A mayor que B", "Condicion: A menor que B", "Condicion: A igual a B", "Condicion: tocando la pantalla", "Condicion: tocando un objeto", "Condicion: A y B", "Condicion: A o B", "Condicion: no A")

    private val tipos = arrayOf("mover", "ir_a", "rotar", "esperar", "repetir", "por_siempre", "asignar", "cambiar", "si", "esperar_hasta", "mostrar_texto", "borrar_texto", "reproducir_sonido", "ir_a_escena", "detener_script", "enviar_mensaje", "siguiente_disfraz", "cambiar_disfraz")
    private val nombres = arrayOf("Mover pasos", "Ir a x, y", "Rotar grados", "Esperar segundos", "Repetir veces", "Por siempre", "Asignar variable", "Cambiar variable", "Si / sino", "Esperar hasta que", "Mostrar texto", "Borrar texto", "Reproducir sonido", "Ir a escena", "Detener este script", "Enviar mensaje", "Siguiente disfraz", "Cambiar a disfraz numero")
    private val condiciones = arrayOf("(dejar la condicion actual)", "Variable mayor que numero", "Variable menor que numero", "Variable igual a numero", "Tocando la pantalla", "Tocando objeto")
    private val numericos = mapOf(
        "mover" to listOf("pasos"), "ir_a" to listOf("x", "y"), "rotar" to listOf("grados"),
        "esperar" to listOf("segundos"), "repetir" to listOf("veces"),
        "asignar" to listOf("valor"), "cambiar" to listOf("valor"),
        "mostrar_texto" to listOf("x", "y", "tamano"),
        "cambiar_disfraz" to listOf("numero")
    )
    private val textos = mapOf(
        "asignar" to listOf("variable"), "cambiar" to listOf("variable"),
        "mostrar_texto" to listOf("texto", "id", "color", "alineacion", "estilo", "fuente"), "borrar_texto" to listOf("id"), "enviar_mensaje" to listOf("mensaje"), "reproducir_sonido" to listOf("sonido"),
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
        "enviar_mensaje" -> "Enviar mensaje: " + b.optString("mensaje")
        "siguiente_disfraz" -> "Siguiente disfraz"
        "cambiar_disfraz" -> "Cambiar a disfraz " + expr(b.opt("numero"))
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
            t.text = etiquetaCompleta(s.optJSONObject("evento"))
            t.textSize = 18f
            t.setTextColor(Color.parseColor("#FF8C1A"))
            t.setOnClickListener { editarEvento(s) }
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
            "mostrar_texto" -> {
                b.put("texto", "Hola")
                b.put("x", 0)
                b.put("y", 40)
                b.put("tamano", 60)
                b.put("color", "#FFFFFF")
            }
            "reproducir_sonido" -> b.put("sonido", "moneda.wav")
            "ir_a_escena" -> b.put("escena", "menu")
            "enviar_mensaje" -> b.put("mensaje", "inicio")
            "cambiar_disfraz" -> b.put("numero", 1)
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



    private fun opcionesDe(p: String): List<String> {
        if (p == "alineacion") return listOf("izquierda", "centro", "derecha")
        if (p == "estilo") return listOf("normal", "negrita", "sombra", "contorno")
        val l = ArrayList<String>()
        l.add("(predeterminada)")
        val fuentes = File(dir, "assets/fuentes").listFiles()
        if (fuentes != null) l.addAll(fuentes.filter { it.isFile }.map { it.name }.sorted())
        return l
    }

    private fun comboCaja(padre: LinearLayout, etiqueta: String, opciones: List<String>, actual: String): Spinner {
        val t = TextView(this)
        t.text = etiqueta
        t.setTextColor(Color.LTGRAY)
        padre.addView(t)
        val ops = if (actual.isNotEmpty() && !opciones.contains(actual)) opciones + actual else opciones
        val s = Spinner(this)
        s.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ops)
        val i = ops.indexOf(actual)
        s.setSelection(if (i >= 0) i else 0)
        padre.addView(s)
        return s
    }

    private fun botonValor(nombre: String, valores: LinkedHashMap<String, Any?>): Button {
        val btn = Button(this)
        btn.setAllCaps(false)
        btn.text = nombre + ": " + expr(valores[nombre])
        btn.setOnClickListener {
            editarExpr(nombre, valores[nombre]) { r ->
                valores[nombre] = r
                btn.text = nombre + ": " + expr(r)
            }
        }
        return btn
    }

    private fun editarBloque(b: JSONObject) {
        val tipo = b.optString("tipo")
        val caja = LinearLayout(this)
        caja.orientation = LinearLayout.VERTICAL
        caja.setPadding(48, 16, 48, 0)
        val valores = LinkedHashMap<String, Any?>()
        val nombresValor = ArrayList<String>()
        nombresValor.addAll(numericos[tipo] ?: emptyList())
        if (tipo == "si" || tipo == "esperar_hasta") nombresValor.add("condicion")
        for (p in nombresValor) {
            valores[p] = b.opt(p)
            caja.addView(botonValor(p, valores))
        }
        val camposTxt = LinkedHashMap<String, EditText>()
        val combosTxt = LinkedHashMap<String, Spinner>()
        for (p in textos[tipo] ?: emptyList()) {
            if (p == "alineacion" || p == "estilo" || p == "fuente") {
                combosTxt[p] = comboCaja(caja, p, opcionesDe(p), b.optString(p))
            } else {
                camposTxt[p] = campo(caja, p, b.optString(p), false)
            }
        }
        val scroll = ScrollView(this)
        scroll.addView(caja)
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(nombres[maxOf(0, tipos.indexOf(tipo))])
            .setView(scroll)
            .setPositiveButton("Guardar") { _, _ ->
                for ((k, v) in valores) {
                    if (v != null) b.put(k, v)
                }
                for ((p, e) in camposTxt) b.put(p, e.text.toString())
                for ((p, s) in combosTxt) {
                    val v = s.selectedItem as String
                    b.put(p, if (v == "(predeterminada)") "" else v)
                }
                guardar()
                redibujar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun claveDe(v: Any?): String {
        if (v is JSONObject) {
            val t = v.optString("tipo")
            return if (claves.contains(t)) t else "num"
        }
        return "num"
    }

    private fun variablesConocidas(): String {
        val r = java.util.TreeSet<String>()
        val re = Regex("\"variable\"\\s*:\\s*\"([^\"]+)\"")
        val escenas = File(dir, "escenas").listFiles()
        if (escenas != null) {
            for (f in escenas) {
                for (m in re.findAll(f.readText())) r.add(m.groupValues[1])
            }
        }
        val pj = JSONObject(File(dir, "proyecto.json").readText())
        val arr = pj.optJSONArray("variablesGlobales")
        if (arr != null) {
            for (i in 0 until arr.length()) r.add(arr.getJSONObject(i).optString("nombre"))
        }
        return if (r.isEmpty()) "ninguna" else r.joinToString(", ")
    }

    private fun editarExpr(titulo: String, actual: Any?, alAceptar: (Any) -> Unit) {
        val cuerpo = LinearLayout(this)
        cuerpo.orientation = LinearLayout.VERTICAL
        cuerpo.setPadding(48, 16, 48, 0)
        val selector = Spinner(this)
        selector.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, etiquetasExpr)
        cuerpo.addView(selector)
        val zona = LinearLayout(this)
        zona.orientation = LinearLayout.VERTICAL
        cuerpo.addView(zona)

        var numero = if (actual is Number) num(actual.toDouble()) else "0"
        var nombre = ""
        var a: Any? = 0.0
        var b: Any? = 0.0
        if (actual is JSONObject) {
            nombre = if (actual.has("nombre")) actual.optString("nombre") else actual.optString("objeto")
            a = if (actual.has("min")) actual.opt("min") else actual.opt("a")
            b = if (actual.has("max")) actual.opt("max") else actual.opt("b")
            if (a == null) a = 0.0
            if (b == null) b = 0.0
        }
        var etTexto: EditText? = null

        fun reconstruir(pos: Int) {
            zona.removeAllViews()
            etTexto = null
            val clave = claves[pos]
            if (clave == "num" || clave == "var" || clave == "tocando_objeto") {
                val e = EditText(this)
                e.setTextColor(Color.WHITE)
                if (clave == "num") {
                    e.setText(numero)
                    e.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                } else if (clave == "var") {
                    e.setText(nombre)
                    e.hint = "Variables: " + variablesConocidas()
                } else {
                    e.setText(nombre)
                    e.hint = "Nombre del objeto"
                }
                zona.addView(e)
                etTexto = e
            } else if (clave == "toque_x" || clave == "toque_y" || clave == "temporizador" || clave == "tocando_pantalla") {
                val t = TextView(this)
                t.text = "Sin parametros."
                t.setTextColor(Color.LTGRAY)
                zona.addView(t)
            } else {
                val bA = Button(this)
                bA.setAllCaps(false)
                bA.text = "A: " + expr(a)
                bA.setOnClickListener {
                    editarExpr("A", a) { r ->
                        a = r
                        bA.text = "A: " + expr(r)
                    }
                }
                zona.addView(bA)
                if (clave != "no") {
                    val bB = Button(this)
                    bB.setAllCaps(false)
                    bB.text = "B: " + expr(b)
                    bB.setOnClickListener {
                        editarExpr("B", b) { r ->
                            b = r
                            bB.text = "B: " + expr(r)
                        }
                    }
                    zona.addView(bB)
                }
            }
        }

        val inicial = maxOf(0, claves.indexOf(claveDe(actual)))
        selector.setSelection(inicial)
        reconstruir(inicial)
        selector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(padre: AdapterView<*>?, vista: View?, pos: Int, id: Long) {
                reconstruir(pos)
            }

            override fun onNothingSelected(padre: AdapterView<*>?) {
            }
        }
        val scroll = ScrollView(this)
        scroll.addView(cuerpo)
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Valor: " + titulo)
            .setView(scroll)
            .setPositiveButton("Aceptar") { _, _ ->
                val clave = claves[selector.selectedItemPosition]
                val txt = etTexto?.text?.toString()?.trim() ?: ""
                val r: Any = when (clave) {
                    "num" -> txt.toDoubleOrNull() ?: 0.0
                    "var" -> JSONObject().put("tipo", "var").put("nombre", txt)
                    "tocando_objeto" -> JSONObject().put("tipo", "tocando_objeto").put("objeto", txt)
                    "azar" -> JSONObject().put("tipo", "azar").put("min", a ?: 0.0).put("max", b ?: 0.0)
                    "suma", "resta", "mult", "div", "mayor", "menor", "igual", "y", "o" ->
                        JSONObject().put("tipo", clave).put("a", a ?: 0.0).put("b", b ?: 0.0)
                    "no" -> JSONObject().put("tipo", "no").put("a", a ?: 0.0)
                    else -> JSONObject().put("tipo", clave)
                }
                alAceptar(r)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun etiquetaCompleta(ev: JSONObject?): String {
        val t = ev?.optString("tipo", "") ?: ""
        val p = ev?.optString("param", "") ?: ""
        return when (t) {
            "al_iniciar" -> "Al iniciar el juego"
            "al_tocar_pantalla" -> "Al tocar la pantalla"
            "al_tocar_objeto" -> "Al tocar este objeto"
            "al_recibir_mensaje" -> "Al recibir el mensaje: " + p
            "al_colisionar" -> "Al chocar con: " + p
            else -> t
        }
    }

    private fun elegirEvento() {
        val ids = arrayOf("al_iniciar", "al_tocar_pantalla", "al_tocar_objeto", "al_recibir_mensaje", "al_colisionar")
        val etiquetas = arrayOf("Al iniciar el juego", "Al tocar la pantalla", "Al tocar este objeto", "Al recibir un mensaje...", "Al chocar con otro objeto...")
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Nuevo script: cuando...")
            .setItems(etiquetas) { _, cual ->
                if (cual < 3) {
                    agregarScript(ids[cual], "")
                } else {
                    pedirParametro(ids[cual])
                }
            }
            .show()
    }

    private fun agregarScript(tipo: String, param: String) {
        val ev = JSONObject().put("tipo", tipo)
        if (param.isNotEmpty()) ev.put("param", param)
        val s = JSONObject()
        s.put("evento", ev)
        s.put("bloques", JSONArray())
        lista(objeto, "scripts").put(s)
        guardar()
        redibujar()
    }

    private fun pedirParametro(tipo: String) {
        val campo = EditText(this)
        campo.setTextColor(Color.WHITE)
        campo.hint = if (tipo == "al_colisionar") "Nombre del otro objeto" else "Nombre del mensaje"
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(if (tipo == "al_colisionar") "Chocar con..." else "Recibir mensaje...")
            .setView(campo)
            .setPositiveButton("Aceptar") { _, _ ->
                agregarScript(tipo, campo.text.toString().trim())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editarEvento(s: JSONObject) {
        val ev = s.optJSONObject("evento") ?: return
        val tipo = ev.optString("tipo", "")
        if (tipo != "al_recibir_mensaje" && tipo != "al_colisionar") return
        val campo = EditText(this)
        campo.setTextColor(Color.WHITE)
        campo.setText(ev.optString("param", ""))
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(if (tipo == "al_colisionar") "Chocar con..." else "Recibir mensaje...")
            .setView(campo)
            .setPositiveButton("Aceptar") { _, _ ->
                ev.put("param", campo.text.toString().trim())
                guardar()
                redibujar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
