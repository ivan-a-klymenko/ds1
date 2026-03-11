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
    private var retryJob: Job? = null

    companion object {
        private const val RETRY_DELAY_MS = 2000L
    }

    fun checkServerStatus(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repo.checkStatus()
                onResult(result)
            } catch (e: Exception) {
                onResult("error: ${e.message}")
            }
        }
    }

    fun onDetect(boxes: List<BoundingBox>) {
        lastDetectJob?.cancel()

        val dtos = boxes.map { it.toDto() }
        queueReport(dtos)

        lastDetectJob = viewModelScope.launch {
            try {
                delay(1000)
                queueReport(emptyList())
            } catch (_: Exception) {
            }
        }
    }

    private fun queueReport(boxes: List<BoundingBoxDto>) {
        _state.value = _state.value.copy(
            isSending = true,
            lastBoxes = boxes
        )

        val report = ReportMessage(
            id = UUID.randomUUID().toString(),
            clientId = ClientConfig.CLIENT_ID,
            timestamp = System.currentTimeMillis(),
            payload = ReportPayload(
                boundingBoxes = boxes
            )
        )

        viewModelScope.launch {
            repo.enqueueReport(report)

            val ok = repo.flushQueue()
            val hasPending = repo.hasPendingReports()

            _state.value = _state.value.copy(
                isSending = hasPending,
                lastSent = if (ok) System.currentTimeMillis() else _state.value.lastSent
            )

            if (hasPending) {
                ensureRetryLoop()
            }
        }
    }

    private fun ensureRetryLoop() {
        if (retryJob?.isActive == true) return

        retryJob = viewModelScope.launch {
            while (true) {
                val hasPending = repo.hasPendingReports()
                if (!hasPending) {
                    _state.value = _state.value.copy(isSending = false)
                    break
                }

                val ok = repo.flushQueue()
                val stillPending = repo.hasPendingReports()

                _state.value = _state.value.copy(
                    isSending = stillPending,
                    lastSent = if (ok && !stillPending) {
                        System.currentTimeMillis()
                    } else {
                        _state.value.lastSent
                    }
                )

                if (!stillPending) break

                delay(RETRY_DELAY_MS)
            }
        }
    }
}