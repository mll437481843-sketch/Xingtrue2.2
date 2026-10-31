package com.starlab.pic2pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns

object Imgs {

    fun meta(c: Context, uri: Uri): Pic? {
        var name = "image"
        try {
            c.contentResolver.query(uri, null, null, null, null)?.use { cur ->
                val i = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && cur.moveToFirst()) name = cur.getString(i) ?: name
            }
        } catch (t: Throwable) {}
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        try {
            c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        } catch (t: Throwable) {
            return null
        }
        if (o.outWidth <= 0) return null
        return Pic(uri.toString(), name, o.outWidth, o.outHeight)
    }

    fun load(c: Context, uriStr: String, maxSide: Int): Bitmap? {
        val uri = Uri.parse(uriStr)
        val b = BitmapFactory.Options()
        b.inJustDecodeBounds = true
        try {
            c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, b) }
        } catch (t: Throwable) { return null }
        if (b.outWidth <= 0) return null
        var sample = 1
        while (b.outWidth / (sample * 2) >= maxSide || b.outHeight / (sample * 2) >= maxSide) sample *= 2
        val o = BitmapFactory.Options()
        o.inSampleSize = sample
        o.inPreferredConfig = Bitmap.Config.ARGB_8888
        return try {
            c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        } catch (t: Throwable) { null }
    }

    fun loadAny(c: Context, uriStr: String, maxSide: Int): Bitmap? {
        if (uriStr.startsWith("/")) {
            val b = BitmapFactory.Options()
            b.inJustDecodeBounds = true
            BitmapFactory.decodeFile(uriStr, b)
            var s = 1
            if (b.outWidth > 0) {
                while (b.outWidth / (s * 2) >= maxSide || b.outHeight / (s * 2) >= maxSide) s *= 2
            }
            val o = BitmapFactory.Options()
            o.inSampleSize = s
            return try { BitmapFactory.decodeFile(uriStr, o) } catch (t: Throwable) { null }
        }
        return load(c, uriStr, maxSide)
    }
}

