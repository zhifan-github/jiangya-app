package com.bloodpressure.app.recognition

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class DigitPrediction(val digit: Int, val confidence: Float, val margin: Float)

class RomSunDigitClassifier(context: Context) : Closeable {
    private val interpreter = Interpreter(
        context.assets.openFd("romsun_digit_classifier.tflite").use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.declaredLength
                )
            }
        },
        Interpreter.Options().apply { setNumThreads(2) }
    )

    @Synchronized
    fun classify(mask: BooleanArray, width: Int, height: Int): DigitPrediction? {
        if (width <= 0 || height <= 0 || mask.size != width * height) return null
        val input = ByteBuffer.allocateDirect(1 * 96 * 64 * 4).order(ByteOrder.nativeOrder())
        for (targetY in 0 until 96) {
            val sourceY = (targetY * height / 96).coerceIn(0, height - 1)
            for (targetX in 0 until 64) {
                val sourceX = (targetX * width / 64).coerceIn(0, width - 1)
                input.putFloat(if (mask[sourceY * width + sourceX]) 255f else 0f)
            }
        }
        input.rewind()
        val output = Array(1) { FloatArray(10) }
        interpreter.run(input, output)
        val ranked = output[0].mapIndexed { digit, score -> digit to score }.sortedByDescending { it.second }
        val best = ranked[0]
        val second = ranked[1]
        return DigitPrediction(best.first, best.second, best.second - second.second)
    }

    @Synchronized
    override fun close() = interpreter.close()
}
