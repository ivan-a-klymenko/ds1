package tech.ai_robotics.drone_shooter_2.ui.common

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log

object CameraDiagnostics {

    fun logAllCameraInfo(context: Context) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIdList = cameraManager.cameraIdList

        for (cameraId in cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)

            val lensFacing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_BACK -> "Back"
                CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                else -> "Unknown"
            }

            val maxDigitalZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
            val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS) ?: floatArrayOf()

            val physicalCameraIds = characteristics.physicalCameraIds
            val isLogical = physicalCameraIds.isNotEmpty()

            Log.i("CameraInfo", "====== Camera ID: $cameraId ======")
            Log.i("CameraInfo", "Facing: $lensFacing")
            Log.i("CameraInfo", "Is logical camera: $isLogical")
            if (isLogical) {
                Log.i("CameraInfo", "Physical camera IDs: $physicalCameraIds")
            }
            Log.i("CameraInfo", "Max digital zoom: $maxDigitalZoom")
            Log.i("CameraInfo", "Available focal lengths: ${focalLengths.joinToString(", ")}")
            Log.i("CameraInfo", "===================================")
        }
    }
}
