package com.starlab.pic2pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** 第一页实时预览 */
class PreviewView(c: Context) : View(c) {
    private var src: S? = null
    private var bmp: Bitmap? = null
    private var pageCount = 0
    private val ex = Executors.newSingleThreadExecutor()
    private val ver = AtomicInteger(0)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG)

    fun bind(s: S) {
        src = s
        refresh()
    }

    fun refresh() {
        val snap = src ?: return
        val v = ver.incrementAndGet()
        ex.execute {
            try {
                val plan = Compositor.plan(snap, 150f)
                pageCount = plan.size
                val first = plan.firstOrNull()
                val b = if (first != null) {
                    val scale = 620f / first.w
                    Compositor.drawPage(context, snap, first, scale, null)
                } else null
                post {
                    if (v == ver.get()) {
                        bmp = b
                        requestLayout()
                        invalidate()
                    }
                }
            } catch (t: Throwable) {
                post { invalidate() }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        if (w <= 0) {
            setMeasuredDimension(w, G.dp(context, 200f))
            return
        }
        val b = bmp
        val inner = w - G.dp(context, 40f)
        val h = if (b == null) G.dp(context, 260f).toFloat() else inner.toFloat() * b.height / b.width
        setMeasuredDimension(w, h.toInt().coerceAtMost(G.dp(context, 420f)))
    }

    override fun onDraw(canvas: Canvas) {
        val b = bmp
        if (b == null) {
            p.color = 0x33FFFFFF
            val r = RectF(G.dp(context, 20f).toFloat(), G.dp(context, 20f).toFloat(), width - G.dp(context, 20f).toFloat(), height - G.dp(context, 20f).toFloat())
            canvas.drawRoundRect(r, G.dp(context, 14f).toFloat(), G.dp(context, 14f).toFloat(), p)
            p.color = G.ink3
            p.textSize = G.dp(context, 13f).toFloat()
            p.textAlign = Paint.Align.CENTER
            canvas.drawText("还没有图片，选几张试试", width / 2f, height / 2f, p)
            return
        }
        val pad = G.dp(context, 20f).toFloat()
        val availW = width - pad * 2
        val availH = height - pad
        var dw = availW
        var dh = dw * b.height / b.width
        if (dh > availH) {
            dh = availH
            dw = dh * b.width / b.height
        }
        val l = (width - dw) / 2f
        val t = pad / 2f
        shadow.color = 0x1A000000
        canvas.drawRoundRect(RectF(l + 2, t + 6, l + dw + 2, t + dh + 6), G.dp(context, 8f).toFloat(), G.dp(context, 8f).toFloat(), shadow)
        p.reset()
        p.isAntiAlias = true
        canvas.drawBitmap(b, null, RectF(l, t, l + dw, t + dh), p)
    }
}

