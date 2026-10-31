package com.starlab.pic2pdf

import android.graphics.Bitmap

/** 像素最低位隐写：写进 B 通道 LSB，需无损编码保存（PDF Flate） */
object Lsb {
    private val MAGIC = byteArrayOf(80, 50, 80, 87) // "P2PW"
    private const val HDR = 8

    fun capacity(bmp: Bitmap): Int = bmp.width * bmp.height / 8

    fun embed(bmp: Bitmap, payload: ByteArray): Boolean {
        if (HDR + payload.size > capacity(bmp)) return false
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val buf = ByteArray(HDR + payload.size)
        System.arraycopy(MAGIC, 0, buf, 0, 4)
        val n = payload.size
        buf[4] = (n ushr 24 and 0xFF).toByte()
        buf[5] = (n ushr 16 and 0xFF).toByte()
        buf[6] = (n ushr 8 and 0xFF).toByte()
        buf[7] = (n and 0xFF).toByte()
        System.arraycopy(payload, 0, buf, HDR, n)
        val totalBits = buf.size * 8
        var i = 0
        while (i < totalBits) {
            val byteVal = buf[i / 8].toInt()
            val bit = (byteVal ushr (7 - (i % 8))) and 1
            val c = px[i]
            px[i] = (c and 0xFFFFFF00.toInt()) or ((c and 0xFE) or bit)
            i++
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h)
        return true
    }

    fun extract(rgb: ByteArray, w: Int, h: Int): String? {
        val bitsN = minOf(rgb.size / 3, w * h)
        if (bitsN < HDR * 8) return null
        val hdr = ByteArray(HDR)
        for (i in 0 until HDR * 8) {
            val bit = rgb[i * 3 + 2].toInt() and 1
            hdr[i / 8] = (hdr[i / 8].toInt() or (bit shl (7 - i % 8))).toByte()
        }
        if (hdr[0].toInt() != 80 || hdr[1].toInt() != 50 || hdr[2].toInt() != 80 || hdr[3].toInt() != 87) return null
        val len = ((hdr[4].toInt() and 0xFF) shl 24) or ((hdr[5].toInt() and 0xFF) shl 16) or
            ((hdr[6].toInt() and 0xFF) shl 8) or (hdr[7].toInt() and 0xFF)
        if (len <= 0 || (HDR + len) * 8 > bitsN) return null
        val data = ByteArray(len)
        for (i in 0 until len * 8) {
            val bitPos = HDR * 8 + i
            val bit = rgb[bitPos * 3 + 2].toInt() and 1
            val k = i / 8
            data[k] = (data[k].toInt() or (bit shl (7 - i % 8))).toByte()
        }
        return String(data, Charsets.UTF_8)
    }
}

