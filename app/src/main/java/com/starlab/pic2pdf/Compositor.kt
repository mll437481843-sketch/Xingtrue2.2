package com.starlab.pic2pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class PPage(val items: List<Int>, val w: Int, val h: Int)

object Compositor {

    const val DPI_REF = 150f

    fun typeface(s: S): Typeface {
        s.wmFontPath?.let { p ->
            try { return Typeface.createFromFile(p) } catch (t: Throwable) {}
        }
        val fam = when (s.wmFont) {
            "serif" -> "serif"
            "mono" -> "monospace"
            "light" -> "sans-serif-light"
            "medium" -> "sans-serif-medium"
            "black" -> "sans-serif-black"
            "cond" -> "sans-serif-condensed"
            "cursive" -> "cursive"
            "casual" -> "casual"
            "serifmono" -> "serif-monospace"
            "bold" -> "sans-serif"
            else -> "sans-serif"
        }
        val style = if (s.wmFont == "bold") Typeface.BOLD else Typeface.NORMAL
        return Typeface.create(fam, style)
    }

    /** 页面尺寸（像素）*/
    fun baseSize(s: S, dpi: Float): Pair<Float, Float> {
        val inch = dpi
        return when (s.ratio) {
            "a4l" -> Pair(11.69f * inch, 8.27f * inch)
            "letter" -> Pair(8.5f * inch, 11.0f * inch)
            "16x9" -> Pair(13.33f * inch, 7.5f * inch)
            "1x1" -> Pair(8f * inch, 8f * inch)
            "auto" -> {
                val p = s.pics.firstOrNull()
                if (p != null && p.w > 0 && p.h > 0) {
                    val bw = 8.27f * inch
                    Pair(bw, bw * p.h / p.w)
                } else Pair(8.27f * inch, 11.69f * inch)
            }
            else -> Pair(8.27f * inch, 11.69f * inch)
        }
    }

    fun cols(n: Int): Int = when (n) {
        1 -> 1
        2 -> 2
        3 -> 3
        4 -> 2
        6 -> 3
        9 -> 3
        else -> ceil(Math.sqrt(n.toDouble())).toInt()
    }

    /** 生成页计划（以 dpi=150 的像素坐标计算比例，再按实际 dpi 缩放）*/
    fun plan(s: S, dpi: Float): List<PPage> {
        val (bw, bh) = baseSize(s, dpi)
        val W = bw.toInt().coerceAtLeast(64)
        val H = bh.toInt().coerceAtLeast(64)
        val out = ArrayList<PPage>()
        if (s.pics.isEmpty()) return out
        when (s.mode) {
            "grid" -> {
                val n = s.perPage.coerceAtLeast(1)
                var i = 0
                while (i < s.pics.size) {
                    val idx = ArrayList<Int>()
                    for (k in i until min(i + n, s.pics.size)) idx.add(k)
                    out.add(PPage(idx, W, H))
                    i += n
                }
            }
            "long" -> {
                // 以页宽为基准，按原图比例竖向拼接；超过高度上限则翻页
                val margin = W * s.margin / 100f
                val gap = W * s.gap / 100f
                val innerW = W - 2 * margin
                val maxH = (H * 3.2f).toInt()
                var cur = ArrayList<Int>()
                var curH = margin * 2
                for ((i, p) in s.pics.withIndex()) {
                    val hh = if (p.w > 0) innerW * p.h / p.w else innerW
                    val need = if (cur.isEmpty()) hh else hh + gap
                    if (cur.isNotEmpty() && curH + need > maxH) {
                        out.add(PPage(cur, W, curH.toInt().coerceAtLeast(64)))
                        cur = ArrayList()
                        curH = margin * 2
                    }
                    cur.add(i)
                    curH += if (cur.size == 1) hh else hh + gap
                }
                if (cur.isNotEmpty()) out.add(PPage(cur, W, curH.toInt().coerceAtLeast(64)))
            }
            else -> {
                for ((i, _) in s.pics.withIndex()) {
                    out.add(PPage(listOf(i), W, H))
                }
            }
        }
        return out
    }

    fun drawPage(c: Context, s: S, page: PPage, scale: Float, bmpOut: Bitmap?, showMarks: Boolean = true): Bitmap {
        val W = max(1, (page.w * scale).toInt())
        val H = max(1, (page.h * scale).toInt())
        val bmp = bmpOut ?: Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.drawColor(s.bg)

        val margin = W * s.margin / 100f
        val gap = W * s.gap / 100f

        if (s.mode == "grid" && page.items.size > 1) {
            val n = page.items.size
            val cs = cols(n)
            val rows = ceil(n.toDouble() / cs).toInt()
            val innerW = W - 2 * margin
            val innerH = H - 2 * margin
            val cw = (innerW - gap * (cs - 1)) / cs
            val ch = (innerH - gap * (rows - 1)) / rows
            for ((k, idx) in page.items.withIndex()) {
                val r = k / cs
                val cc = k % cs
                val l = margin + cc * (cw + gap)
                val t = margin + r * (ch + gap)
                val b = Imgs.load(c, s.pics[idx].uri, max(cw, ch).toInt().coerceAtLeast(320) * 2)
                if (b != null) {
                    drawCell(cv, b, RectF(l, t, l + cw, t + ch), s, W)
                    b.recycle()
                }
            }
        } else {
            // 单列（一页一图 / 长图拼接）
            var y = margin
            val innerW = W - 2 * margin
            for ((k, idx) in page.items.withIndex()) {
                val p = s.pics[idx]
                val hh = if (p.w > 0) innerW * p.h / p.w else innerW
                if (k > 0) y += gap
                val b = Imgs.load(c, p.uri, max(innerW, hh).toInt().coerceAtLeast(320) * 2)
                if (b != null) {
                    drawCell(cv, b, RectF(margin, y, margin + innerW, y + hh), s, W)
                    b.recycle()
                }
                y += hh
            }
        }

        if (showMarks) Watermarker.draw(c, cv, s, W, H)
        return bmp
    }

    private fun drawCell(cv: Canvas, b: Bitmap, dst: RectF, s: S, pageW: Int) {
        val r = min(dst.width(), dst.height()) * s.corner / 100f
        val path = Path()
        path.addRoundRect(dst, r, r, Path.Direction.CW)
        cv.save()
        cv.clipPath(path)
        val srcAspect = b.width.toFloat() / b.height
        val dstAspect = dst.width() / dst.height()
        val cover = s.fit == "cover"
        if ((srcAspect > dstAspect) == cover) {
            val sw = b.height * dstAspect
            val sx = (b.width - sw) / 2f
            cv.drawBitmap(b, Rect(sx.toInt(), 0, (sx + sw).toInt(), b.height), dst, null)
        } else {
            val sh = b.width / dstAspect
            val sy = (b.height - sh) / 2f
            cv.drawBitmap(b, Rect(0, sy.toInt(), b.width, (sy + sh).toInt()), dst, null)
        }
        cv.restore()
    }
}

