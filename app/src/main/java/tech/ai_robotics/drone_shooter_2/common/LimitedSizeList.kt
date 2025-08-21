package tech.ai_robotics.drone_shooter_2.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale

class LimitedSizeList<T>(private val maxSize: Int) : LinkedList<T>() {

    override fun add(element: T): Boolean {
        if (size == maxSize) {
            removeFirst()
        }
        return super.add(element)
    }
}

data class Point(
    val currentTimeMillis: Long,
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(currentTimeMillis)),
    val value: Float
)

data class Target(
    val interceptMillis: Long,
    val value: Float
)

fun predictValue(points: List<Point>, deltaMillis: Long): Float {
    if (points.size < 2) throw IllegalArgumentException("Нужно минимум 2 точки")

    val speeds = mutableListOf<Float>()

    for (i in 1 until points.size) {
        for (j in 0 until i) {
            val p1 = points[j]
            val p2 = points[i]

            val dt = (p2.currentTimeMillis - p1.currentTimeMillis).toFloat()
            if (dt != 0f) {
                val dv = p2.value - p1.value
                val v = dv / dt // скорость в единицах value/мс
                speeds.add(v)
            }
        }
    }

    if (speeds.isEmpty()) throw IllegalStateException("Невозможно вычислить скорости")

    val averageSpeed = speeds.average().toFloat()

    val lastPoint = points.last()
    return lastPoint.value + averageSpeed * deltaMillis
}

fun findIntersectionTime(
    points: List<Point>,
    speed2: Float = SPEED,
    epsilon: Float = TARGET_DIFF.toFloat(),
    maxTimeMillis: Long = 60_000L,
    stepMillis: Long = 10L
): Target? {
    val maxSteps = (maxTimeMillis / stepMillis).toInt()

    for (i in 1..maxSteps) {
        val t = i * stepMillis
        val predictedValue = predictValue(points, t)
        val needDistance = speed2 * t
        val value2 = if (predictedValue > 0.5) 0.5f + needDistance else 0.5f - needDistance

        val abs = kotlin.math.abs(predictedValue - value2)
        if (abs < epsilon) {
            return Target(t, predictedValue)
        }
        if (predictedValue > 1) break
    }

    return null // не нашли за maxTimeMillis
}




