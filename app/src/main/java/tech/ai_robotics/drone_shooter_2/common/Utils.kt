package tech.ai_robotics.drone_shooter_2.common

import tech.ai_robotics.drone_shooter_2.object_detection.BoundingBox
import kotlin.math.abs

const val TARGET_DIFF = 0.02
const val FIRE_DIFF = 0.02
const val SPEED = 0.0001F
const val VERTICAL_RATIO = 1.2
const val HORIZONTAL_RATIO = 1.2
const val FIRE_TIME = 3000L

fun BoundingBox.isFireStart(): Boolean {
    val hDiff = abs(0.5F - cx)
    val vDiff = abs(0.5F - cy)
    return true

}

