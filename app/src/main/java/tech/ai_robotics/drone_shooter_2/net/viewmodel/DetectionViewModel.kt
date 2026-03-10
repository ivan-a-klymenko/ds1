package tech.ai_robotics.drone_shooter_2.net.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import tech.ai_robotics.drone_shooter_2.net.ClientConfig
import tech.ai_robotics.drone_shooter_2.net.mapper.toDto
import tech.ai_robotics.drone_shooter_2.net.model.BoundingBoxDto
import tech.ai_robotics.drone_shooter_2.net.model.ReportMessage
import tech.ai_robotics.drone_shooter_2.net.repository.DetectionRepository
import tech.ai_robotics.drone_shooter_2.object_detection.BoundingBox
import java.util.UUID

class DetectionViewModel(
    private val repo: DetectionRepository = DetectionRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(DetectionState())
    val state: StateFlow<DetectionState> = _state

    private var lastDetectJob: Job? = null

    fun onDetect(boxes: List<BoundingBox>) {
        // cancel previous timer
        lastDetectJob?.cancel()
        val dtos = boxes.map { it.toDto() }
        sendReport(dtos)
        // start 1s timer to send empty list if no new detects
        lastDetectJob = viewModelScope.launch {
            try {
                delay(1000)
                sendReport(emptyList())
            } catch (_: Exception) {
            }
        }
    }

    private fun boxesToJsonString(boxes: List<BoundingBoxDto>): String {
        val arr = JSONArray()
        for (b in boxes) {
            val o = JSONObject()
            o.put("x1", b.x1)
            o.put("y1", b.y1)
            o.put("x2", b.x2)
            o.put("y2", b.y2)
            o.put("cx", b.cx)
            o.put("cy", b.cy)
            o.put("w", b.w)
            o.put("h", b.h)
            o.put("cnf", b.cnf)
            o.put("cls", b.cls)
            o.put("clsName", b.clsName)
            arr.put(o)
        }
        return arr.toString()
    }

    private fun sendReport(boxes: List<BoundingBoxDto>) {
        _state.value = _state.value.copy(isSending = true, lastBoxes = boxes)
        val payloadMap = mutableMapOf<String, String>()
        // serialize boxes list into JSON string and put under key "boundingBoxes"
        payloadMap["boundingBoxes"] = boxesToJsonString(boxes)
        val report = ReportMessage(
            id = UUID.randomUUID().toString(),
            clientId = ClientConfig.CLIENT_ID,
            timestamp = System.currentTimeMillis(),
            payload = payloadMap
        )
        viewModelScope.launch {
            val ok = repo.sendReport(report)
            _state.value =
                _state.value.copy(isSending = false, lastSent = System.currentTimeMillis())
            if (!ok) {
                // log or handle failure
            }
        }
    }
}
