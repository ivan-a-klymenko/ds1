package tech.ai_robotics.drone_shooter_2.net.model

data class ReportMessage(
    val id: String,                 // уникальный ID конкретного сообщения
    val clientId: String,           // постоянный ID устройства
    val timestamp: Long,
    val payload: Map<String, String>
)

data class BoundingBoxDto(
    val x1: Float = 0.0F,
    val y1: Float = 0.0F,
    val x2: Float = 0.0F,
    val y2: Float = 0.0F,
    val cx: Float,
    val cy: Float,
    val w: Float = 0.0F,
    val h: Float = 0.0F,
    val cnf: Float = 0.0F,
    val cls: Int = 0,
    val clsName: String = ""
)