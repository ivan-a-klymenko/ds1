package tech.ai_robotics.drone_shooter_2.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatTextView
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import tech.ai_robotics.drone_shooter_2.R
import tech.ai_robotics.drone_shooter_2.databinding.FragmentHomeBinding
import tech.ai_robotics.drone_shooter_2.object_detection.BoundingBox
import tech.ai_robotics.drone_shooter_2.object_detection.Constants.LABELS_PATH
import tech.ai_robotics.drone_shooter_2.object_detection.Constants.SPOT_3X_10X_20X
import tech.ai_robotics.drone_shooter_2.object_detection.Detector
import tech.ai_robotics.drone_shooter_2.ui.home.Direction.BOTTOM
import tech.ai_robotics.drone_shooter_2.ui.home.Direction.LEFT
import tech.ai_robotics.drone_shooter_2.ui.home.Direction.RIGHT
import tech.ai_robotics.drone_shooter_2.ui.home.Direction.STOP_X
import tech.ai_robotics.drone_shooter_2.ui.home.Direction.STOP_Y
import tech.ai_robotics.drone_shooter_2.ui.home.Direction.TOP
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.absoluteValue

const val TARGET_DIFF = 0.02

class HomeFragment : Fragment(), Detector.DetectorListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: Detector

    private lateinit var cameraExecutor: ExecutorService

    private var zoom = 3.0F
    private var targetHorizontal = 0.5
    private var targetVertical = 0.5

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        detector = Detector(requireContext(), SPOT_3X_10X_20X, LABELS_PATH, this)
        detector.setup()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        return root
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            btLeft.setOnClickListener {
                Log.d("HomeFragment", "btLeft pressed: ${LEFT.commandValue} 30")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.button_left) + " command removed (no Bluetooth)",
                    Toast.LENGTH_SHORT
                ).show()
            }
            btRight.setOnClickListener {
                Log.d("HomeFragment", "btRight pressed: ${RIGHT.commandValue} 30")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.button_right) + " command removed (no Bluetooth)",
                    Toast.LENGTH_SHORT
                ).show()
            }
            btTop.setOnClickListener {
                Log.d("HomeFragment", "btTop pressed: ${TOP.commandValue} 30")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.button_top) + " command removed (no Bluetooth)",
                    Toast.LENGTH_SHORT
                ).show()
            }
            btBottom.setOnClickListener {
                Log.d("HomeFragment", "btBottom pressed: ${BOTTOM.commandValue} 30")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.button_bottom) + " command removed (no Bluetooth)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()){
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop camera when fragment is paused
        cameraProvider?.unbindAll()
        camera = null
        preview = null
        imageAnalyzer = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.clear()
        cameraExecutor.shutdown()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider  = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview =  Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            detector.detect(rotatedBitmap)
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            camera?.cameraControl?.setZoomRatio(zoom)
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch(exc: Exception) {
            Log.e("HomeFragment", "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.CAMERA] == true) { startCamera() }
    }

    override fun onEmptyDetect() {
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        boundingBoxes.forEachIndexed { index, it ->
            Log.d("TTT onDetect", "$index $it")
        }
        if (!isResumed) return

        requireActivity().runOnUiThread {
            if (!isResumed) return@runOnUiThread
            handleDetectedObject(boundingBoxes)
            binding.inferenceTime.text =
                getString(R.string.inference_time_ms, inferenceTime.toInt())
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
        }
    }

    private fun handleDetectedObject(boundingBoxes: List<BoundingBox>) {
        val box = boundingBoxes.minByOrNull {
            it.cx
        }
        box?.let {
            val horizontalAngle = getHorizontalAngle(targetHorizontal - it.cx)
            val horizontalDirection = when  {
                it.cx < targetHorizontal - TARGET_DIFF -> RIGHT
                it.cx > targetHorizontal + TARGET_DIFF -> LEFT
                else -> STOP_X
            }
            val horizontalCommand = "${horizontalDirection.commandValue} $horizontalAngle"
            Log.d(
                "HomeFragment TT3",
                "cx: ${box.cx} cy: ${box.cy} horizontalCommand: $horizontalCommand"
            )

            val verticalAngle = getVerticalAngle(targetVertical - it.cy)
            val verticalDirection = when  {
                it.cy < targetVertical - TARGET_DIFF -> TOP
                it.cy > targetVertical - TARGET_DIFF -> BOTTOM
                else -> STOP_Y
            }
            val verticaCommand = "${verticalDirection.commandValue} $verticalAngle"
            Log.d(
                "HomeFragment TT3",
                "verticalCommand: $verticaCommand (not sent, Bluetooth removed)"
            )
        }
    }

    private fun getVerticalAngle(diff: Double): Int? {
        if (isResumed) {
            binding.vDiff.text =
                getString(R.string.v_diff, diff.times(-1).toString().substring(0, 10))
            showDiffColor(diff, binding.vDiff)
            showTargetColor(diff, binding.aimHorizontal)
        }
        val diffAbsoluteValue = diff.absoluteValue
        return when {
            diffAbsoluteValue in 0.35..0.5 -> 100
            0.2 < diffAbsoluteValue && diffAbsoluteValue < 0.35 -> 30
            0.1 < diffAbsoluteValue && diffAbsoluteValue <= 0.2 -> 10
            0.05 < diffAbsoluteValue && diffAbsoluteValue <= 0.1 -> 5
            diffAbsoluteValue in TARGET_DIFF..0.05 -> 5
            else -> null
        }
    }

    private fun getHorizontalAngle(diff: Double): Int {
        if (isResumed) {
            binding.hDiff.text =
                getString(R.string.h_diff, diff.times(-1).toString().substring(0, 10))
            showDiffColor(diff, binding.hDiff)
            showTargetColor(diff, binding.aimVertical)
        }
        val diffAbsoluteValue = diff.absoluteValue
        return when {
            diffAbsoluteValue in 0.45..0.5 -> 170
            0.4 < diffAbsoluteValue && diffAbsoluteValue < 0.45 -> 140
            diffAbsoluteValue in 0.35..0.4 -> 120
            0.3 < diffAbsoluteValue && diffAbsoluteValue < 0.35 -> 100
            diffAbsoluteValue in 0.25..0.3 -> 80
            0.2 < diffAbsoluteValue && diffAbsoluteValue < 0.25 -> 50
            diffAbsoluteValue in 0.15..0.2 -> 30
            0.1 < diffAbsoluteValue && diffAbsoluteValue <= 0.15 -> 10
            0.05 < diffAbsoluteValue && diffAbsoluteValue <= 0.1 -> 5
            diffAbsoluteValue in TARGET_DIFF..0.05 -> 5
            else -> 0
        }
    }

    private fun showDiffColor(diff: Double, textView: AppCompatTextView) {
        val colorId = if (diff.absoluteValue < TARGET_DIFF) R.color.colorRecieveText else R.color.colorPrimary
        textView.setTextColor(ContextCompat.getColor(requireContext(), colorId))
    }

    private fun showTargetColor(diff: Double, view: View) {
        val colorId =
            if (diff.absoluteValue < TARGET_DIFF) R.color.colorRecieveText else R.color.bounding_box_color
        view.setBackgroundResource(colorId)
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA
        ).toTypedArray()
    }
}


enum class Direction(val commandValue: String){
    LEFT("L"),
    RIGHT("R"),
    TOP("T"),
    BOTTOM("B"),
    STOP_Y("Y"),
    STOP_X("X");
}