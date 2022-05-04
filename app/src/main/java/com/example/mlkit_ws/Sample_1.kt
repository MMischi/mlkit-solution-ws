/**
 * Object Detection - base model
 * detect object, classify/name detected objects, surround objects by rectangle
 */
package com.example.mlkit_ws

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class Sample_1 : Fragment(), View.OnClickListener {

    private lateinit var rootView: View

    // layout elements
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    private lateinit var btn: Button

    // needed for image
    private val REQUEST_CODE_PERMISSIONS: Int = 123
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val REQUEST_IMAGE_CAPTURE: Int = 234

    // for object detection
    private lateinit var image: Bitmap

    // object detector better usability
    private var COLORLIST_COLOR: ArrayList<Int> = arrayListOf(
        Color.BLUE,
        Color.RED,
        Color.YELLOW,
        Color.GREEN
    )
    private var COLORLIST_TEXT: ArrayList<String> = arrayListOf(
        "BLUE",
        "RED",
        "YELLOW",
        "GREEN"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_sample_1, container, false)

        // initialize layout elements
        imageView = rootView.findViewById(R.id.OD_imageView)
        textView = rootView.findViewById(R.id.OD_text)
        btn = rootView.findViewById(R.id.OD_imgBtn)

        // on click listener
        btn.setOnClickListener(this)

        return rootView
    }

    override fun onClick(v: View) {
        // check camera permissions and open it
        startActivity()
    }

// ================================================================================================
// CAMERA
// ================================================================================================
// Permissions
    // permissions base structure - open camera if permissions granted
    private fun startActivity() {
        if (allPermissionsGranted()) {
            /* permissions granted already */
            // start camera activity
            dispatchTakePictureIntent()
        } else {
            /* permissions not granted */
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    // check permissions
    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                this.requireActivity(), it
            ) == PackageManager.PERMISSION_GRANTED
        }

    // what should happen after the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                /* permissions granted yet */
                dispatchTakePictureIntent()
            } else {
                /* permissions denied */
                Toast.makeText(
                    context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

// Camera
    // take image
    //  we use camera which expires/is deprecated, but it is an easy and
    //  fast way which is sufficient for our application
    //
    //  camera opens camera application of device
    private fun dispatchTakePictureIntent() {
        // create camera instance
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            println("Camera not working: $e")
        }
    }

    // get thumbnail as result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE    // checks same request int
            && resultCode == Activity.RESULT_OK     // checks if activity was success
        ) {
            image = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(image)

            // analyse image with object detection
            objectDetection()
        }
    }

// ================================================================================================
// Object Detection
// ================================================================================================
    // object detection base structure
    private fun objectDetection() {
        // configure object detector
        val options: ObjectDetectorOptions = confOptions()

        // create instance of object detector
        val objectDetector: ObjectDetector = ObjectDetection.getClient(options)

        // prepare image (Bitmap to InputImage)
        val image: InputImage = prepImg()

        // process object detection
        objectDetector.process(image)
            .addOnSuccessListener { detectedObjects ->
                /* processing success */
                handleObjects(detectedObjects)
            }
            .addOnFailureListener { e ->
                /* processing failed */
                textView.text = "Something failed at processing"
                println(e)
            }
    }

    // configure the API for these use cases
    private fun confOptions(): ObjectDetectorOptions {
        return ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)   // if not real-time preview
            // .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)      // if real-time mode
            .enableMultipleObjects()    // enable detect more objects than one
            .enableClassification()     // enable object classification (classes: plant, food)
            .build()
    }

    // convert Bitmap to InputImage
    private fun prepImg(): InputImage {
        return InputImage.fromBitmap(image, 0)
    }

    // process results (detected objects)
    private fun handleObjects(detectedObjects: List<DetectedObject>) {
        var output: String = ""

        if (detectedObjects.isNotEmpty()) {
            for ((idx, obj) in detectedObjects.withIndex()) {
                output += "${COLORLIST_TEXT[idx]} \n"
                output += "${classifyObj(obj)} \n"

                val boundingBox: Rect = obj.boundingBox
                drawRect(idx, boundingBox)
            }
        } else {
            textView.text = "Nothing detected!"
        }
        textView.text = output
    }

    // name detected object
    private fun classifyObj(obj: DetectedObject): String {
        var result: String = ""

        for (label in obj.labels) {
            result += "${label.text} | ${label.confidence} \n"
        }
        return result
    }

    // draw rectangle around object
    private fun drawRect(idx: Int, boundingBox: Rect) {
        // create drawing board
        image = image.copy(Bitmap.Config.RGB_565, true)
        val canvas: Canvas = Canvas(image)

        // configure painting options
        val paint = Paint()
        paint.alpha = 0xA0                      // the transparency
        paint.color = COLORLIST_COLOR[idx]      // color is red
        paint.style = Paint.Style.STROKE        // stroke or fill or ...
        paint.strokeWidth = 1F                  // the stroke width

        // draw rectangle
        // create rectangle: var rec = Rect(left, top, right, bottom)
        // draw rectangle alternative: var rec = Rect(left, top, right, bottom, paint)
        canvas.drawRect(boundingBox, paint)

        // show rect on image view
        imageView.setImageBitmap(image)
    }
}