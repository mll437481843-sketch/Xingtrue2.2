package com.starlab.pic2pdf

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

object UI {

    fun dp(c: Context, v: Float): Int = G.dp(c, v)

    fun text(c: Context, t: String, sp: Float, color: Int, bold: Boolean = false, font: Typeface? = null): TextView {
        val v = TextView(c)
        v.text = t
        v.setTextSize(sp)
        v.setTextColor(color)
        v.typeface = font ?: Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        v.includeFontPadding = false
        return v
    }

    fun lp(w: Int, h: Int, weight: Float = 0f): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(w, h)
        p.weight = weight
        return p
    }

    fun margins(p: LinearLayout.LayoutParams, c: Context, l: Float = 0f, t: Float = 0f, r: Float = 0f, b: Float = 0f): LinearLayout.LayoutParams {
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b))
        return p
    }

    /** 玻璃卡 + 标题 */
    fun card(c: Context, title: String, sub: String = ""): LinearLayout {
        val card = LinearLayout(c)
        card.orientation = LinearLayout.VERTICAL
        card.background = G.glass(c)
        val pad = dp(c, 18f)
        card.setPadding(pad, dp(c, 16f), pad, dp(c, 16f))
        card.elevation = dp(c, 1f).toFloat()
        card.addView(text(c, title, 15f, G.ink, true))
        if (sub.isNotEmpty()) {
            val s = text(c, sub, 11.5f, G.ink3)
            s.setPadding(0, dp(c, 3f), 0, 0)
            card.addView(s)
        }
        return card
    }

    fun sectionGap(c: Context, parent: LinearLayout, h: Float = 12f) {
        val v = View(c)
        parent.addView(v, lp(1, dp(c, h)))
    }

    /** 横向滚动胶囊组 */
    class Seg(c: Context, items: List<String>, private val fonts: List<Typeface?>? = null) : HorizontalScrollView(c) {
        private val row = LinearLayout(c)
        var selected = 0
        var onSelect: (Int) -> Unit = {}
        private val labels: MutableList<TextView> = mutableListOf()

        init {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            row.orientation = LinearLayout.HORIZONTAL
            addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            for ((i, name) in items.withIndex()) {
                val t = TextView(c)
                t.text = name
                t.setTextSize(13f)
                t.gravity = Gravity.CENTER
                t.typeface = fonts?.getOrNull(i) ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
                val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(c, 36f))
                p.setMargins(0, 0, dp(c, 8f), 0)
                t.setPadding(dp(c, 15f), 0, dp(c, 15f), 0)
                t.background = G.pill(c, false)
                t.setTextColor(G.ink2)
                t.setOnClickListener {
                    selected = i
                    paint()
                    onSelect(i)
                }
                labels.add(t)
                row.addView(t, p)
            }
            paint()
        }

        fun paint() {
            for ((i, t) in labels.withIndex()) {
                val a = i == selected
                t.background = G.pill(context, a)
                t.setTextColor(if (a) 0xFFFFFFFF.toInt() else G.ink2)
            }
        }

        fun set(i: Int) {
            selected = i
            paint()
        }
    }

    /** 带数值的滑杆行 */
    class Slider(
        c: Context,
        label: String,
        private val min: Int,
        private val max: Int,
        value: Int,
        private val suffix: String
    ) : LinearLayout(c) {
        private val valueLabel: TextView
        private val bar: SeekBar
        var onChange: (Int) -> Unit = {}

        init {
            orientation = LinearLayout.VERTICAL
            val top = LinearLayout(c)
            top.orientation = LinearLayout.HORIZONTAL
            top.addView(text(c, label, 12.5f, G.ink2), lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            valueLabel = text(c, "$value$suffix", 12.5f, G.ink, true)
            top.addView(valueLabel)
            addView(top)

            bar = SeekBar(c)
            bar.max = max - min
            bar.progress = (value - min).coerceIn(0, max - min)
            bar.setPadding(dp(c, -6f), dp(c, 5f), dp(c, 5f), dp(c, 5f))
            addView(bar, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 34f)))
            bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    valueLabel.text = "${p + min}$suffix"
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {
                    onChange(bar.progress + min)
                }
            })
        }

        fun value(): Int = bar.progress + min
    }

    fun input(c: Context, hint: String, value: String): EditText {
        val e = EditText(c)
        e.hint = hint
        e.setText(value)
        e.setTextSize(13.5f)
        e.setTextColor(G.ink)
        e.setHintTextColor(G.ink3)
        e.background = G.inner(c, 14f)
        e.setPadding(dp(c, 14f), dp(c, 10f), dp(c, 14f), dp(c, 10f))
        e.setSingleLine(true)
        return e
    }

    fun smallButton(c: Context, label: String, onTap: () -> Unit): TextView {
        val t = text(c, label, 12.5f, G.accent, true)
        t.background = G.soft(c, 12f)
        t.setPadding(dp(c, 12f), dp(c, 7f), dp(c, 12f), dp(c, 7f))
        t.setOnClickListener { onTap() }
        return t
    }
}

