/**
 * Text Recognition - Language ID - Translate Text
 * detect Text, identify language of detected text, translate text
 * e.g. travel translator
 *
 * here we use cameraX, see:
 *      - AndroidManifest.xml
 *      - build.grandle:app
 */
package com.example.mlkit_ws

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.nl.entityextraction.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Sample_4 : Fragment() {

    private lateinit var rootView: View

    // layout elements
    private lateinit var imageView: PreviewView
    private lateinit var textView: TextView

    // needed for image
    private val REQUEST_CODE_PERMISSIONS: Int = 987
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    // needed for camera
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var cameraSelector: CameraSelector
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_sample_4, container, false)

        // initialize layout elements
        imageView = rootView.findViewById(R.id.BS_preview)
        textView = rootView.findViewById(R.id.BS_text)

        return rootView
    }

    // Livecycle: Will be executed when the fragment is opened
    override fun onStart() {
        super.onStart()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this.requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onPause() {
        super.onPause()
        cameraProvider.unbindAll();
    }

// ================================================================================================
// CAMERA
// ================================================================================================
// Permissions
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
                startCamera()
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

    // base structure of cameraX
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this.requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindPreview()
            // captureImage() -> not implemented in this use-case
            imageAnalysis()

            cameraProvider.bindToLifecycle(
                context as LifecycleOwner, cameraSelector, imageAnalysis, preview
            )
        }, ContextCompat.getMainExecutor(this.requireActivity()))
    }

    // gives us the possibility to see preview of camera activity
    private fun bindPreview() {
        preview = Preview.Builder()
            .build()

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        var previewView: PreviewView = imageView
        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    // gives us the possibility to edit each frame individually
    private fun imageAnalysis() {
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(imageView.width, imageView.height))
            .build()

        imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { imageProxy ->
            // start activity
            barcodeScanner(imageProxy)
        })
    }

// ================================================================================================
// Barcode Scanner
// ================================================================================================
    // basic structure of barcode scanner
    @SuppressLint("UnsafeOptInUsageError")
    private fun barcodeScanner(imageProxy: ImageProxy) {
        // configure barcode scanner
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC)
            .build()

        // prepare image
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image =
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // create instance of barcode scanner
            val scanner = BarcodeScanning.getClient(options)

            // process scan
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    /* processing success */
                    if (barcodes.isNotEmpty()) {
                        for (barcode in barcodes) {
                            val boundingBox: Rect = barcode.boundingBox!!
                            outlineBarcode(boundingBox)
                            identifyBarcode(barcode)
                        }
                    }
                }

            // after done, release the ImageProxy object
            imageProxy.close()
        }
    }

    // handle barcode result
    private fun identifyBarcode(barcode: Barcode) {
        textView.text = "Barcode Scanner"

        writeType(barcode)
        writeMsg(barcode)
    }
    private fun writeType(barcode: Barcode) {
        val type: String = when(barcode.valueType) {
            1 -> "Contact Info"
            2 -> "Email"
            3 -> "ISBN"
            4 -> "Phone"
            5 -> "Product"
            6 -> "SMS"
            7 -> "Text"
            8 -> "URL"
            9 -> "Wifi"
            10 -> "Geo"
            11 -> "Calender Event"
            12 -> "Driver License"
            else -> "undefined"
        }
        textView.append("\nType: $type")
    }
    private fun writeMsg(barcode: Barcode) {
        var text: String = barcode.rawValue!!

        if (barcode.valueType == 10) {
            text = text.substring(4)
            openMaps(text)
        }

        textView.append("\nMessage: $text")
        entityExtraction(barcode.rawValue!!)
    }

    // highlight barcode
    private fun outlineBarcode(boundingBox: Rect) {
        val imageV: ImageView = rootView.findViewById(R.id.BS_overlay)
        val bitmap: Bitmap =
            Bitmap.createBitmap(imageV.width, imageV.height, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bitmap)

        // paint settings
        val paint = Paint()
        paint.alpha = 0xA0                      // the transparency
        paint.color = Color.RED                 // color is red
        paint.style = Paint.Style.STROKE        // stroke or fill or ...
        paint.strokeWidth = 10F                 // the stroke width

        // draw Rect
        canvas.drawRect(boundingBox, paint)

        // set bitmap as background to ImageView
        imageV.background = BitmapDrawable(resources, bitmap)
    }

// ================================================================================================
// Entity Extraction
// ================================================================================================
    // basic structure of entity extraction
    private fun entityExtraction(text: String) {
        // create instance of extractor
        val entityExtractor =
            EntityExtraction.getClient(
                EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH)
            .build())

        entityExtractor
            .downloadModelIfNeeded()
            .addOnSuccessListener { _ ->
                /* download success */
                processEntity(entityExtractor, text)
            }
            .addOnFailureListener { e ->
                /* download failed */
                textView.append("\n Download translation model failed (Entity Extraction)")
                println(e)
            }
    }

    // process entity extraction
    private fun processEntity(entityExtractor: EntityExtractor, text: String) {
        // create params object
        val params = getEntityParams(text)

        // extract information of params
        entityExtractor
            .annotate(params)
            .addOnSuccessListener { entityAnnotations ->
                /* Annotation successful */
                if (entityAnnotations.isNotEmpty()) {
                    // one or more entities detected
                    handleEntity(entityAnnotations)
                }
            }
            .addOnFailureListener { e ->
                /* Annotation failed */
                textView.append("\n Annotation failed (Entity Extraction)")
                println(e)
            }
    }

    // config extractor
    private fun getEntityParams(input: String): EntityExtractionParams {
        return EntityExtractionParams.Builder(input)
            .setEntityTypesFilter(setOf(1, 3, 8, 10))
            .build()

        /*
         * specify params:
         *  .setEntityTypesFilter(optional entity type filter)
         *      A set must be passed eg: var typesSet: Set<Int> = setOf(1) // 1 = address
         *      List of Entity-Types:
         *          https://developers.google.com/android/reference/com/google/mlkit/nl/entityextraction/Entity#TYPE_ADDRESS
         *  .setPreferredLocale(optional preferred locale)
         *  .setReferenceTime(optional reference date-time)
         *  .setReferenceTimeZone(optional reference timezone)
         *
         */
    }

    // handle found entity
    private fun handleEntity(entityAnnotations: List<EntityAnnotation>) {
        for (entityAnnotation in entityAnnotations) {
            val entities = entityAnnotation.entities
            val annotatedText = entityAnnotation.annotatedText

            for (entity in entities) {
                when (entity.type) {
                    Entity.TYPE_URL -> openURL(annotatedText)
                    Entity.TYPE_EMAIL -> openEmail(annotatedText)
                    Entity.TYPE_ADDRESS -> openMaps(annotatedText)
                    Entity.TYPE_PHONE -> openPhone(annotatedText)
                    else -> ""
                }
            }
        }
    }
    private fun openURL(annotatedText: String) {
        var url = annotatedText
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            url = "http://$url"

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }
    private fun openEmail(annotatedText: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "plain/text"
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(annotatedText))
        startActivity(Intent.createChooser(intent, ""))
    }
    private fun openMaps(annotatedText: String) {
        val uri = "http://maps.google.co.in/maps?q=$annotatedText"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(intent)
    }
    private fun openPhone(annotatedText: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$annotatedText")
        startActivity(intent)
    }
}