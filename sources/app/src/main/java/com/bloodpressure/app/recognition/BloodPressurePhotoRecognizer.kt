package com.bloodpressure.app.recognition

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class BloodPressureRecognitionResult(
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int
)

object BloodPressurePhotoRecognizer {
    private const val NORMALIZED_WIDTH = 600
    private const val NORMALIZED_HEIGHT = 770

    fun hasVisibleDisplay(screen: Bitmap): Boolean {
        val normalized = Bitmap.createScaledBitmap(screen, NORMALIZED_WIDTH, NORMALIZED_HEIGHT, true)
        val pixels = IntArray(NORMALIZED_WIDTH * NORMALIZED_HEIGHT)
        normalized.getPixels(pixels, 0, NORMALIZED_WIDTH, 0, 0, NORMALIZED_WIDTH, NORMALIZED_HEIGHT)
        if (normalized !== screen) normalized.recycle()
        return pixels.count(::isBlueDisplayPixel) >= 2_000
    }

    fun recognizeScreen(screen: Bitmap, classifier: RomSunDigitClassifier): BloodPressureRecognitionResult? {
        val normalized = Bitmap.createScaledBitmap(screen, NORMALIZED_WIDTH, NORMALIZED_HEIGHT, true)
        val pixels = IntArray(NORMALIZED_WIDTH * NORMALIZED_HEIGHT)
        normalized.getPixels(pixels, 0, NORMALIZED_WIDTH, 0, 0, NORMALIZED_WIDTH, NORMALIZED_HEIGHT)
        if (normalized !== screen) normalized.recycle()

        val rawMask = BooleanArray(pixels.size)
        var litPixels = 0
        pixels.forEachIndexed { index, pixel ->
            val lit = isBlueDisplayPixel(pixel)
            rawMask[index] = lit
            if (lit) litPixels++
        }
        if (litPixels !in 2_000..180_000) return null
        if (!passesQualityGate(pixels)) return null
        val mask = deskew(rawMask)
        val readingRows = detectReadingRows(mask) ?: return null

        val systolic = recognizeRow(mask, readingRows[0], classifier) ?: return null
        val diastolic = recognizeRow(mask, readingRows[1], classifier) ?: return null
        val heartRate = recognizeRow(mask, readingRows[2], classifier) ?: return null
        if (systolic !in 60..250 || diastolic !in 40..150 || heartRate !in 30..200) return null
        if (systolic <= diastolic) return null
        return BloodPressureRecognitionResult(systolic, diastolic, heartRate)
    }

    fun consensus(results: List<BloodPressureRecognitionResult?>): BloodPressureRecognitionResult? {
        return results.filterNotNull()
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    private fun recognizeRow(
        mask: BooleanArray,
        rowRange: IntRange,
        classifier: RomSunDigitClassifier
    ): Int? {
        val top = rowRange.first
        val bottom = rowRange.last + 1
        val left = (NORMALIZED_WIDTH * 0.34f).toInt()
        val rowHeight = bottom - top
        val counts = IntArray(NORMALIZED_WIDTH - left)
        for (x in left until NORMALIZED_WIDTH) {
            var count = 0
            for (y in top until bottom) if (mask[y * NORMALIZED_WIDTH + x]) count++
            counts[x - left] = count
        }

        val threshold = maxOf(3, (rowHeight * 0.025f).toInt())
        val active = BooleanArray(counts.size) { counts[it] >= threshold }
        fillSmallGaps(active, 8)
        val runs = mutableListOf<IntRange>()
        var start = -1
        active.forEachIndexed { index, value ->
            if (value && start < 0) start = index
            if (!value && start >= 0) {
                if (index - start >= 8) runs += (start + left)..(index - 1 + left)
                start = -1
            }
        }
        if (start >= 0 && active.size - start >= 8) runs += (start + left)..(active.lastIndex + left)
        val plausibleRuns = runs.filter { run ->
            val width = run.last - run.first + 1
            width <= rowHeight * 0.95f
        }
        val selectedRuns = plausibleRuns.takeLast(3)
        val wideWidths = selectedRuns
            .map { it.last - it.first + 1 }
            .filter { it >= rowHeight * 0.32f }
            .sorted()
        val targetDigitWidth = if (wideWidths.isNotEmpty()) {
            wideWidths[wideWidths.size / 2]
        } else {
            (rowHeight * 0.58f).toInt()
        }
        val digitRuns = selectedRuns.map { run ->
            if (run.last - run.first + 1 < targetDigitWidth / 2) {
                (run.last - targetDigitWidth + 1).coerceAtLeast(left)..run.last
            } else {
                run
            }
        }
        if (digitRuns.size !in 2..3) return null
        if (digitRuns.zipWithNext().any { (first, second) -> first.last >= second.first }) return null

        val digits = digitRuns.map { run -> decodeDigit(mask, run, top, bottom, classifier) ?: return null }
        return digits.joinToString("").toIntOrNull()
    }

    private fun decodeDigit(
        mask: BooleanArray,
        xRange: IntRange,
        rowTop: Int,
        rowBottom: Int,
        classifier: RomSunDigitClassifier
    ): Int? {
        var top = rowBottom
        var bottom = rowTop
        for (y in rowTop until rowBottom) {
            for (x in xRange) {
                if (mask[y * NORMALIZED_WIDTH + x]) {
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        if (bottom <= top) return null
        val width = xRange.last - xRange.first + 1
        val height = bottom - top + 1
        val widthRatio = width.toFloat() / height

        val regions = arrayOf(
            floatArrayOf(0.20f, 0.00f, 0.80f, 0.17f),
            floatArrayOf(0.70f, 0.09f, 1.00f, 0.47f),
            floatArrayOf(0.70f, 0.53f, 1.00f, 0.91f),
            floatArrayOf(0.20f, 0.83f, 0.80f, 1.00f),
            floatArrayOf(0.00f, 0.53f, 0.30f, 0.91f),
            floatArrayOf(0.00f, 0.09f, 0.30f, 0.47f),
            floatArrayOf(0.20f, 0.42f, 0.80f, 0.58f)
        )
        val occupancy = regions.map { region ->
            regionOccupancy(mask, xRange.first, top, width, height, region)
        }
        val ruleDigit = decodeSevenSegment(occupancy, widthRatio) ?: return null
        val digitMask = BooleanArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            digitMask[y * width + x] = mask[(top + y) * NORMALIZED_WIDTH + xRange.first + x]
        }
        val prediction = classifier.classify(digitMask, width, height) ?: return null
        return ruleDigit.takeIf { shouldAcceptDigit(it, prediction) }
    }

    private fun regionOccupancy(
        mask: BooleanArray,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        region: FloatArray
    ): Float {
        val x1 = (left + width * region[0]).toInt().coerceIn(0, NORMALIZED_WIDTH - 1)
        val y1 = (top + height * region[1]).toInt().coerceIn(0, NORMALIZED_HEIGHT - 1)
        val x2 = (left + width * region[2]).toInt().coerceIn(x1 + 1, NORMALIZED_WIDTH)
        val y2 = (top + height * region[3]).toInt().coerceIn(y1 + 1, NORMALIZED_HEIGHT)
        var active = 0
        var total = 0
        for (y in y1 until y2) for (x in x1 until x2) {
            if (mask[y * NORMALIZED_WIDTH + x]) active++
            total++
        }
        return active.toFloat() / total.coerceAtLeast(1)
    }

    private fun fillSmallGaps(values: BooleanArray, maxGap: Int) {
        var index = 0
        while (index < values.size) {
            if (values[index]) {
                index++
                continue
            }
            val start = index
            while (index < values.size && !values[index]) index++
            if (start > 0 && index < values.size && index - start <= maxGap) {
                for (i in start until index) values[i] = true
            }
        }
    }

    private fun isBlueDisplayPixel(pixel: Int): Boolean {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        return blue >= 135 && green >= 110 && blue - red >= 22 && green - red >= 4 && blue >= green - 24
    }

    private fun passesQualityGate(pixels: IntArray): Boolean {
        var nearWhite = 0
        var gradient = 0L
        var comparisons = 0
        for (y in 1 until NORMALIZED_HEIGHT step 3) {
            for (x in 1 until NORMALIZED_WIDTH step 3) {
                val index = y * NORMALIZED_WIDTH + x
                val pixel = pixels[index]
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)
                if (red > 247 && green > 247 && blue > 247) nearWhite++
                val luminance = (red * 3 + green * 6 + blue) / 10
                val left = pixels[index - 1]
                val leftLuminance = (Color.red(left) * 3 + Color.green(left) * 6 + Color.blue(left)) / 10
                gradient += kotlin.math.abs(luminance - leftLuminance)
                comparisons++
            }
        }
        val whiteRatio = nearWhite.toFloat() / comparisons.coerceAtLeast(1)
        val averageGradient = gradient.toFloat() / comparisons.coerceAtLeast(1)
        return whiteRatio < 0.22f && averageGradient >= 2.0f
    }

    private fun deskew(source: BooleanArray): BooleanArray {
        val points = ArrayList<Pair<Int, Int>>()
        for (y in 0 until NORMALIZED_HEIGHT step 2) for (x in 0 until NORMALIZED_WIDTH step 2) {
            if (source[y * NORMALIZED_WIDTH + x]) points += x to y
        }
        if (points.size < 100) return source
        val centerX = NORMALIZED_WIDTH / 2f
        val centerY = NORMALIZED_HEIGHT / 2f
        var bestAngle = 0
        var bestScore = Long.MIN_VALUE
        for (angle in -8..8) {
            val radians = Math.toRadians(angle.toDouble())
            val sinAngle = sin(radians)
            val cosAngle = cos(radians)
            val rows = IntArray(NORMALIZED_HEIGHT)
            points.forEach { (x, y) ->
                val rotatedY = ((x - centerX) * sinAngle + (y - centerY) * cosAngle + centerY).roundToInt()
                if (rotatedY in rows.indices) rows[rotatedY]++
            }
            val score = rows.sumOf { it.toLong() * it }
            if (score > bestScore) {
                bestScore = score
                bestAngle = angle
            }
        }
        if (bestAngle == 0) return source
        val result = BooleanArray(source.size)
        val radians = Math.toRadians(bestAngle.toDouble())
        val sinAngle = sin(radians)
        val cosAngle = cos(radians)
        for (y in 0 until NORMALIZED_HEIGHT) for (x in 0 until NORMALIZED_WIDTH) {
            if (!source[y * NORMALIZED_WIDTH + x]) continue
            val rotatedX = ((x - centerX) * cosAngle - (y - centerY) * sinAngle + centerX).roundToInt()
            val rotatedY = ((x - centerX) * sinAngle + (y - centerY) * cosAngle + centerY).roundToInt()
            if (rotatedX in 0 until NORMALIZED_WIDTH && rotatedY in 0 until NORMALIZED_HEIGHT) {
                result[rotatedY * NORMALIZED_WIDTH + rotatedX] = true
            }
        }
        return result
    }
}

internal fun detectReadingRows(mask: BooleanArray): List<IntRange>? {
    if (mask.size != 600 * 770) return null
    val width = 600
    val height = 770
    val left = (width * 0.30f).toInt()
    val right = (width * 0.98f).toInt()
    val rowCounts = IntArray(height)
    for (y in 0 until height) {
        var count = 0
        for (x in left until right) if (mask[y * width + x]) count++
        rowCounts[y] = count
    }

    val active = BooleanArray(height) { rowCounts[it] >= 40 }
    fillSmallGapsForRows(active, 10)
    val minimumHeight = (height * 0.065f).toInt()
    val maximumHeight = (height * 0.32f).toInt()
    val candidates = mutableListOf<IntRange>()
    var start = -1
    active.forEachIndexed { index, value ->
        if (value && start < 0) start = index
        if (!value && start >= 0) {
            val rowHeight = index - start
            if (rowHeight in minimumHeight..maximumHeight) candidates += start until index
            start = -1
        }
    }
    if (start >= 0) {
        val rowHeight = height - start
        if (rowHeight in minimumHeight..maximumHeight) candidates += start until height
    }
    if (candidates.size < 3) return null

    val rows = candidates.takeLast(3)
    val systolic = rows[0]
    val diastolic = rows[1]
    val heartRate = rows[2]
    if (systolic.first !in (height * 0.14f).toInt()..(height * 0.58f).toInt()) return null
    if (diastolic.first <= systolic.last + 4 || heartRate.first <= diastolic.last + 4) return null
    if (heartRate.last < height * 0.78f) return null
    if (heartRate.last - systolic.first < height * 0.46f) return null
    return rows
}

private fun fillSmallGapsForRows(values: BooleanArray, maxGap: Int) {
    var index = 0
    while (index < values.size) {
        if (values[index]) {
            index++
            continue
        }
        val start = index
        while (index < values.size && !values[index]) index++
        if (start > 0 && index < values.size && index - start <= maxGap) {
            for (i in start until index) values[i] = true
        }
    }
}

internal fun decodeSevenSegment(occupancy: List<Float>, widthRatio: Float): Int? {
    if (widthRatio !in 0.32f..1.05f) return null
    if (occupancy.size != 7) return null
    val patterns = arrayOf(
        booleanArrayOf(true, true, true, true, true, true, false),
        booleanArrayOf(false, true, true, false, false, false, false),
        booleanArrayOf(true, true, false, true, true, false, true),
        booleanArrayOf(true, true, true, true, false, false, true),
        booleanArrayOf(false, true, true, false, false, true, true),
        booleanArrayOf(true, false, true, true, false, true, true),
        booleanArrayOf(true, false, true, true, true, true, true),
        booleanArrayOf(true, true, true, false, false, false, false),
        booleanArrayOf(true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, false, true, true)
    )

    val strongestSegment = occupancy.maxOrNull() ?: return null
    if (strongestSegment < 0.14f) return null
    val threshold = maxOf(0.10f, strongestSegment * 0.38f)
    val ambiguityBand = maxOf(0.025f, threshold * 0.10f)
    if (occupancy.any { abs(it - threshold) < ambiguityBand }) return null
    val observed = occupancy.map { it >= threshold }
    // This ROMSUN display lights a strong upper-left edge on 7 at close range.
    if (observed == listOf(true, true, true, false, false, true, false)) return 7
    return patterns.indexOfFirst { pattern ->
        pattern.indices.all { index -> pattern[index] == observed[index] }
    }.takeIf { it >= 0 }
}

internal fun shouldAcceptDigit(ruleDigit: Int, prediction: DigitPrediction): Boolean {
    if (ruleDigit != prediction.digit) return false
    val strictConfidence = prediction.confidence >= 0.92f && prediction.margin >= 0.25f
    val strongAgreement = prediction.confidence >= 0.82f && prediction.margin >= 0.60f
    return strictConfidence || strongAgreement
}
