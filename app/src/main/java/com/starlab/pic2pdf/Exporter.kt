package com.starlab.pic2pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.Deflater

object Exporter {

    class Result(val path: String, val uri: Uri?, val bytes: Long, val pages: Int, val payload: String, val elapsedMs: Long)

    fun export(c: Context, s: S, onProgress: (done: Int, total: Int, note: String) -> Unit): Result {
        val t0 = System.currentTimeMillis()
        val dpi = s.dpi.toFloat()
        val plan = Compositor.plan(s, dpi)
        val payload = Watermarker.payload(s)
        val payloadB64 = Base64.encodeToString(payload.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val useLsb = s.invisOn && (s.invisMode == "lsb" || s.invisMode == "both")
        val useMeta = s.invisOn && (s.invisMode == "meta" || s.invisMode == "both" || s.invisMode == "visual")
        val list = ArrayList<Pdf.PageData>()

        for ((i, pg) in plan.withIndex()) {
            onProgress(i, plan.size, "渲染第 " + (i + 1) + " 页")
            val bmp = Compositor.drawPage(c, s, pg, 1f, null)
            val bytes: ByteArray
            if (useLsb) {
                Lsb.embed(bmp, payloadB64.toByteArray(Charsets.UTF_8))
                bytes = flate(toRgb(bmp))
            } else {
                val bo = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, s.quality, bo)
                bytes = bo.toByteArray()
            }
            val pw = bmp.width / dpi * 72f
            val ph = bmp.height / dpi * 72f
            list.add(Pdf.PageData(bytes, if (useLsb) "FlateDecode" else "DCTDecode", bmp.width, bmp.height, pw, ph))
            bmp.recycle()
        }

        onProgress(plan.size, plan.size, "写入 PDF")
        val info = LinkedHashMap<String, String>()
        val title = if (s.title.isBlank()) "图片转PDF" else s.title
        info["Title"] = title
        info["Author"] = "Pic2PDF"
        info["Creator"] = "Pic2PDF · 星球卑一号实验室"
        info["Producer"] = "Pic2PDF 1.0"
        if (useMeta) {
            info["Subject"] = "P2PW1:" + payloadB64
            info["Keywords"] = title + ", " + payloadB64
        }
        info["CreationDate"] = pdfDate(Date())
        info["ModDate"] = pdfDate(Date())
        val pdf = Pdf.build(list, info)

        val name = safeName(title) + "-" + SimpleDateFormat("MMdd-HHmm", Locale.US).format(Date()) + ".pdf"
        val saved = save(c, name, pdf)
        return Result(saved.first, saved.second, pdf.size.toLong(), plan.size, payload, System.currentTimeMillis() - t0)
    }

    private fun toRgb(bmp: Bitmap): ByteArray {
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val out = ByteArray(w * h * 3)
        var j = 0
        for (p in px) {
            out[j++] = (p ushr 16 and 0xFF).toByte()
            out[j++] = (p ushr 8 and 0xFF).toByte()
            out[j++] = (p and 0xFF).toByte()
        }
        return out
    }

    private fun flate(data: ByteArray): ByteArray {
        val d = Deflater(6)
        d.setInput(data)
        d.finish()
        val bo = ByteArrayOutputStream(data.size / 3)
        val buf = ByteArray(64 * 1024)
        while (!d.finished()) {
            val n = d.deflate(buf)
            if (n == 0) break
            bo.write(buf, 0, n)
        }
        d.end()
        return bo.toByteArray()
    }

    private fun safeName(s: String): String {
        val t = s.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").trim('_')
        return if (t.isEmpty()) "Pic2PDF" else t.take(40)
    }

    private fun pdfDate(d: Date): String =
        "D:" + SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(d) + "+08'00'"

    fun save(c: Context, name: String, data: ByteArray): Pair<String, Uri?> {
        if (Build.VERSION.SDK_INT >= 29) {
            val cv = ContentValues()
            cv.put(MediaStore.Downloads.DISPLAY_NAME, name)
            cv.put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            cv.put(MediaStore.Downloads.RELATIVE_PATH, "Download/Pic2PDF")
            cv.put(MediaStore.Downloads.IS_PENDING, 1)
            val uri = c.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
            if (uri != null) {
                c.contentResolver.openOutputStream(uri)?.use { it.write(data); it.flush() }
                val cv2 = ContentValues()
                cv2.put(MediaStore.Downloads.IS_PENDING, 0)
                c.contentResolver.update(uri, cv2, null, null)
                return Pair("/sdcard/Download/Pic2PDF/" + name, uri)
            }
        }
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Pic2PDF")
        if (!dir.exists()) dir.mkdirs()
        val f = File(dir, name)
        FileOutputStream(f).use { it.write(data) }
        return Pair(f.absolutePath, null)
    }
}

