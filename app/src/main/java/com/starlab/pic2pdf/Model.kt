package com.starlab.pic2pdf

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

data class Pic(val uri: String, val name: String, val w: Int, val h: Int)

data class S(
    var pics: MutableList<Pic> = mutableListOf(),
    var mode: String = "single",
    var perPage: Int = 4,
    var ratio: String = "a4p",
    var fit: String = "contain",
    var margin: Float = 4f,
    var gap: Float = 2f,
    var corner: Float = 1.5f,
    var bg: Int = Color.WHITE,
    var dpi: Int = 150,
    var quality: Int = 92,
    var wmOn: Boolean = true,
    var wmText: String = "星球卑一号实验室",
    var wmFont: String = "sans-serif",
    var wmFontPath: String? = null,
    var wmSize: Float = 4.5f,
    var wmAlpha: Float = 0.45f,
    var wmColor: Int = Color.WHITE,
    var wmAngle: Float = -28f,
    var wmLayout: String = "tile",
    var logoUri: String? = null,
    var logoScale: Float = 22f,
    var logoAlpha: Float = 0.6f,
    var logoPos: String = "br",
    var invisOn: Boolean = true,
    var invisText: String = "星球卑一号实验室",
    var invisMode: String = "visual",
    var title: String = ""
)

object Prefs {
    private const val NAME = "pic2pdf"

    fun load(c: Context): S {
        val sp = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val s = S()
        try {
            val j = JSONObject(sp.getString("s", "{}") ?: "{}")
            s.mode = j.optString("mode", s.mode)
            s.perPage = j.optInt("perPage", s.perPage)
            s.ratio = j.optString("ratio", s.ratio)
            s.fit = j.optString("fit", s.fit)
            s.margin = j.optDouble("margin", s.margin.toDouble()).toFloat()
            s.gap = j.optDouble("gap", s.gap.toDouble()).toFloat()
            s.corner = j.optDouble("corner", s.corner.toDouble()).toFloat()
            s.bg = j.optInt("bg", s.bg)
            s.dpi = j.optInt("dpi", s.dpi)
            s.quality = j.optInt("quality", s.quality)
            s.wmOn = j.optBoolean("wmOn", s.wmOn)
            s.wmText = j.optString("wmText", s.wmText)
            s.wmFont = j.optString("wmFont", s.wmFont)
            s.wmFontPath = j.optString("wmFontPath", "").ifEmpty { null }
            s.wmSize = j.optDouble("wmSize", s.wmSize.toDouble()).toFloat()
            s.wmAlpha = j.optDouble("wmAlpha", s.wmAlpha.toDouble()).toFloat()
            s.wmColor = j.optInt("wmColor", s.wmColor)
            s.wmAngle = j.optDouble("wmAngle", s.wmAngle.toDouble()).toFloat()
            s.wmLayout = j.optString("wmLayout", s.wmLayout)
            s.logoUri = j.optString("logoUri", "").ifEmpty { null }
            s.logoScale = j.optDouble("logoScale", s.logoScale.toDouble()).toFloat()
            s.logoAlpha = j.optDouble("logoAlpha", s.logoAlpha.toDouble()).toFloat()
            s.logoPos = j.optString("logoPos", s.logoPos)
            s.invisOn = j.optBoolean("invisOn", s.invisOn)
            s.invisText = j.optString("invisText", s.invisText)
            s.invisMode = j.optString("invisMode", s.invisMode)
            s.title = j.optString("title", s.title)
            val arr = j.optJSONArray("pics") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                s.pics.add(Pic(o.optString("u"), o.optString("n"), o.optInt("w"), o.optInt("h")))
            }
        } catch (t: Throwable) {
        }
        return s
    }

    fun save(c: Context, s: S) {
        val j = JSONObject()
        j.put("mode", s.mode); j.put("perPage", s.perPage); j.put("ratio", s.ratio)
        j.put("fit", s.fit); j.put("margin", s.margin.toDouble()); j.put("gap", s.gap.toDouble())
        j.put("corner", s.corner.toDouble()); j.put("bg", s.bg); j.put("dpi", s.dpi)
        j.put("quality", s.quality); j.put("wmOn", s.wmOn); j.put("wmText", s.wmText)
        j.put("wmFont", s.wmFont); j.put("wmFontPath", s.wmFontPath ?: "")
        j.put("wmSize", s.wmSize.toDouble()); j.put("wmAlpha", s.wmAlpha.toDouble())
        j.put("wmColor", s.wmColor); j.put("wmAngle", s.wmAngle.toDouble())
        j.put("wmLayout", s.wmLayout); j.put("logoUri", s.logoUri ?: "")
        j.put("logoScale", s.logoScale.toDouble()); j.put("logoAlpha", s.logoAlpha.toDouble())
        j.put("logoPos", s.logoPos); j.put("invisOn", s.invisOn); j.put("invisText", s.invisText)
        j.put("invisMode", s.invisMode); j.put("title", s.title)
        val arr = JSONArray()
        for (p in s.pics) {
            val o = JSONObject()
            o.put("u", p.uri); o.put("n", p.name); o.put("w", p.w); o.put("h", p.h)
            arr.put(o)
        }
        j.put("pics", arr)
        c.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString("s", j.toString()).apply()
    }
}

