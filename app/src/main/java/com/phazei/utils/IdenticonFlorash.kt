package com.phazei.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.lang.Math.pow
import kotlin.math.*

/**
 * Original creator implemented it in JS:
 * https://github.com/AKRFranko/florash/
 *
 * This was adapted with the assistance of GPT4
 *
 * IdenticonFlorash.generateBitmap(florashHash, size) //hash is hex, size is px
 */
class IdenticonFlorash() {
    private val twoPi = PI * 2
    private val phi = (1 + sqrt(5.0)) / 2

    private fun createHSLString(h: Int, s: Int, l: Int): Int {
        return Color.HSVToColor(floatArrayOf((h % 360).toFloat(), (100 - (s % 100)).toFloat() / 100, (100 - (l % 100)).toFloat() / 100))
    }

    private fun hashSum(hash: String): Int {
        return hash.chunked(2).sumOf { it.toInt(16) }
    }

    private fun getHashColor(hash: String): Int {
        val sum = hashSum(hash)
        val initialHue = (sum * 360) / (255 * (hash.length / 2))
        val offsetHue = (initialHue + (sum / PI)).toInt() * 360
        return offsetHue
    }

    private fun createHashParameters(hash: String): List<Map<String, Double>> {
        val flts = hash.chunked(2).map { it.toInt(16) / 255.0 }
        val groups = mutableListOf<List<Double>>()
        var remainingFlts = flts
        while (groups.size < hash.length / 8) {
            val group = remainingFlts.take(4)
            remainingFlts = remainingFlts.drop(4)
            groups.add(group)
        }
        return groups.mapIndexed { index, group ->
            val fillHue = (group[0] * 360).toInt()
            val strokeHue = (group[1] * 360).toInt()
            val n = hash.length / group.size
            val m = n * 2 - (group[0] * n + index)
            val params = mapOf(
                "fill" to (fillHue + ((360 / groups.size) * index)) % 360.0,
                "stroke" to strokeHue.toDouble(),
                "m" to m - m % (index + 2),
                "n1" to sqrt(0.5) + group[1],
                "n2" to sqrt(0.5) + group[2],
                "n3" to sqrt(0.5) + group[3]
            )
            params
        }
    }

    private fun superFormula(a: Double, b: Double, m: Double, n1: Double, n2: Double, n3: Double): List<Pair<Double, Double>> {
        val output = mutableListOf<Pair<Double, Double>>()
        var p = 0.0
        while (p <= twoPi) {
            val ang = m * p / 4
            val r = pow(pow(abs(cos(ang) / a), n2) + pow(abs(sin(ang) / b), n3), -1 / n1)
            val xp = r * cos(p)
            val yp = r * sin(p)
            p += 0.01
            output.add(Pair(xp, yp))
        }
        return output
    }

    fun generateBitmap(hash: String, size: Int = 256): Bitmap {
        val mainHue = getHashColor(hash)
        val parameters = createHashParameters(hash)
        val center = size / 2
        val shapes = parameters.map { params ->
            val fill = createHSLString(params["fill"]!!.toInt(), 100, 66)
            val stroke = createHSLString(params["stroke"]!!.toInt(), 100, 22 + (11 * params["fill"]!!.toInt()))
            val m = params["m"]!!
            val n1 = params["n1"]!!
            val n2 = params["n2"]!!
            val n3 = params["n3"]!!
            val points = superFormula(1.0, 1.0, m, n1, n2, n3).map { (x, y) ->
                Pair(center + (x * (size / 2 - (phi / 100 * size))).toInt(), center + (y * (size / 2 - (phi / 100 * size))).toInt())
            }
            ShapeData(fill, stroke, points)
        }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND


        val shapesCount = shapes.size
        var offset = 0f
        var newSize = size.toFloat()

        shapes.forEach { (fill, stroke, points) ->
            val shrink = size.toFloat() / shapesCount.toFloat()

            paint.color = fill
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 0f

            canvas.save()
            canvas.translate(offset, offset)
            val scaleFactor = newSize / size.toFloat()
            canvas.scale(scaleFactor, scaleFactor)
            canvas.drawPath(points.toPath(), paint)

            paint.color = stroke
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (phi / 100 * newSize).toFloat()
            canvas.drawPath(points.toPath(), paint)

            canvas.restore()

            newSize -= shrink
            offset += (shrink / 2)
        }

        return bitmap
    }

    private fun List<Pair<Int, Int>>.toPath(): android.graphics.Path {
        val path = android.graphics.Path()
        path.moveTo(first().first.toFloat(), first().second.toFloat())
        forEach { (x, y) ->
            path.lineTo(x.toFloat(), y.toFloat())
        }
        path.close()
        return path
    }
}

data class ShapeData(val fill: Int, val stroke: Int, val points: List<Pair<Int, Int>>)
