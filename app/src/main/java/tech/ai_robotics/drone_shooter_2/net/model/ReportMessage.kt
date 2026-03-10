package tech.ai_robotics.drone_shooter_2.net.model

data class ReportPayload(
    val boundingBoxes: List<BoundingBoxDto>
)

data class ReportMessage(
    val id: String,
    val clientId: String,
    val timestamp: Long,
    val payload: ReportPayload
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