package tech.ai_robotics.drone_shooter_2.net.viewmodel

import tech.ai_robotics.drone_shooter_2.net.model.BoundingBoxDto

data class DetectionState(
    val lastSent: Long = 0L,
    val lastBoxes: List<BoundingBoxDto> = emptyList(),
    val isSending: Boolean = false
)

