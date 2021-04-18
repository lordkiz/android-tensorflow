package com.example.androidtensorflow

import android.R.attr.bitmap
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import com.bumptech.glide.Glide
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var button: Button
    private lateinit var textView: TextView

    private lateinit var classifier: ImageClassifier

    internal lateinit var backgroundThread: HandlerThread
    internal lateinit var backgroundHandler: Handler

    internal val lock = Any()
    internal var runClassifier = false

    val runPerically = object: Runnable {
        override fun run() {
            synchronized(lock) {
                if (runClassifier) {
                    classifyImage()
                }
            }
            backgroundHandler.post(this)
        }
    }

    companion object {
        internal const val HANDLER_THREAD_NAME = "HANDLER_THREAD";

        private const val IMAGE_PICK_CODE = 112
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById<ImageView>(R.id.selectedImageView)
        button = findViewById<Button>(R.id.chooseImageButton)
        textView = findViewById(R.id.predictionText)

        button.setOnClickListener(View.OnClickListener {
            var intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Pick a Picture"), IMAGE_PICK_CODE)
        })

        try {
            classifier = ImageClassifierQuantizedMobileNet(this)
        } catch (e: IOException) {
            Log.e("Classifier", "unable to init classifier")
            e.printStackTrace()
        }

        startBackgroundThread()
    }

    override fun onDestroy() {
        stopBackgroundThread()
        classifier.close()
        super.onDestroy()

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            IMAGE_PICK_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    var selectedImageUri: Uri? = data?.getData();
                    if (selectedImageUri != null) {
                        val inputStream = contentResolver.openInputStream(selectedImageUri);
                        val selectedImage = BitmapFactory.decodeStream(inputStream);
                        imageView.setImageBitmap(selectedImage);
                        backgroundHandler.post(runPerically)
                    }

                }
            }
        }
    }


    private fun classifyImage() {
        val originalBitmap: Bitmap = (imageView.drawable as BitmapDrawable).bitmap
        val scaleDrawable = Bitmap.createScaledBitmap(originalBitmap, classifier.imageSizeX, classifier.imageSizeY, false)

        val text = classifier.classifyFrame(scaleDrawable);

        scaleDrawable.recycle()

        runOnUiThread{ textView.setText(text) }

    }
}