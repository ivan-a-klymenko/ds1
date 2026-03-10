package tech.ai_robotics.drone_shooter_2.net.mapper

import tech.ai_robotics.drone_shooter_2.net.model.BoundingBoxDto
import tech.ai_robotics.drone_shooter_2.object_detection.BoundingBox

fun BoundingBox.toDto(): BoundingBoxDto {
    return BoundingBoxDto(
        x1 = this.x1,
        y1 = this.y1,
        x2 = this.x2,
        y2 = this.y2,
        cx = this.cx,
        cy = this.cy,
        w = this.w,
        h = this.h,
        cnf = this.cnf,
        cls = this.cls,
        clsName = this.clsName
    )
}

