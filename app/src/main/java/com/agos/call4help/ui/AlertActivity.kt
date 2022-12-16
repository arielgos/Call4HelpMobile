package com.agos.call4help.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.agos.call4help.R
import com.agos.call4help.Utils
import com.agos.call4help.createFile
import com.agos.call4help.databinding.AlertActivityBinding
import com.agos.call4help.model.Alert
import com.agos.call4help.service.GpsService
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.storage.ktx.storage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.io.FileInputStream

class AlertActivity : AppCompatActivity() {

    private lateinit var binding: AlertActivityBinding
    private var firebaseUser: FirebaseUser? = null
    private var alert: Alert = Alert()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AlertActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseUser = Firebase.auth.currentUser

        alert = intent.extras?.get("alert") as Alert

        with(binding) {

            binding.description.setText(alert.description)
            binding.tags.setText(alert.tags)
            binding.objects.setText(alert.objects)

            if (alert.image.isNotEmpty()) {
                Glide.with(applicationContext)
                    .asBitmap()
                    .load("${Utils.imageUrl}${alert.image}?alt=media")
                    .into(binding.image)
            }

            if (alert.id.isNotEmpty()) {
                binding.title.setText(R.string.edit_alert)
            }

            back.setOnClickListener {
                onBackPressed()
            }

            save.setOnClickListener {

                alert.date = Date()
                alert.status = getString(R.string.pending)
                alert.latitude = GpsService.myLocation?.latitude ?: 0.0
                alert.longitude = GpsService.myLocation?.longitude ?: 0.0
                alert.description = description.text.toString()

                val collection = Firebase.firestore
                    .collection("users")
                    .document(firebaseUser?.uid.toString())
                    .collection("alerts")

                if (alert.id.isEmpty()) {
                    val newAlert = collection.document()
                    alert.id = newAlert.id
                    newAlert.set(alert)
                        .addOnSuccessListener {
                            setResult(RESULT_OK, Intent())
                            finish()
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                        }
                } else {
                    collection.document(alert.id)
                        .set(alert)
                        .addOnSuccessListener {
                            setResult(RESULT_OK, Intent())
                            finish()
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                        }
                }


            }

            cancel.setOnClickListener {
                onBackPressed()
            }

            image.setOnClickListener {
                val intent = com.canhub.cropper.CropImage.activity()
                    .setAspectRatio(Utils.imageWith, Utils.imageHeight)
                    .getIntent(applicationContext)
                startActivityForResult(intent, Utils.requestNewImage)
            }
        }

        /**
         * Analytics
         */
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "New Alert")
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED, Intent())
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Utils.requestNewImage -> {
                    val result = com.canhub.cropper.CropImage.getActivityResult(data)

                    Glide.with(this)
                        .asBitmap()
                        .load(result!!.uri)
                        .into(target)

                    Glide.with(this)
                        .asBitmap()
                        .load(result.uri)
                        .into(binding.image)

                    /**
                     * Image Labeling
                     */
                    val image = InputImage.fromFilePath(applicationContext, result.uri)
                    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                    labeler.process(image)
                        .addOnSuccessListener { labels ->
                            val tags = mutableListOf<String>()
                            for (label in labels) {
                                val text = label.text
                                val confidence = label.confidence
                                val index = label.index
                                Log.d(Utils.tag, "Label [$index] $confidence / $text")
                                tags.add(text)
                            }
                            alert.tags = tags.distinct()
                                .joinToString(",") {
                                    it
                                }
                            binding.tags.setText(alert.tags)
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                            Log.e(Utils.tag, e.message, e)
                        }

                    /**
                     * Object recognition
                     */
                    val objectDetectorOptions = ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()
                        .build()
                    val objectDetector = ObjectDetection.getClient(objectDetectorOptions)
                    objectDetector.process(image)
                        .addOnSuccessListener { detectedObjects ->
                            val objects = mutableListOf<String>()
                            for (detectedObject in detectedObjects) {
                                val boundingBox = detectedObject.boundingBox
                                for (label in detectedObject.labels) {
                                    val text = label.text
                                    val confidence = label.confidence
                                    val index = label.index
                                    Log.d(Utils.tag, "Object Detection [$index] $confidence / $text (${boundingBox.top},${boundingBox.left},${boundingBox.bottom},${boundingBox.right})")
                                    objects.add(text)
                                }
                            }
                            alert.objects = objects.distinct()
                                .joinToString(",") {
                                    it
                                }
                            binding.objects.setText(alert.objects)
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                            Log.e(Utils.tag, e.message, e)
                        }
                }
            }
        }
    }

    private val target = object : CustomTarget<Bitmap>() {

        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            val fileName = "${UUID.randomUUID()}.jpg"
            val path = "${externalCacheDir?.absolutePath}/$fileName"
            val file = resource.createFile(path)

            Firebase.storage.reference
                .child(fileName)
                .putStream(FileInputStream(file))
                .addOnSuccessListener {
                    Log.d(Utils.tag, "Image ${it.task.result}")
                    alert.image = fileName
                }.addOnFailureListener {
                    it.printStackTrace()
                }
        }

        override fun onLoadCleared(placeholder: Drawable?) {}
    }
}