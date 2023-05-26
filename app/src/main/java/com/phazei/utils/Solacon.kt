package com.phazei.utils

import android.graphics.*
import android.util.Log
import okhttp3.internal.toHexString
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin

/**
 * Originally a javascript embedded SVG
 * https://github.com/naknomum/solacon
 *
 * Solacon.generateBitmap(solaconHash, size, rbg) //hash is string, size is px, rgb is #FFFFFF or null
 */
class Solacon() {
    private var width: Int = 0
    private var height: Int = 0
    private lateinit var center: PointF
    private var rgb: String? = null
    private var hashValue: Int = 0

    companion object {
        private val instance = Solacon()
        private val cache = ConcurrentHashMap<String, Bitmap>()

        fun generateBitmap(value: String, size: Int, customRGB: String? = null): Bitmap {
            return cache.getOrPut(value) { instance.generateBitmap(value, size, customRGB) }
        }
    }

    fun generateBitmap(value: String, size: Int, customRGB: String? = null): Bitmap {
        width = size
        height = size
        center = PointF(width / 2f, height / 2f)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        hashValue = sdbm(value)
        rgb = customRGB ?: setHSLFromHash(value)
        // Log.d("TAG", "value $value ======= color $rgb ======= hash $hashValue")
        val slices = (hashValue and 0x07) + 3
        val wAngle = Math.PI * 2 / slices

        val data = Array(6) { DoubleArray(3) }
        for (i in 0..5) {
            data[i][0] = ((hashValue ushr (i * 3) and 0x07) / 7.0)
            data[i][1] = ((hashValue ushr (i * 3 + 1) and 0x07) / 7.0)
            data[i][2] = (hashValue ushr (i * 4 + 8) and 0x0F).toDouble()
        }

        for (i in 0 until slices) {
            wedge(canvas, wAngle * i, wAngle * (i + 1), Math.min(width, height) / 2.0, data)
        }

        return bitmap
    }

    private fun wedge(canvas: Canvas, a1: Double, a2: Double, size: Double, data: Array<DoubleArray>) {
        for (i in data.indices) {
            swish(canvas, a1, a2, size * data[i][0], size * data[i][1], data[i][2].toInt())
        }
    }

    private fun swish(canvas: Canvas, a1: Double, a2: Double, r1: Double, r2: Double, alpha: Int) {
        val path = bez(a1, a2, r1, r2, center)

        val paint = Paint()
        paint.color = Color.parseColor(rgb)
        paint.alpha = 32 + ((224 * (alpha / 15.0)).toInt()) // Shift alpha up by 64
        paint.style = Paint.Style.FILL

        canvas.drawPath(path, paint)
    }


    private fun bez(a1: Double, a2: Double, r1: Double, r2: Double, offset: PointF): Path {
        val path = Path()
        val p1 = pt(a1, r1)
        val p2 = pt(a2, r2)
        val bd = (a2 - a1) / 3
        val b1 = pt(a1 + bd, (r1 + r2) / 2)
        val b2 = pt(a2 - bd, (r1 + r2) / 2)

        path.moveTo(p1.x + offset.x, p1.y + offset.y)
        path.cubicTo(b1.x + offset.x, b1.y + offset.y, b2.x + offset.x, b2.y + offset.y, p2.x + offset.x, p2.y + offset.y)

        val b1a = pt(a1 + bd, (r1 + r2) / 3)
        val b2a = pt(a2 - bd, (r1 + r2) / 3)
        path.cubicTo(b1a.x + offset.x, b1a.y + offset.y, b2a.x + offset.x, b2a.y + offset.y, p1.x + offset.x, p1.y + offset.y)

        return path
    }


    private fun pt(theta: Double, r: Double): PointF {
        val x = (r * cos(theta)).toFloat()
        val y = (r * sin(theta)).toFloat()
        return PointF(x, y)
    }

    private fun setHSLFromHash(input: String): String {
        //use sha1 for balanced randomness
        val hash = sha1(input)
        var hue = ((hash.substring(0, 4).toInt(16) / 65536f) * 360).toInt() % 360
        // scale number from 0-50, then add 50 for 55-100 range
        val saturation = ((hash.substring(4, 8).toInt(16) / 65536f) * 70 + 30).toInt() % 101
        // scale number from 0-40, then add 40 for 50-90 range
        var lightness = ((hash.substring(8, 12).toInt(16) / 65536f) * 50 + 40).toInt() % 101

        //adjustments to avoid undesirable colors
        // Adjust hue to avoid orange-brown colors
        if (hue in 30..70) {
            val gradient = (hue - 40) / 40.0
            // gradient 0 - 1, shift to 65-360 & 0-25
            hue = (70 + (gradient * 310)).toInt() % 360
            // Scale lightness if hue is in the orange range
            lightness = (lightness + gradient * 10).toInt().coerceAtMost(100)
        }

        // Violet is great, but it doesn't contrast with the default app color
        // Adjust hue to avoid violet
        if (hue in 260..285) {
            val gradient = (hue - 260) / 20.0
            hue = (285 + (gradient * 20)).toInt() % 360
        }


        val hsv = floatArrayOf(hue.toFloat(), saturation.toFloat() / 100, lightness.toFloat() / 100)
        val rgbColor = Color.HSVToColor(hsv)

        return String.format("#%02x%02x%02x", Color.red(rgbColor), Color.green(rgbColor), Color.blue(rgbColor))
    }

    private fun sha1(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    private fun sdbm(s: String): Int {
        var str = s
        if (str.length < 6) str = s.repeat(5) // short strings kinda suck
        var h = 0
        for (i in str.indices) {
            h = str[i].toInt() + (h shl 6) + (h shl 16) - h
        }
        return h ushr 0
    }
}
