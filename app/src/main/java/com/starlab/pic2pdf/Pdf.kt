package com.starlab.pic2pdf

import java.io.ByteArrayOutputStream
import java.security.SecureRandom

/** 极简 PDF 写出器：每页一张全幅图片 + 完整 /Info 元数据（用于隐形水印） */
object Pdf {

    class PageData(val data: ByteArray, val filter: String, val w: Int, val h: Int, val pw: Float, val ph: Float)

    private fun pdfText(s: String): String {
        val allAscii = s.all { it.code in 32..126 }
        if (allAscii) {
            val sb = StringBuilder("(")
            for (ch in s) {
                when (ch) {
                    '(', ')', '\\' -> sb.append('\\').append(ch)
                    else -> sb.append(ch)
                }
            }
            return sb.append(")").toString()
        }
        val sb = StringBuilder("<FEFF")
        for (ch in s) {
            sb.append(String.format("%04X", ch.code))
        }
        return sb.append(">").toString()
    }

    fun build(pages: List<PageData>, info: LinkedHashMap<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        val offsets = HashMap<Int, Int>()
        fun raw(b: ByteArray) = out.write(b)
        fun txt(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))
        fun objStart(n: Int) { offsets[n] = out.size(); txt("$n 0 obj\n") }

        txt("%PDF-1.7\n")
        raw(byteArrayOf('%'.code.toByte(), 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))

        val pageObj = { i: Int -> 3 + i * 3 }
        objStart(1)
        txt("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        objStart(2)
        val kids = (0 until pages.size).joinToString(" ") { "${pageObj(it)} 0 R" }
        txt("<< /Type /Pages /Kids [ $kids ] /Count ${pages.size} >>\nendobj\n")

        for ((i, p) in pages.withIndex()) {
            val po = pageObj(i)
            objStart(po)
            txt("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${fmt(p.pw)} ${fmt(p.ph)}] " +
                "/Resources << /XObject << /Im0 ${po + 2} 0 R >> /ProcSet [/PDF /ImageC] >> " +
                "/Contents ${po + 1} 0 R >>\nendobj\n")

            val content = "q ${fmt(p.pw)} 0 0 ${fmt(p.ph)} 0 0 cm /Im0 Do Q\n".toByteArray(Charsets.ISO_8859_1)
            objStart(po + 1)
            txt("<< /Length ${content.size} >>\nstream\n")
            raw(content)
            txt("endstream\nendobj\n")

            objStart(po + 2)
            txt("<< /Type /XObject /Subtype /Image /Width ${p.w} /Height ${p.h} " +
                "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /${p.filter} /Length ${p.data.size} >>\nstream\n")
            raw(p.data)
            txt("\nendstream\nendobj\n")
        }

        val infoObj = 3 + pages.size * 3
        objStart(infoObj)
        val sb = StringBuilder("<< ")
        for ((k, v) in info) {
            sb.append("/").append(k).append(" ").append(pdfText(v)).append(" ")
        }
        sb.append(">>\nendobj\n")
        txt(sb.toString())

        val xrefPos = out.size()
        val total = infoObj + 1
        txt("xref\n0 $total\n")
        txt("0000000000 65535 f \n")
        for (n in 1 until total) {
            val off = offsets[n] ?: 0
            txt(String.format("%010d 00000 n \n", off))
        }
        val rnd = ByteArray(16)
        SecureRandom().nextBytes(rnd)
        val idHex = rnd.joinToString("") { String.format("%02x", it) }
        txt("trailer\n<< /Size $total /Root 1 0 R /Info $infoObj 0 R /ID [<$idHex> <$idHex>] >>\n")
        txt("startxref\n$xrefPos\n%%EOF\n")
        return out.toByteArray()
    }

    private fun fmt(v: Float): String = String.format(java.util.Locale.US, "%.2f", v)
}

