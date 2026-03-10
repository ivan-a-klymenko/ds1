package tech.ai_robotics.drone_shooter_2.net.viewmodel

import tech.ai_robotics.drone_shooter_2.object_detection.BoundingBox

sealed class DetectionIntent {
    data class Detected(val boundingBoxes: List<BoundingBox>) : DetectionIntent()
}

