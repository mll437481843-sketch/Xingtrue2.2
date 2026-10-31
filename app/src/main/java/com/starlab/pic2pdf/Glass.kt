package com.starlab.pic2pdf

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable

/** 浅色液态玻璃主题：中性冷白底 + 半透明白玻璃 + 发丝高光边 + iOS 蓝点缀 */
object G {
    val ink = 0xFF11161F.toInt()
    val ink2 = 0xFF5F6878.toInt()
    val ink3 = 0xFF9AA3B2.toInt()
    val accent = 0xFF0A84FF.toInt()
    val accentSoft = 0xFFE7F1FF.toInt()
    val danger = 0xFFFF453A.toInt()

    fun dp(c: Context, v: Float): Int = (v * c.resources.displayMetrics.density + 0.5f).toInt()

    fun glass(c: Context, radius: Float = 26f): LayerDrawable {
        val fill = GradientDrawable()
        fill.shape = GradientDrawable.RECTANGLE
        fill.cornerRadius = dp(c, radius).toFloat()
        fill.colors = intArrayOf(0xE6FFFFFF.toInt(), 0x80FFFFFF.toInt())
        fill.gradientType = GradientDrawable.LINEAR_GRADIENT
        fill.orientation = GradientDrawable.Orientation.TL_BR

        val edge = GradientDrawable()
        edge.shape = GradientDrawable.RECTANGLE
        edge.cornerRadius = dp(c, radius).toFloat()
        edge.setColor(0x00FFFFFF)
        edge.setStroke(1, 0x99FFFFFF.toInt())

        val d = LayerDrawable(arrayOf<android.graphics.drawable.Drawable>(fill, edge))
        d.setLayerInset(1, 0, 0, 0, 0)
        return d
    }

    fun inner(c: Context, radius: Float = 18f): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = dp(c, radius).toFloat()
        d.colors = intArrayOf(0x99FFFFFF.toInt(), 0x59FFFFFF.toInt())
        d.gradientType = GradientDrawable.LINEAR_GRADIENT
        d.orientation = GradientDrawable.Orientation.TL_BR
        d.setStroke(1, 0x66FFFFFF)
        return d
    }

    fun pill(c: Context, active: Boolean): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = dp(c, 99f).toFloat()
        if (active) {
            d.colors = intArrayOf(0xFF0A84FF.toInt(), 0xFF57A8FF.toInt())
            d.gradientType = GradientDrawable.LINEAR_GRADIENT
            d.orientation = GradientDrawable.Orientation.LEFT_RIGHT
            d.setStroke(1, 0x40FFFFFF)
        } else {
            d.setColor(0x8CFFFFFF.toInt())
            d.setStroke(1, 0xA6FFFFFF.toInt())
        }
        return d
    }

    fun primary(c: Context): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = dp(c, 18f).toFloat()
        d.colors = intArrayOf(0xFF0A84FF.toInt(), 0xFF3E9BFF.toInt())
        d.gradientType = GradientDrawable.LINEAR_GRADIENT
        d.orientation = GradientDrawable.Orientation.LEFT_RIGHT
        return d
    }

    fun soft(c: Context, radius: Float = 16f): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = dp(c, radius).toFloat()
        d.setColor(accentSoft)
        return d
    }

    fun sheet(c: Context): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = dp(c, 28f).toFloat()
        d.setColor(0xFFFAFCFF.toInt())
        d.setStroke(2, 0xFFFFFFFF.toInt())
        return d
    }
}

