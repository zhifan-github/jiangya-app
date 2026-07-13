package com.bloodpressure.app.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BloodPressurePhotoRecognizerTest {
    @Test
    fun decodeSevenSegment_decodesAllDigits() {
        val patterns = listOf(
            listOf(1, 1, 1, 1, 1, 1, 0),
            listOf(0, 1, 1, 0, 0, 0, 0),
            listOf(1, 1, 0, 1, 1, 0, 1),
            listOf(1, 1, 1, 1, 0, 0, 1),
            listOf(0, 1, 1, 0, 0, 1, 1),
            listOf(1, 0, 1, 1, 0, 1, 1),
            listOf(1, 0, 1, 1, 1, 1, 1),
            listOf(1, 1, 1, 0, 0, 0, 0),
            listOf(1, 1, 1, 1, 1, 1, 1),
            listOf(1, 1, 1, 1, 0, 1, 1)
        )

        patterns.forEachIndexed { digit, pattern ->
            val occupancy = pattern.map { if (it == 1) 0.62f else 0.04f }
            assertEquals(digit, decodeSevenSegment(occupancy, 0.58f))
        }
    }

    @Test
    fun decodeSevenSegment_requiresTheTwoRightSegmentsForOne() {
        assertEquals(1, decodeSevenSegment(listOf(0.03f, 0.65f, 0.61f, 0.02f, 0.04f, 0.03f, 0.05f), 0.58f))
    }

    @Test
    fun decodeSevenSegment_handlesUnevenOutlineBrightness() {
        assertEquals(8, decodeSevenSegment(listOf(0.87f, 0.79f, 0.75f, 0.84f, 0.81f, 0.69f, 0.89f), 0.58f))
        assertEquals(0, decodeSevenSegment(listOf(0.87f, 0.79f, 0.74f, 0.84f, 0.79f, 0.76f, 0.15f), 0.58f))
    }

    @Test
    fun decodeSevenSegment_handlesRomSunSevenUpperLeftEdge() {
        assertEquals(7, decodeSevenSegment(listOf(0.85f, 0.77f, 0.65f, 0.15f, 0.0f, 0.81f, 0.08f), 0.62f))
    }

    @Test
    fun detectReadingRows_ignoresHeaderAndAllowsVerticalOffset() {
        val width = 600
        val height = 770
        val mask = BooleanArray(width * height)
        listOf(128..205, 238..410, 438..606, 638..742).forEach { row ->
            for (y in row) for (x in 260..540) mask[y * width + x] = true
        }

        assertEquals(listOf(238..410, 438..606, 638..742), detectReadingRows(mask))
    }

    @Test
    fun detectReadingRows_ignoresBlueBackgroundNoiseAtCloseRange() {
        val width = 600
        val height = 770
        val mask = BooleanArray(width * height)
        for (y in 90..735) for (x in 180..199) mask[y * width + x] = true
        listOf(126..330, 361..551, 594..713).forEach { row ->
            for (y in row) for (x in 260..540) mask[y * width + x] = true
        }

        assertEquals(listOf(126..330, 361..551, 594..713), detectReadingRows(mask))
    }

    @Test
    fun shouldAcceptDigit_allowsStrongRuleAndModelAgreement() {
        assertEquals(true, shouldAcceptDigit(7, DigitPrediction(7, 0.84f, 0.70f)))
        assertEquals(false, shouldAcceptDigit(7, DigitPrediction(7, 0.84f, 0.40f)))
        assertEquals(false, shouldAcceptDigit(7, DigitPrediction(1, 0.99f, 0.98f)))
    }

    @Test
    fun consensus_acceptsOneValidFrameAndPrefersTheMostFrequentReading() {
        val reading = BloodPressureRecognitionResult(123, 80, 82)
        val otherReading = BloodPressureRecognitionResult(124, 81, 83)
        assertEquals(reading, BloodPressurePhotoRecognizer.consensus(listOf(null, reading, null)))
        assertEquals(
            reading,
            BloodPressurePhotoRecognizer.consensus(listOf(reading, otherReading, reading))
        )
        assertNull(BloodPressurePhotoRecognizer.consensus(listOf(null, null)))
    }
}
