package com.studies.skinlens.Presentation.Screens.CameraScreen

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy



import com.studies.skinlens.Domain.Models.Recognition
import com.studies.skinlens.Domain.Repository.Classifier

class DiseaseAnalyzer(
    private val classifier: Classifier,
    private val onResults: (List<Recognition>) -> Unit // Updated to pass RectF and text
): ImageAnalysis.Analyzer {

    private var frameSkipCounter = 0

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
       if (frameSkipCounter % 40 == 0) {
            val rotationDegrees = image.imageInfo.rotationDegrees
            val mediaImage = image.image
            // Convert ImageProxy to Bitmap
            val bitmap = image.toBitmap().centerCrop(224,224)


           // Get results from classifier
           val results = classifier.recognizeImage(bitmap, rotationDegrees)

           // Pass results to the callback
           onResults(results)


    }
        frameSkipCounter++
        image.close()

}}
