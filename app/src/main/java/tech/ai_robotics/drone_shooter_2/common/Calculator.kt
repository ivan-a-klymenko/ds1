package tech.ai_robotics.drone_shooter_2.common

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs


class Calculator {

    fun calculateMotorCommands(
        points: List<DetectedPoint>,
        speed: Float = SPEED,
        epsilon: Float = TARGET_DIFF.toFloat(),
        maxTimeMillis: Long = 60_000L,
        stepMillis: Long = 10L
    ): Prediction? {
        // Извлекаем отдельно значения и временные метки
        val xValues = points.map { it.cx }
        val yValues = points.map { it.cy }
        val timestamps = points.map { it.currentTimeMillis }

        // Предсказываем пересечение по X и Y отдельно
        val xTarget = findAxisIntersectionTime(xValues, timestamps, speed, epsilon, maxTimeMillis, stepMillis)
        val yTarget = findAxisIntersectionTime(yValues, timestamps, speed, epsilon, maxTimeMillis, stepMillis)

        if (xTarget == null || yTarget == null) return null

        // Определяем какая ось требует больше времени
        val (finalTime, targetX, targetY) = if (xTarget.interceptMillis > yTarget.interceptMillis) {
            // Если X требует больше времени, пересчитываем Y позицию для этого времени
            val newY = predictValue(yValues, timestamps, xTarget.interceptMillis)
            Triple(xTarget.interceptMillis, xTarget.value, newY)
        } else {
            // Если Y требует больше времени, пересчитываем X позицию для этого времени
            val newX = predictValue(xValues, timestamps, yTarget.interceptMillis)
            Triple(yTarget.interceptMillis, newX, yTarget.value)
        }

        Log.d("TT4", "finalTime: $finalTime, targetX: $targetX, targetY: $targetY")

        // Рассчитываем команды для моторов
        val xSteps = calculateSteps(0.5f, targetX, HORIZONTAL_RATIO)
        val ySteps = calculateSteps(0.5f, targetY, VERTICAL_RATIO)

        val xCommand = if (targetX > 0.5f) "R $xSteps" else "L $xSteps"
        val yCommand = if (targetY > 0.5f) "B $ySteps" else "T $ySteps"

        return Prediction(
            finalTime = finalTime,
            targetX = targetX,
            targetY = targetY,
            commands = xCommand to yCommand
        )
    }

    private fun findAxisIntersectionTime(
        values: List<Float>,
        timestamps: List<Long>,
        speed: Float,
        epsilon: Float,
        maxTimeMillis: Long,
        stepMillis: Long
    ): Target? {
        val maxSteps = (maxTimeMillis / stepMillis).toInt()

        for (i in 1..maxSteps) {
            val t = i * stepMillis
            val predictedValue = predictValue(values, timestamps, t)
            val needDistance = speed * t
            val centerValue = if (predictedValue > 0.5f) 0.5f + needDistance else 0.5f - needDistance

            if (abs(predictedValue - centerValue) < epsilon) {
                return Target(t, predictedValue)
            }
            if (predictedValue < 0 || predictedValue > 1) break
        }
        return null
    }

    private fun predictValue(values: List<Float>, timestamps: List<Long>, timeMillis: Long): Float {
        // Реализация предсказания позиции на основе истории
        // Для простоты используем линейную экстраполяцию по последним двум точкам
        if (values.size < 2 || timestamps.size < 2 || values.size != timestamps.size) {
            return values.lastOrNull() ?: 0.5f
        }

        val lastValue = values.last()
        val prevValue = values[values.size - 2]
        val lastTime = timestamps.last()
        val prevTime = timestamps[timestamps.size - 2]

        val timeDiff = lastTime - prevTime
        if (timeDiff == 0L) return lastValue // избегаем деления на ноль

        val speed = (lastValue - prevValue) / timeDiff
        return lastValue + speed * timeMillis
    }

    private fun calculateSteps(from: Float, to: Float, ratio: Double): Int {
        val distance = abs(to - from)
        // Конвертируем расстояние в шаги мотора (зависит от конкретной реализации)
        return (distance * 100 * ratio).toInt()
    }

    fun checkRun() {
        val now = System.currentTimeMillis()
        val detectedPoints = listOf(
            DetectedPoint(
                currentTimeMillis = now - 2000,
                cx = 0.1F,
                cy = 0.1F
            ),
            DetectedPoint(
                currentTimeMillis = now - 1000,
                cx = 0.2F,
                cy = 0.15F
            ),
            DetectedPoint(
                currentTimeMillis = now,
                cx = 0.3F,
                cy = 0.2F
            )
        )
        Log.d("TT4", "checkRun: ${calculateMotorCommands(detectedPoints)}")
    }
}

data class DetectedPoint(
    val currentTimeMillis: Long,
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(
        Date(currentTimeMillis)
    ),
    val cx: Float,
    val cy: Float
)

data class Prediction(
    val finalTime: Long,
    val targetX: Float,
    val targetY: Float,
    val commands: Pair<String, String>
)