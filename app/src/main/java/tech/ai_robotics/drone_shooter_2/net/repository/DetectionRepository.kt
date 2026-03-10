package tech.ai_robotics.drone_shooter_2.net.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import tech.ai_robotics.drone_shooter_2.net.ClientConfig
import tech.ai_robotics.drone_shooter_2.net.model.ReportMessage

class DetectionRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendReport(report: ReportMessage): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = ClientConfig.SERVER_HOST + ClientConfig.REPORT_PATH
                // build JSON manually
                val root = JSONObject()
                root.put("id", report.id)
                root.put("timestamp", report.timestamp)
                val payloadObj = JSONObject()
                for ((k, v) in report.payload) {
                    payloadObj.put(k, v)
                }
                root.put("payload", payloadObj)

                val body = root.toString().toRequestBody(mediaType)
                val req = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()
                client.newCall(req).execute().use { resp ->
                    resp.isSuccessful
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
