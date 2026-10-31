package com.starlab.pic2pdf

object Fonts {
    fun familyOf(key: String): String = when (key) {
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
}

