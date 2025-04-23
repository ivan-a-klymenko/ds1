// Camera2-based Fragment with camera switching, zoom control, and UI
package tech.ai_robotics.drone_shooter_2.ui.camera2

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import tech.ai_robotics.drone_shooter_2.object_detection.Detector
import tech.ai_robotics.drone_shooter_2.ui.common.ImageUtils
import java.util.concurrent.Executors

class Camera2Fragment : Fragment() {

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null

    private lateinit var imageReader: ImageReader
    private lateinit var imageReaderHandlerThread: HandlerThread
    private lateinit var imageReaderHandler: Handler

    private lateinit var surfaceView: SurfaceView
    private lateinit var zoomSeekBar: SeekBar
    private lateinit var zoomLabel: TextView
    private lateinit var cameraLabel: TextView

    private lateinit var detector: Detector
    private var currentCameraId: String? = null
    private var cameraIdList: List<String> = emptyList()
    private var currentCameraIndex = 0

    private var currentZoom = 1.0f
    private var maxZoom = 1.0f

    private val processorCount = Runtime.getRuntime().availableProcessors()
    private val cameraExecutor = Executors.newFixedThreadPool(processorCount)
    private val coroutineScope = CoroutineScope(Dispatchers.Default.limitedParallelism(processorCount))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootLayout = FrameLayout(requireContext())
        surfaceView = SurfaceView(requireContext())
        zoomSeekBar = SeekBar(requireContext())
        zoomLabel = TextView(requireContext())
        cameraLabel = TextView(requireContext())

        zoomSeekBar.max = 100
        zoomSeekBar.progress = 100
        zoomLabel.text = "Zoom: 1.0x"
        cameraLabel.text = "Camera: -"

        zoomSeekBar.translationY = 50f
        zoomLabel.translationY = 100f
        cameraLabel.translationY = 150f

        rootLayout.addView(surfaceView)
        rootLayout.addView(zoomSeekBar)
        rootLayout.addView(zoomLabel)
        rootLayout.addView(cameraLabel)

        return rootLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        detector = Detector(requireContext(), "model.tflite", "labels.txt", object : Detector.DetectorListener {
            override fun onEmptyDetect() { Log.d("Detector", "No objects detected") }
            override fun onDetect(boundingBoxes: List<tech.ai_robotics.drone_shooter_2.object_detection.BoundingBox>, inferenceTime: Long) {
                Log.d("Detector", "Detection in $inferenceTime ms, boxes: ${boundingBoxes.size}")
            }
        })
        detector.setup()
        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraIdList = cameraManager.cameraIdList.filter {
            cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
        logAvailableCameras()
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                openCameraByIndex(currentCameraIndex)
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                closeCamera()
            }
        })

        surfaceView.setOnClickListener {
            currentCameraIndex = (currentCameraIndex + 1) % cameraIdList.size
            closeCamera()
            openCameraByIndex(currentCameraIndex)
        }

        zoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val ratio = 1f + (maxZoom - 1f) * (progress / 100f)
                currentZoom = ratio
                zoomLabel.text = "Zoom: %.1fx".format(ratio)
                updateZoom()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun logAvailableCameras() {
        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val zoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
            Log.d("Camera2", "CameraId: $id, Facing: $facing, MaxZoom: $zoom")
        }
    }

    private fun openCameraByIndex(index: Int) {
        if (index in cameraIdList.indices) {
            currentCameraId = cameraIdList[index]
            cameraLabel.text = "Camera: $currentCameraId"
            openCamera(cameraIdList[index])
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(cameraId: String) {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
        currentZoom = maxZoom
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        val previewSize = map.getOutputSizes(ImageFormat.YUV_420_888).maxByOrNull { it.height * it.width } ?: Size(640, 480)

        imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.YUV_420_888, 2)

        imageReaderHandlerThread = HandlerThread("ImageReaderThread").apply { start() }
        imageReaderHandler = Handler(imageReaderHandlerThread.looper)

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val bitmap = ImageUtils.imageToBitmap(image)
            image.close()
            coroutineScope.launch { detector.detect(bitmap) }
        }, imageReaderHandler)

        cameraManager.openCamera(cameraId, ContextCompat.getMainExecutor(requireContext()), object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                cameraDevice = device
                startPreview()
            }
            override fun onDisconnected(device: CameraDevice) { device.close() }
            override fun onError(device: CameraDevice, error: Int) { device.close() }
        })
    }

    private fun startPreview() {
        val surface = surfaceView.holder.surface
        previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(surface)
            addTarget(imageReader.surface)
            set(CaptureRequest.SCALER_CROP_REGION, null)
            set(CaptureRequest.CONTROL_ZOOM_RATIO, currentZoom)
        }

        cameraDevice?.createCaptureSession(listOf(surface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                previewRequestBuilder?.build()?.let {
                    session.setRepeatingRequest(it, null, null)
                }
            }
            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e("Camera2", "Configuration failed")
            }
        }, null)
    }

    private fun updateZoom() {
        previewRequestBuilder?.set(CaptureRequest.CONTROL_ZOOM_RATIO, currentZoom)
        previewRequestBuilder?.build()?.let {
            captureSession?.setRepeatingRequest(it, null, null)
        }
    }

    private fun closeCamera() {
        captureSession?.close()
        cameraDevice?.close()
        imageReader.close()
        imageReaderHandlerThread.quitSafely()
        imageReaderHandlerThread.join()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detector.clear()
        coroutineScope.cancel()
        cameraExecutor.shutdown()
    }
}
