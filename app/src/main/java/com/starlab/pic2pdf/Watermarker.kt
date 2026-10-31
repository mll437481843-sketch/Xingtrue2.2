package com.starlab.pic2pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.max

object Watermarker {

    fun draw(c: Context, cv: Canvas, s: S, W: Int, H: Int) {
        if (s.invisOn && s.invisText.isNotBlank()) {
            drawHidden(cv, s, W, H)
        }
        if (!s.wmOn) return
        if (s.wmText.isNotBlank()) drawText(cv, s, W, H, s.wmText)
        drawLogo(c, cv, s, W, H)
    }

    private fun isLight(color: Int): Boolean {
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        return (0.299 * r + 0.587 * g + 0.114 * b) > 140
    }

    /** 同底色墨水：肉眼几乎不可见，取色/OCR/逆向可读 */
    private fun drawHidden(cv: Canvas, s: S, W: Int, H: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.typeface = Compositor.typeface(s)
        p.textSize = W * 0.016f
        p.color = if (isLight(s.bg)) darken(s.bg) else lighten(s.bg)
        p.alpha = 12
        p.textAlign = Paint.Align.CENTER
        val txt = s.invisText + " · " + s.title
        // 页底留白处一行
        cv.drawText(txt, W / 2f, H - max(W * 0.008f, 6f), p)
        // 左边缘竖排一行
        cv.save()
        cv.rotate(-90f, 0f, 0f)
        cv.drawText(txt, 0f, -max(W * 0.008f, 6f), p)
        cv.restore()
    }

    private fun darken(c: Int): Int = Color.rgb(
        (Color.red(c) * 0.80).toInt().coerceIn(0, 255),
        (Color.green(c) * 0.80).toInt().coerceIn(0, 255),
        (Color.blue(c) * 0.80).toInt().coerceIn(0, 255)
    )

    private fun lighten(c: Int): Int = Color.rgb(
        (Color.red(c) + (255 - Color.red(c)) * 0.85).toInt().coerceIn(0, 255),
        (Color.green(c) + (255 - Color.green(c)) * 0.85).toInt().coerceIn(0, 255),
        (Color.blue(c) + (255 - Color.blue(c)) * 0.85).toInt().coerceIn(0, 255)
    )

    private fun drawText(cv: Canvas, s: S, W: Int, H: Int, text: String) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.typeface = Compositor.typeface(s)
        p.textSize = W * s.wmSize / 100f
        p.color = s.wmColor
        p.alpha = (s.wmAlpha.coerceIn(0f, 1f) * 255).toInt()
        val tw = p.measureText(text)
        val th = p.textSize

        when (s.wmLayout) {
            "tile" -> {
                cv.save()
                cv.rotate(s.wmAngle, W / 2f, H / 2f)
                val stepX = tw + th * 1.5f
                val stepY = th * 3.4f
                var y = -H.toFloat()
                while (y < H * 2f) {
                    var x = -W.toFloat()
                    while (x < W * 2f) {
                        cv.drawText(text, x, y, p)
                        x += stepX
                    }
                    y += stepY
                }
                cv.restore()
            }
            "center" -> {
                cv.save()
                cv.rotate(s.wmAngle, W / 2f, H / 2f)
                p.textAlign = Paint.Align.CENTER
                cv.drawText(text, W / 2f, H / 2f + th / 3f, p)
                cv.restore()
            }
            "corner" -> {
                p.textAlign = Paint.Align.LEFT
                val m = W * 0.03f
                cv.drawText(text, m, m + th, p)
                cv.save(); cv.rotate(180f, W / 2f, H / 2f)
                cv.drawText(text, m, m + th, p)
                cv.restore()
                cv.save(); cv.rotate(90f, W / 2f, H / 2f)
                cv.drawText(text, m, m + th, p)
                cv.restore()
            }
            "bottom" -> {
                p.textAlign = Paint.Align.CENTER
                cv.drawText(text, W / 2f, H - max(W * 0.015f, th * 0.4f), p)
            }
            else -> {
                cv.save()
                cv.rotate(s.wmAngle, W / 2f, H / 2f)
                p.textAlign = Paint.Align.CENTER
                var k = -2
                while (k <= 2) {
                    cv.drawText(text, W / 2f, H / 2f + k * th * 1.8f, p)
                    k++
                }
                cv.restore()
            }
        }
    }

    private fun drawLogo(c: Context, cv: Canvas, s: S, W: Int, H: Int) {
        val uri = s.logoUri ?: return
        val b = Imgs.loadAny(c, uri, (W * 0.6f).toInt()) ?: return
        val lw = W * s.logoScale / 100f
        val lh = lw * b.height / max(1, b.width)
        val m = W * 0.035f
        val l = when (s.logoPos) {
            "bl", "tl" -> m
            "center" -> (W - lw) / 2f
            else -> W - m - lw
        }
        val t = when (s.logoPos) {
            "tl", "tr" -> m
            "center" -> (H - lh) / 2f
            else -> H - m - lh
        }
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.alpha = (s.logoAlpha.coerceIn(0f, 1f) * 255).toInt()
        cv.drawBitmap(b, Rect(0, 0, b.width, b.height), RectF(l, t, l + lw, t + lh), p)
        b.recycle()
    }

    fun payload(s: S): String {
        val sb = StringBuilder()
        sb.append("text=").append(s.invisText)
        sb.append("|title=").append(s.title)
        sb.append("|pics=").append(s.pics.size)
        sb.append("|t=").append(System.currentTimeMillis())
        sb.append("|d=").append(android.os.Build.MODEL)
        return sb.toString()
    }
}

