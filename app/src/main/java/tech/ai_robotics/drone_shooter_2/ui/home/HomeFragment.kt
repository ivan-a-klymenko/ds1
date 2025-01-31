package tech.ai_robotics.drone_shooter_2.ui.home

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import tech.ai_robotics.drone_shooter_2.R
import tech.ai_robotics.drone_shooter_2.bluetooth.BluetoothStorage
import tech.ai_robotics.drone_shooter_2.bluetooth.Connected.FALSE
import tech.ai_robotics.drone_shooter_2.bluetooth.Connected.PENDING
import tech.ai_robotics.drone_shooter_2.bluetooth.Connected.TRUE
import tech.ai_robotics.drone_shooter_2.bluetooth.SerialListener
import tech.ai_robotics.drone_shooter_2.bluetooth.SerialService
import tech.ai_robotics.drone_shooter_2.bluetooth.SerialService.SerialBinder
import tech.ai_robotics.drone_shooter_2.bluetooth.SerialSocket
import tech.ai_robotics.drone_shooter_2.bluetooth.TextUtil
import tech.ai_robotics.drone_shooter_2.databinding.FragmentHomeBinding
import tech.ai_robotics.drone_shooter_2.object_detection.BoundingBox
import tech.ai_robotics.drone_shooter_2.object_detection.Constants.LABELS_PATH
import tech.ai_robotics.drone_shooter_2.object_detection.Constants.MODEL_PATH
import tech.ai_robotics.drone_shooter_2.object_detection.Detector
import java.util.ArrayDeque
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "HomeFragment"
private const val STOP = "pp"
private const val HORIZONTAL_LEFT = "ff"
private const val HORIZONTAL_RIGHT = "dd"
private const val VERTICAL_TOP = "ff"
private const val VERTICAL_BOTTOM = "ff"

class HomeFragment : Fragment(), Detector.DetectorListener, SerialListener, ServiceConnection {

    private var initialStart = true
    private val hexEnabled: Boolean = false
    private var pendingNewline = false
    private val newline = TextUtil.newline_crlf
    private var service: SerialService? = null
    private var connected = FALSE
    private var deviceAddress: String? = null
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: Detector

    private lateinit var cameraExecutor: ExecutorService

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        requireActivity().bindService(
            Intent(requireActivity(), SerialService::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        homeViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }

        detector = Detector(requireContext(), MODEL_PATH, LABELS_PATH, this)
        detector.setup()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        deviceAddress = BluetoothStorage.bluetoothDeviceId
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            btStop.setOnClickListener {
                send(STOP)
            }
            btLeft.setOnClickListener {
                send(HORIZONTAL_LEFT)
            }
            btRight.setOnClickListener {
                send(HORIZONTAL_RIGHT)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        service?.attach(this)
            ?: requireActivity().startService(
                Intent(
                    requireActivity(),
                    SerialService::class.java
                )
            )
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()){
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
        if (initialStart && service != null) {
            initialStart = false
            requireActivity().runOnUiThread { this.connect() }
        }
    }

    override fun onStop() {
        if (service != null && !requireActivity().isChangingConfigurations) service?.detach()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        if (connected != FALSE) disconnect()
        requireActivity().stopService(Intent(activity, SerialService::class.java))
        super.onDestroy()
        detector.clear()
        cameraExecutor.shutdown()
    }

    override fun onDetach() {
        try {
            requireActivity().unbindService(this)
        } catch (ignored: java.lang.Exception) {}
        super.onDetach()
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

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
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
//        binding.overlay.invalidate()
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        requireActivity().runOnUiThread {
            boundingBoxes.forEachIndexed { index, it ->
                Log.d("TTT onDetect", "$index $it")
            }
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
        }
    }

    private fun disconnect() {
        connected = FALSE
        service?.disconnect()
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA
        ).toTypedArray()
    }

    override fun onSerialConnect() {
        Log.d("$TAG TTT", "onSerialConnect")
        status("connected")
        connected = TRUE
    }

    override fun onSerialConnectError(e: java.lang.Exception) {
        Log.d("$TAG TTT", "onSerialConnectError: ${e.message}")
        status("connection failed: " + e.message)
        disconnect()
    }

    override fun onSerialRead(data: ByteArray) {
        val datas = ArrayDeque<ByteArray>()
        datas.add(data)
        receive(datas)
    }

    override fun onSerialRead(data: ArrayDeque<ByteArray>) {
        receive(data)
    }

    override fun onSerialIoError(e: java.lang.Exception) {
        status("connection lost: " + e.message)
        disconnect()
    }

    private fun receive(datas: ArrayDeque<ByteArray>) {
        val spn = SpannableStringBuilder()
        for (data in datas) {
            if (hexEnabled) {
                spn.append(TextUtil.toHexString(data)).append('\n')
            } else {
                var msg = String(data)
                if (newline == TextUtil.newline_crlf && msg.length > 0) {
                    // don't show CR as ^M if directly before LF
                    msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf)
                    // special handling if CR and LF come in separate fragments
                    if (pendingNewline && msg[0] == '\n') {
                        if (spn.length >= 2) {
                            spn.delete(spn.length - 2, spn.length)
                        } else {
                            val edt: Editable? = BluetoothStorage.receivedText
                            if (edt != null && edt.length >= 2) edt.delete(
                                edt.length - 2,
                                edt.length
                            )
                        }
                    }
                    pendingNewline = msg[msg.length - 1] == '\r'
                }
                spn.append(TextUtil.toCaretString(msg, newline.length != 0))
            }
        }
        Log.d("$TAG TTT receive", spn.toString())
    }

    private fun status(str: String) {
        val spn = SpannableStringBuilder(str + '\n')
        spn.setSpan(
            ForegroundColorSpan(resources.getColor(R.color.colorStatusText)),
            0,
            spn.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
//        receiveText.append(spn)
        Log.d("$TAG TTT status", spn.toString())
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder) {
        Log.d("$TAG TTT", "onServiceConnected")
        service = (binder as SerialBinder).service
        service?.attach(this)
        if (initialStart && isResumed) {
            initialStart = false
            requireActivity().runOnUiThread { this.connect() }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d("$TAG TTT", "onServiceDisconnected")
        service = null
    }

    private fun connect() {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            Log.d("$TAG TTT", "connect: deviceAddress $deviceAddress device $device")
            status("connecting...")
            connected = PENDING
            val socket = SerialSocket(requireActivity().applicationContext, device)
            service?.connect(socket)
        } catch (e: java.lang.Exception) {
            onSerialConnectError(e)
        }
    }

    private fun send(str: String) {
        if (connected != TRUE) {
            Toast.makeText(activity, "not connected", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val msg: String
            val data: ByteArray
            if (hexEnabled) {
                val sb = StringBuilder()
                TextUtil.toHexString(sb, TextUtil.fromHexString(str))
                TextUtil.toHexString(sb, newline.toByteArray())
                msg = sb.toString()
                data = TextUtil.fromHexString(msg)
            } else {
                msg = str
                data = (str + newline).toByteArray()
            }
            val spn = SpannableStringBuilder(msg + '\n')
            spn.setSpan(
                ForegroundColorSpan(resources.getColor(R.color.colorSendText)),
                0,
                spn.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
//            receiveText.append(spn)
            service!!.write(data)
        } catch (e: java.lang.Exception) {
            onSerialIoError(e)
        }
    }
}