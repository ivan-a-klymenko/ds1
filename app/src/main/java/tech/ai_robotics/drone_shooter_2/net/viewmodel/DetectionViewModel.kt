package tech.ai_robotics.drone_shooter_2.net.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import tech.ai_robotics.drone_shooter_2.net.ClientConfig
import tech.ai_robotics.drone_shooter_2.net.mapper.toDto
import tech.ai_robotics.drone_shooter_2.net.model.BoundingBoxDto
import tech.ai_robotics.drone_shooter_2.net.model.ReportMessage
import tech.ai_robotics.drone_shooter_2.net.model.ReportPayload
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
        lastDetectJob?.cancel()

        val dtos = boxes.map { it.toDto() }
        sendReport(dtos)

        lastDetectJob = viewModelScope.launch {
            try {
                delay(1000)
                sendReport(emptyList())
            } catch (_: Exception) {
            }
        }
    }

    private fun sendReport(boxes: List<BoundingBoxDto>) {
        _state.value = _state.value.copy(isSending = true, lastBoxes = boxes)

        val report = ReportMessage(
            id = UUID.randomUUID().toString(),
            clientId = ClientConfig.CLIENT_ID,
            timestamp = System.currentTimeMillis(),
            payload = ReportPayload(
                boundingBoxes = boxes
            )
        )

        viewModelScope.launch {
            val ok = repo.sendReport(report)
            _state.value = _state.value.copy(
                isSending = false,
                lastSent = System.currentTimeMillis()
            )
            if (!ok) {
                // log or handle failure
            }
        }
    }
}