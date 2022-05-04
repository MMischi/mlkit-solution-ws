/**
 * Text Recognition - Language ID - Translate Text
 * detect Text, identify language of detected text, translate text
 * e.g. travel translator
 *
 * here we use cameraX, see:
 *      - AndroidManifest.xml
 *      - build.grandle:app (for this use-case we need internet)
 */
package com.example.mlkit_ws

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class Sample_3 : Fragment(), View.OnClickListener {

    private lateinit var rootView: View

    // layout elements
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    private lateinit var btn: Button

    // needed for image
    private val REQUEST_CODE_PERMISSIONS: Int = 123
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val REQUEST_IMAGE_CAPTURE: Int = 234

    // use-case variables
    private lateinit var image: Bitmap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_sample_3, container, false)

        // initialize layout elements
        imageView = rootView.findViewById(R.id.TR_imageView)
        textView = rootView.findViewById(R.id.TR_text)
        btn = rootView.findViewById(R.id.TR_imgBtn)

        btn.setOnClickListener(this)

        return rootView
    }

    override fun onClick(p0: View?) {
        if (allPermissionsGranted()) {
            startActivity()
        } else {
            ActivityCompat.requestPermissions(
                this.requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
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
            textDetection(image)
        }
    }

// ================================================================================================
// Text Recognition
// ================================================================================================
    // base structure of text recognition
    private fun textDetection(image: Bitmap) {
        // create instance of text recognition
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // prepare image (Bitmap to InputImage)
        val image: InputImage = prepImg()

        // process text recognizer
        recognizer.process(image)
            .addOnSuccessListener { result ->
                /* processing success */
                handleResult(result)
            }
            .addOnFailureListener { e ->
                /* processing failed */
                textView.append("\n\n Something failed at processing")
                println(e)
            }
    }

    // convert Bitmap to InputImage
    private fun prepImg(): InputImage {
        return InputImage.fromBitmap(image, 0)
    }

    // get text and identify language
    private fun handleResult(result: Text) {
        // full text
        // val firstBlock = result.text

        // get text (first) block
        // val firstBlock = result.textBlocks[0]

        // get text (first) line
        // val firstLine = result.textBlocks[0].lines[0]

        // get text (first) element/word
        // val firstElem = result.textBlocks[0].lines[0].elements[0]

        textView.text = ""

        val fullText = result.text
        textView.append("Original:\n $fullText")

        identifyLanguage(fullText)
    }

// ================================================================================================
// Language Identification
// ================================================================================================
    // get language of text and translate it / basic structure of language identification
    private fun identifyLanguage(text: String) {
        var langCode: String = "und"

        // create instance of language identifier
        val languageIdentifier = LanguageIdentification.getClient()

        // process language identifier
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                /* processing success */
                if (languageCode == "und") {
                    textView.append("\n (Language not found!)")
                    textView.append("\n\n Übersetzung nicht möglich")
                } else {
                    langCode = languageCode
                    textView.append("\n ($languageCode)")
                    textView.append("\n\n English translation:")
                    translateText(text, langCode)
                }
            }
            .addOnFailureListener { e ->
                /* processing failed */
                textView.append("\n\n Something failed at processing")
                println(e)
            }
    }

// ================================================================================================
// Translate Text
// ================================================================================================
    // translate text / basic structure of translate text
    private fun translateText(text: String, langCode: String) {
        // configure object detector
        val options: TranslatorOptions = confOptions(langCode)

        // create instance of object detector
        val englishTranslator = Translation.getClient(options)

        // make sure the required translation model has been downloaded to the device
        var conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        englishTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                /* download success */
                // process translator
                englishTranslator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        /* processing success */
                        textView.append("\n $translatedText")
                    }
                    .addOnFailureListener { e ->
                        /* processing failed */
                        textView.append("\n Something failed at processing")
                        println(e)
                    }
            }
            .addOnFailureListener { e ->
                /* download failed */
                textView.append("\n Download translation model failed")
                println(e)
            }
    }

    private fun confOptions(langCode: String): TranslatorOptions {
        return TranslatorOptions.Builder()
            .setSourceLanguage(getSourceLanguage(langCode))
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
    }

    // helper function: get
    private fun getSourceLanguage(lang: String): String {
        return when(lang) {
            "fr" -> TranslateLanguage.FRENCH
            "it" -> TranslateLanguage.ITALIAN
            "de" -> TranslateLanguage.GERMAN
            else -> "undefined"     // cannot occur, this case has already been prevented. when,
                                    //  however, asks for a default case
        }
        // list of supported lang:
        //  https://developers.google.com/ml-kit/language/translation/translation-language-support
    }
}