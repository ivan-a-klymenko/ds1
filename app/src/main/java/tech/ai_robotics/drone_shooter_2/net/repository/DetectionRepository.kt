package tech.ai_robotics.drone_shooter_2.net.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
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

                val root = JSONObject()
                root.put("id", report.id)
                root.put("clientId", report.clientId)
                root.put("timestamp", report.timestamp)

                val payloadObj = JSONObject()
                val boxesArr = JSONArray()

                for (b in report.payload.boundingBoxes) {
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
                    boxesArr.put(o)
                }

                payloadObj.put("boundingBoxes", boxesArr)
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