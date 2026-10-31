package com.starlab.pic2pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class Backdrop(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f
    private var base: LinearGradient? = null
    private var r1: RadialGradient? = null
    private var r2: RadialGradient? = null
    private var r3: RadialGradient? = null
    private var lastW = 0
    private var lastH = 0

    init {
        if (Build.VERSION.SDK_INT >= 31) {
            try { setRenderEffect(android.graphics.RenderEffect.createBlurEffect(54f, 54f, Shader.TileMode.CLAMP)) } catch (t: Throwable) {}
        }
    }

    private fun build(w: Int, h: Int) {
        base = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(0xFFF2F6FF.toInt(), 0xFFFBF3FB.toInt(), 0xFFEDF8FF.toInt()),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        r1 = RadialGradient(w * 0.22f, h * 0.18f, w * 0.62f,
            intArrayOf(0x99A9C9FF.toInt(), 0x0099BBFF), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        r2 = RadialGradient(w * 0.86f, h * 0.32f, w * 0.58f,
            intArrayOf(0x8CFFC2E4.toInt(), 0x00FFC2E4), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        r3 = RadialGradient(w * 0.50f, h * 0.88f, w * 0.66f,
            intArrayOf(0x8CB6F5E6.toInt(), 0x00B6F5E6), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        lastW = w; lastH = h
    }

    override fun onDraw(canvas: Canvas) {
        val w = width; val h = height
        if (w == 0 || h == 0) return
        if (w != lastW || h != lastH) build(w, h)
        p.shader = base
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        val a = 18f
        val t1 = t * 0.00022f
        val t2 = t * 0.00017f + 2f
        val t3 = t * 0.00013f + 4f
        p.shader = r1
        canvas.save(); canvas.translate(cos(t1) * a, sin(t1 * 1.3f) * a); canvas.drawPaint(p); canvas.restore()
        p.shader = r2
        canvas.save(); canvas.translate(cos(t2) * a * 1.4f, sin(t2) * a); canvas.drawPaint(p); canvas.restore()
        p.shader = r3
        canvas.save(); canvas.translate(cos(t3) * a, sin(t3 * 1.1f) * a * 1.2f); canvas.drawPaint(p); canvas.restore()
        p.shader = null
        postInvalidateDelayed(80)
    }
}

