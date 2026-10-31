package com.starlab.pic2pdf

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

/** 从导出的 PDF 里取回隐形水印（元数据 / LSB） */
object PdfProbe {

    data class Found(val subject: String?, val lsb: String?, val pages: Int)

    fun probe(bytes: ByteArray): Found {
        val text = String(bytes, Charsets.ISO_8859_1)
        var subject: String? = null
        val si = text.indexOf("/Subject (")
        if (si >= 0) {
            var i = si + "/Subject (".length
            val sb = StringBuilder()
            while (i < text.length) {
                val ch = text[i]
                if (ch == '\\') { sb.append(text[i + 1]); i += 2; continue }
                if (ch == ')') break
                sb.append(ch); i++
            }
            subject = sb.toString()
        }
        val pages = Regex("/Type /Page[^s]").findAll(text).count()
        var lsb: String? = null
        try {
            val wm = Regex("/Width (\\d+)").find(text)
            val hm = Regex("/Height (\\d+)").find(text)
            val smi = text.indexOf("/FlateDecode")
            if (smi > 0 && wm != null && hm != null) {
                val st = text.indexOf("stream\n", smi)
                val en = text.indexOf("endstream", st)
                if (st > 0 && en > st) {
                    val start = st + 7
                    val raw = bytes.copyOfRange(start, en)
                    val inf = Inflater()
                    inf.setInput(raw)
                    val bo = ByteArrayOutputStream()
                    val buf = ByteArray(256 * 1024)
                    while (!inf.finished()) {
                        val n = inf.inflate(buf)
                        if (n == 0) break
                        bo.write(buf, 0, n)
                    }
                    inf.end()
                    val w = wm.groupValues[1].toInt()
                    val h = hm.groupValues[1].toInt()
                    lsb = Lsb.extract(bo.toByteArray(), w, h)
                }
            }
        } catch (t: Throwable) {
        }
        return Found(subject, lsb, pages)
    }
}

