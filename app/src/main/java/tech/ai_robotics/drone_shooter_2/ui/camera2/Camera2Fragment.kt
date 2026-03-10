// Camera2-based Fragment with detection logic from HomeFragment
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
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import tech.ai_robotics.drone_shooter_2.databinding.FragmentCamera2Binding
import tech.ai_robotics.drone_shooter_2.object_detection.BoundingBox
import tech.ai_robotics.drone_shooter_2.object_detection.Detector
import tech.ai_robotics.drone_shooter_2.ui.common.CameraDiagnostics
import tech.ai_robotics.drone_shooter_2.ui.common.Direction
import tech.ai_robotics.drone_shooter_2.ui.common.ImageUtils
import java.util.concurrent.Executors

private const val TAG = "Camera2Fragment"

class Camera2Fragment : Fragment() {

    private var _binding: FragmentCamera2Binding? = null
    private val binding get() = _binding!!

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null

    private lateinit var imageReader: ImageReader
    private lateinit var imageReaderHandlerThread: HandlerThread
    private lateinit var imageReaderHandler: Handler

    private lateinit var detector: Detector
    private var currentCameraId: String? = null
    private var cameraIdList: List<String> = emptyList()
    private var currentCameraIndex = 0

    private var currentZoom = 1.0f
    private var maxZoom = 1.0f

    private val processorCount = Runtime.getRuntime().availableProcessors()
    private val cameraExecutor = Executors.newFixedThreadPool(processorCount)
    private val coroutineScope = CoroutineScope(Dispatchers.Default.limitedParallelism(processorCount))

    private var hCommand: Direction? = null
    private var vCommand: Direction? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCamera2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        detector = Detector(requireContext(), "spot_3x_10x_20x.tflite", "labels.txt", object : Detector.DetectorListener {
            override fun onEmptyDetect() {
//                coroutineScope.launch(Dispatchers.Main) {
//                    hCommand = null
//                    vCommand = null
//                    binding.overlayView.clear()
//                }
            }
            override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
                requireActivity().runOnUiThread {
                    if (hCommand == null || vCommand == null) {
                        boundingBoxes.forEachIndexed { index, it ->
                            Log.d("TTT onDetect", "hCommand $hCommand vCommand $vCommand")
                            Log.d("TTT onDetect", "$index $it")
                        }
                    }
                    handleDetectedObject(boundingBoxes)
//                    binding.inferenceTime.text = "${inferenceTime}ms"
                    binding.overlayView.apply {
                        setResults(boundingBoxes)
                        invalidate()
                    }
                }
            }
        })
        detector.setup()
        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraIdList = cameraManager.cameraIdList.filter {
            cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
        CameraDiagnostics.logAllCameraInfo(requireContext())
        binding.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                openCameraByIndex(currentCameraIndex)
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                closeCamera()
            }
        })

        binding.btnSwitchCamera.setOnClickListener {
            currentCameraIndex = (currentCameraIndex + 1) % cameraIdList.size
            closeCamera()
            openCameraByIndex(currentCameraIndex)
        }

        binding.zoomSeekBar.max = 100
        binding.zoomSeekBar.progress = 100
        val layoutParams = binding.zoomSeekBar.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin += 48
        binding.zoomSeekBar.layoutParams = layoutParams

        binding.zoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val ratio = 1f + (maxZoom - 1f) * (progress / 100f)
                currentZoom = ratio
                binding.zoomLabel.text = "Zoom: %.1fx".format(ratio)
                updateZoom()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun handleDetectedObject(boundingBoxes: List<BoundingBox>) {
        boundingBoxes.minByOrNull {
            it.cx
        }
//        box?.let {
//            val horizontalAngle = getAngle((Storage.targetHorizontal - it.cx).absoluteValue)
//            val horizontalDirection = when  {
//                it.cx < 0.5 -> LEFT
//                it.cx > 0.5 -> RIGHT
//                else -> null
//            }
//            horizontalAngle?.let { angle ->
//                horizontalDirection?.let { direction ->
//                    val horizontalCommand = "${direction.commandValue} $angle"
//                    if (hCommand == null && connected == Connected.TRUE) {
//                        hCommand = horizontalDirection
//                        send(horizontalCommand)
//                    }
//                }
//            }
//
//            val verticalAngle = getAngle((Storage.targetVertical - it.cy).absoluteValue)
//            val verticalDirection = when  {
//                it.cy < 0.5 -> TOP
//                it.cy > 0.5 -> BOTTOM
//                else -> null
//            }
//            verticalAngle?.let { angle ->
//                verticalDirection?.let { direction ->
//                    val verticaCommand = "${direction.commandValue} $angle"
//                    if (vCommand == null && connected == Connected.TRUE) {
//                        vCommand = verticalDirection
//                        send(verticaCommand)
//                    }
//                }
//            }
//        }
    }

    private fun getAngle(diff: Double): Int? {
        return when {
            diff in 0.3..0.5 -> 10
            diff in 0.15..0.3 -> 5
            diff in 0.05..0.15 -> 1
            else -> null
        }
    }

    private fun openCameraByIndex(index: Int) {
        if (index in cameraIdList.indices) {
            currentCameraId = cameraIdList[index]
            binding.cameraLabel.text = "Camera: $currentCameraId"
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
        val surface = binding.surfaceView.holder.surface
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
        _binding = null
    }

    private fun send(str: String) {
        Log.d("$TAG TTT", "send: $str")
//        if (connected != Connected.TRUE) {
//            Toast.makeText(activity, "not connected", Toast.LENGTH_SHORT).show()
//            return
//        }
//        try {
//            val msg: String
//            val data: ByteArray
//            if (hexEnabled) {
//                val sb = StringBuilder()
//                TextUtil.toHexString(sb, TextUtil.fromHexString(str))
//                TextUtil.toHexString(sb, newline.toByteArray())
//                msg = sb.toString()
//                data = TextUtil.fromHexString(msg)
//            } else {
//                msg = str
//                data = (str + newline).toByteArray()
//            }
//            val spn = SpannableStringBuilder(msg + '\n')
//            spn.setSpan(
//                ForegroundColorSpan(resources.getColor(R.color.colorSendText)),
//                0,
//                spn.length,
//                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//            )
////            receiveText.append(spn)
//            service!!.write(data)
//        } catch (e: java.lang.Exception) {
//            onSerialIoError(e)
//        }
    }
}
