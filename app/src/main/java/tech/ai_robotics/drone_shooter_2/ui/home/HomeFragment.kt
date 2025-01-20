package tech.ai_robotics.drone_shooter_2.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Typeface
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.Image.Plane
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.mediapipe.tasks.vision.core.RunningMode
import tech.ai_robotics.drone_shooter_2.R
import tech.ai_robotics.drone_shooter_2.databinding.FragmentHomeBinding
import tech.ai_robotics.drone_shooter_2.drawing.BorderedText
import tech.ai_robotics.drone_shooter_2.drawing.MultiBoxTracker
import tech.ai_robotics.drone_shooter_2.drawing.OverlayView
import tech.ai_robotics.drone_shooter_2.live_feed.CameraConnectionFragment
import tech.ai_robotics.drone_shooter_2.live_feed.ImageUtils.convertYUV420ToARGB8888
import tech.ai_robotics.drone_shooter_2.live_feed.ImageUtils.getTransformationMatrix
import tech.ai_robotics.drone_shooter_2.ml.ObjectDetectorHelper
import tech.ai_robotics.drone_shooter_2.ml.Recognition

class HomeFragment : Fragment(), ImageReader.OnImageAvailableListener, ObjectDetectorHelper.DetectorListener {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var frameToCropTransform: Matrix? = null
    private var cropToFrameTransform: Matrix? = null

    // Configuration values for the prepackaged SSD model.
    private val MAINTAIN_ASPECT = false
    private val TEXT_SIZE_DIP = 10f

    var trackingOverlay: OverlayView? = null
    private var borderedText: BorderedText? = null
    private lateinit var objectDetectorHelper: ObjectDetectorHelper

    var previewHeight = 0
    var previewWidth = 0
    private var sensorOrientation = 0

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
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requireContext().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
            ) {
                val permission = arrayOf(
                    Manifest.permission.CAMERA
                )
                requestPermissions(permission, 1122)
            } else {
                //TODO show live camera footage
                setFragment()
            }
        }else{
            //TODO show live camera footage
            setFragment()
        }

        objectDetectorHelper =
            ObjectDetectorHelper(
                context = requireContext(),
                threshold = 0.8f,
                currentDelegate = ObjectDetectorHelper.DELEGATE_CPU,
                modelName = "cars_v1_2.tflite",
                maxResults = ObjectDetectorHelper.MAX_RESULTS_DEFAULT,
                runningMode = RunningMode.IMAGE,
                objectDetectorListener = this
            )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //TODO show live camera footage
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //TODO show live camera footage
            setFragment()
        } else {
            requireActivity().finish()
        }
    }
    protected fun setFragment() {
        val manager =
            requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var cameraId: String? = null
        try {
            cameraId = manager.cameraIdList[0]
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        val fragment: Fragment
        val camera2Fragment = CameraConnectionFragment.newInstance(
            object :
                CameraConnectionFragment.ConnectionCallback {
                override fun onPreviewSizeChosen(size: Size?, rotation: Int) {
                    previewHeight = size!!.height
                    previewWidth = size.width
                    val textSizePx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        TEXT_SIZE_DIP,
                        resources.displayMetrics
                    )
                    borderedText = BorderedText(textSizePx)
                    borderedText!!.setTypeface(Typeface.MONOSPACE)
                    tracker = MultiBoxTracker(requireContext())

                    val cropSize = 300
                    previewWidth = size.width
                    previewHeight = size.height
                    sensorOrientation = rotation - getScreenOrientation()
                    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)

                    frameToCropTransform = getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT
                    )
                    cropToFrameTransform = Matrix()
                    frameToCropTransform!!.invert(cropToFrameTransform)

                    trackingOverlay =
                        requireView().findViewById<View>(R.id.tracking_overlay) as OverlayView
                    trackingOverlay!!.addCallback(
                        object : OverlayView.DrawCallback {
                            override fun drawCallback(canvas: Canvas?) {
                                tracker!!.draw(canvas!!)
                            }
                        })
                    tracker!!.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation)
                }
            },
            this,
            R.layout.camera_fragment,
            Size(640, 480)
        )
        camera2Fragment.setCamera(cameraId)
        fragment = camera2Fragment
        childFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
    }


    //TODO getting frames of live camera footage and passing them to model
    private var isProcessingFrame = false
    private val yuvBytes = arrayOfNulls<ByteArray>(3)
    private var rgbBytes: IntArray? = null
    private var yRowStride = 0
    private var postInferenceCallback: Runnable? = null
    private var imageConverter: Runnable? = null
    private var rgbFrameBitmap: Bitmap? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onImageAvailable(reader: ImageReader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return
        }
        if (rgbBytes == null) {
            rgbBytes = IntArray(previewWidth * previewHeight)
        }
        try {
            val image = reader.acquireLatestImage() ?: return
            if (isProcessingFrame) {
                image.close()
                return
            }
            isProcessingFrame = true
            val planes = image.planes
            fillBytes(planes, yuvBytes)
            yRowStride = planes[0].rowStride
            val uvRowStride = planes[1].rowStride
            val uvPixelStride = planes[1].pixelStride
            imageConverter = Runnable {
                convertYUV420ToARGB8888(
                    yuvBytes[0]!!,
                    yuvBytes[1]!!,
                    yuvBytes[2]!!,
                    previewWidth,
                    previewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    rgbBytes!!
                )
            }
            postInferenceCallback = Runnable {
                image.close()
                isProcessingFrame = false
            }
            processImage()
        } catch (e: Exception) {
            Log.d("tryError", e.message + "abc ")
            return
        }
    }


    var croppedBitmap: Bitmap? = null
    private var tracker: MultiBoxTracker? = null
    @RequiresApi(Build.VERSION_CODES.N)
    fun processImage() {
        imageConverter!!.run()
        rgbFrameBitmap =
            Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
        rgbFrameBitmap!!.setPixels(rgbBytes!!, 0, previewWidth, 0, 0, previewWidth, previewHeight)

        val canvas = Canvas(croppedBitmap!!)
        canvas.drawBitmap(rgbFrameBitmap!!, frameToCropTransform!!, null)

        //TODO pass image to model and get results
        var resultBundle = objectDetectorHelper.detectImage(rgbFrameBitmap!!)
        if(resultBundle != null){
            var results = ArrayList<Recognition>()
            var resultsList = resultBundle.results
            for(singleResult in resultsList){
                var detections = singleResult.detections()
                for(singleDetection in detections){
                    singleDetection.boundingBox()
                    var categorieslist = singleDetection.categories()
                    var objectName = ""
                    var objectScore = 0f
                    for(singleCategory in categorieslist){
                        Log.d("tryRess","${singleCategory.categoryName()} x: ${singleDetection.boundingBox().centerX()} y: ${singleDetection.boundingBox().centerY()}")
                        if(singleCategory.score()>objectScore){
                            objectScore = singleCategory.score()
                            objectName = singleCategory.categoryName()
                        }
                    }
                    var recognition =
                        Recognition(
                            "result",
                            objectName,
                            objectScore,
                            singleDetection.boundingBox()
                        )
                    results.add(recognition)
                }
            }
            tracker?.trackResults(results, 10)
            trackingOverlay?.postInvalidate()
            postInferenceCallback!!.run()
        }


    }

    protected fun fillBytes(
        planes: Array<Plane>,
        yuvBytes: Array<ByteArray?>
    ) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (i in planes.indices) {
            val buffer = planes[i].buffer
            if (yuvBytes[i] == null) {
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            buffer[yuvBytes[i]]
        }
    }

    protected fun getScreenOrientation(): Int {
        return when (requireActivity().windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_270 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_90 -> 90
            else -> 0
        }
    }

    override fun onError(error: String, errorCode: Int) {
        Log.d("TTT","onError: $error")
    }

    override fun onResults(resultBundle: ObjectDetectorHelper.ResultBundle) {
        Log.d("TTT","onResults: $resultBundle")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}