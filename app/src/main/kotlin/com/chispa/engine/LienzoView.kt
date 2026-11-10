package com.chispa.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import org.json.JSONObject
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class LienzoView(
    contexto: Context,
    private val dir: File,
    private val escena: JSONObject,
    private val alGuardar: () -> Unit,
    private val alSeleccionar: (JSONObject?) -> Unit
) : View(contexto) {

    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmaps = HashMap<String, Bitmap?>()
    var seleccionado: JSONObject? = null
        private set
    var cuadricula = false
        set(valor) {
            field = valor
            invalidate()
        }
    var mx = 640f
    var my = 360f
    private var s = 1f
    private var cx = 0f
    private var cy = 0f
    private var modo = 0
    private var offX = 0f
    private var offY = 0f
    private var esc0 = 1f
    private var d0 = 1f
    private var rot0 = 0f
    private var ang0 = 0f
    private var scx = 0f
    private var scy = 0f

    private fun num(o: JSONObject, k: String, d: Double): Float = o.optDouble(k, d).toFloat()

    private fun colorDe(o: JSONObject): Int {
        return try {
            Color.parseColor(o.optString("color", "#FFFFFF"))
        } catch (ex: Exception) {
            Color.WHITE
        }
    }

    private fun bitmap(nombre: String): Bitmap? {
        if (!bitmaps.containsKey(nombre)) {
            bitmaps[nombre] = BitmapFactory.decodeFile(File(dir, "assets/imagenes/" + nombre).path)
        }
        return bitmaps[nombre]
    }

    private fun ordenados(): List<JSONObject> {
        val arr = escena.getJSONArray("objetos")
        val l = ArrayList<JSONObject>()
        for (i in 0 until arr.length()) l.add(arr.getJSONObject(i))
        return l.sortedBy { it.optInt("capa", 0) }
    }

    private fun cajaLocal(o: JSONObject): FloatArray {
        val texto = o.optString("texto", "")
        if (texto.isNotEmpty()) {
            val tam = num(o, "tamTexto", 40.0)
            pincel.textSize = tam
            val w = maxOf(tam, pincel.measureText(texto))
            val izq = when (o.optString("alinTexto", "centro")) {
                "izquierda" -> 0f
                "derecha" -> -w
                else -> -w / 2f
            }
            return floatArrayOf(izq, -tam / 2f, izq + w, tam / 2f)
        }
        val t = o.optJSONObject("tamano")
        val w = (t?.optDouble("ancho", 100.0) ?: 100.0).toFloat()
        val h = (t?.optDouble("alto", 100.0) ?: 100.0).toFloat()
        return floatArrayOf(-w / 2f, -h / 2f, w / 2f, h / 2f)
    }

    private fun aPantalla(o: JSONObject, lx: Float, ly: Float): FloatArray {
        val k = s * num(o, "escala", 1.0)
        val phi = Math.toRadians((-num(o, "rotacion", 0.0)).toDouble())
        val px = lx * k
        val py = ly * k
        val rx = (px * cos(phi) - py * sin(phi)).toFloat()
        val ry = (px * sin(phi) + py * cos(phi)).toFloat()
        return floatArrayOf(cx + num(o, "x", 0.0) * s + rx, cy - num(o, "y", 0.0) * s + ry)
    }

    private fun contiene(o: JSONObject, tx: Float, ty: Float): Boolean {
        val k = s * num(o, "escala", 1.0)
        if (k <= 0f) return false
        val phi = Math.toRadians((-num(o, "rotacion", 0.0)).toDouble())
        val dx = tx - (cx + num(o, "x", 0.0) * s)
        val dy = ty - (cy - num(o, "y", 0.0) * s)
        val lx = ((dx * cos(phi) + dy * sin(phi)) / k).toFloat()
        val ly = ((-dx * sin(phi) + dy * cos(phi)) / k).toFloat()
        val b = cajaLocal(o)
        return lx >= b[0] && lx <= b[2] && ly >= b[1] && ly <= b[3]
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(Color.parseColor("#0F1722"))
        s = minOf(width / (mx * 2.2f), height / (my * 2.2f))
        cx = width / 2f
        cy = height / 2f
        val fondo = escena.optJSONObject("fondo")
        pincel.style = Paint.Style.FILL
        pincel.color = try {
            Color.parseColor(fondo?.optString("color", "#1B2A41") ?: "#1B2A41")
        } catch (ex: Exception) {
            Color.DKGRAY
        }
        c.drawRect(cx - mx * s, cy - my * s, cx + mx * s, cy + my * s, pincel)
        if (cuadricula) dibujarCuadricula(c)
        pincel.style = Paint.Style.STROKE
        pincel.strokeWidth = 2f
        pincel.color = Color.argb(90, 255, 255, 255)
        c.drawLine(cx - mx * s, cy, cx + mx * s, cy, pincel)
        c.drawLine(cx, cy - my * s, cx, cy + my * s, pincel)
        for (o in ordenados()) dibujar(c, o)
        val sel = seleccionado
        if (sel != null) dibujarSeleccion(c, sel)
    }

    private fun dibujarCuadricula(c: Canvas) {
        pincel.style = Paint.Style.STROKE
        pincel.strokeWidth = 1f
        pincel.color = Color.argb(50, 255, 255, 255)
        var gx = -mx.toInt()
        while (gx <= mx.toInt()) {
            c.drawLine(cx + gx * s, cy - my * s, cx + gx * s, cy + my * s, pincel)
            gx += 80
        }
        var gy = -my.toInt()
        while (gy <= my.toInt()) {
            c.drawLine(cx - mx * s, cy + gy * s, cx + mx * s, cy + gy * s, pincel)
            gy += 80
        }
    }

    private fun dibujar(c: Canvas, o: JSONObject) {
        val esc = num(o, "escala", 1.0)
        val visible = o.optBoolean("visible", true)
        val b = cajaLocal(o)
        c.save()
        c.translate(cx + num(o, "x", 0.0) * s, cy - num(o, "y", 0.0) * s)
        c.rotate(-num(o, "rotacion", 0.0))
        c.scale(s * esc, s * esc)
        pincel.style = Paint.Style.FILL
        val texto = o.optString("texto", "")
        if (texto.isNotEmpty()) {
            pincel.color = colorDe(o)
            pincel.textSize = num(o, "tamTexto", 40.0)
            pincel.textAlign = when (o.optString("alinTexto", "centro")) {
                "izquierda" -> Paint.Align.LEFT
                "derecha" -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            if (!visible) pincel.alpha = 90
            c.drawText(texto, 0f, -(pincel.ascent() + pincel.descent()) / 2f, pincel)
        } else {
            val nombre = nombreImagen(o)
            val bmp = if (nombre.isEmpty()) null else bitmap(nombre)
            if (bmp != null) {
                pincel.color = Color.WHITE
                pincel.isFilterBitmap = false
                if (!visible) pincel.alpha = 90
                c.drawBitmap(bmp, null, RectF(b[0], b[1], b[2], b[3]), pincel)
            } else {
                pincel.color = colorDe(o)
                if (!visible) pincel.alpha = 90
                c.drawRect(b[0], b[1], b[2], b[3], pincel)
            }
        }
        pincel.alpha = 255
        pincel.textAlign = Paint.Align.LEFT
        c.restore()
    }

    private fun manijaRotar(o: JSONObject): FloatArray {
        val b = cajaLocal(o)
        val mx = (b[0] + b[2]) / 2f
        val centro = aPantalla(o, mx, 0f)
        val top = aPantalla(o, mx, b[1])
        var vx = top[0] - centro[0]
        var vy = top[1] - centro[1]
        val l = hypot(vx, vy)
        if (l < 1f) {
            vx = 0f
            vy = -1f
        } else {
            vx /= l
            vy /= l
        }
        return floatArrayOf(top[0] + vx * 56f, top[1] + vy * 56f)
    }

    private fun dibujarSeleccion(c: Canvas, o: JSONObject) {
        val b = cajaLocal(o)
        val p1 = aPantalla(o, b[0], b[1])
        val p2 = aPantalla(o, b[2], b[1])
        val p3 = aPantalla(o, b[2], b[3])
        val p4 = aPantalla(o, b[0], b[3])
        val ruta = Path()
        ruta.moveTo(p1[0], p1[1])
        ruta.lineTo(p2[0], p2[1])
        ruta.lineTo(p3[0], p3[1])
        ruta.lineTo(p4[0], p4[1])
        ruta.close()
        pincel.style = Paint.Style.STROKE
        pincel.strokeWidth = 4f
        pincel.color = Color.parseColor("#FFD93D")
        c.drawPath(ruta, pincel)
        val h = manijaRotar(o)
        val top = aPantalla(o, (b[0] + b[2]) / 2f, b[1])
        c.drawLine(top[0], top[1], h[0], h[1], pincel)
        pincel.style = Paint.Style.FILL
        pincel.color = Color.parseColor("#FF8C1A")
        c.drawCircle(p3[0], p3[1], 22f, pincel)
        pincel.color = Color.parseColor("#3FA7FF")
        c.drawCircle(h[0], h[1], 22f, pincel)
    }

    private fun angulo(tx: Float, ty: Float): Float {
        return Math.toDegrees(atan2((scy - ty).toDouble(), (tx - scx).toDouble())).toFloat()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val tx = e.x
        val ty = e.y
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                modo = 0
                val sel = seleccionado
                if (sel != null) {
                    val b = cajaLocal(sel)
                    val esq = aPantalla(sel, b[2], b[3])
                    val rot = manijaRotar(sel)
                    val centro = aPantalla(sel, 0f, 0f)
                    scx = centro[0]
                    scy = centro[1]
                    if (hypot(tx - esq[0], ty - esq[1]) < 50f) {
                        modo = 2
                        esc0 = num(sel, "escala", 1.0)
                        d0 = maxOf(1f, hypot(tx - scx, ty - scy))
                    } else if (hypot(tx - rot[0], ty - rot[1]) < 50f) {
                        modo = 3
                        rot0 = num(sel, "rotacion", 0.0)
                        ang0 = angulo(tx, ty)
                    }
                }
                if (modo == 0) {
                    var golpe: JSONObject? = null
                    val lista = ordenados()
                    for (i in lista.indices.reversed()) {
                        if (contiene(lista[i], tx, ty)) {
                            golpe = lista[i]
                            break
                        }
                    }
                    seleccionado = golpe
                    if (golpe != null) {
                        modo = 1
                        offX = (tx - cx) / s - num(golpe, "x", 0.0)
                        offY = (cy - ty) / s - num(golpe, "y", 0.0)
                    }
                    alSeleccionar(golpe)
                }
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val sel = seleccionado
                if (sel != null && modo != 0) {
                    if (modo == 1) {
                        var nx = (tx - cx) / s - offX
                        var ny = (cy - ty) / s - offY
                        if (cuadricula) {
                            nx = Math.round(nx / 10f) * 10f
                            ny = Math.round(ny / 10f) * 10f
                        }
                        sel.put("x", nx.toDouble())
                        sel.put("y", ny.toDouble())
                    } else if (modo == 2) {
                        val d = hypot(tx - scx, ty - scy)
                        sel.put("escala", maxOf(0.05f, minOf(30f, esc0 * d / d0)).toDouble())
                    } else {
                        var r = rot0 + (angulo(tx, ty) - ang0)
                        if (cuadricula) r = Math.round(r / 5f) * 5f
                        sel.put("rotacion", r.toDouble())
                    }
                    alSeleccionar(sel)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (modo != 0) alGuardar()
                modo = 0
            }
        }
        return true
    }

    fun duplicar() {
        val sel = seleccionado ?: return
        val copia = JSONObject(sel.toString())
        copia.put("id", "obj_" + System.currentTimeMillis())
        copia.put("nombre", sel.optString("nombre", "Objeto") + " copia")
        copia.put("x", sel.optDouble("x", 0.0) + 40)
        copia.put("y", sel.optDouble("y", 0.0) - 40)
        escena.getJSONArray("objetos").put(copia)
        seleccionado = copia
        alGuardar()
        alSeleccionar(copia)
        invalidate()
    }

    fun borrar() {
        val sel = seleccionado ?: return
        val arr = escena.getJSONArray("objetos")
        for (i in 0 until arr.length()) {
            if (arr.getJSONObject(i) === sel) {
                arr.remove(i)
                break
            }
        }
        seleccionado = null
        alGuardar()
        alSeleccionar(null)
        invalidate()
    }

    fun capa(d: Int) {
        val sel = seleccionado ?: return
        sel.put("capa", sel.optInt("capa", 0) + d)
        alGuardar()
        alSeleccionar(sel)
        invalidate()
    }

    private fun nombreImagen(o: JSONObject): String {
        val d = o.optJSONArray("disfraces")
        if (d != null && d.length() > 0) return d.optString(0, "")
        return o.optString("imagen", "")
    }
}
