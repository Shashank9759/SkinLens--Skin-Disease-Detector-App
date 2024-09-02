package com.studies.skinlens.Domain.Repository

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import android.os.Trace
import android.util.Log



import com.studies.skinlens.Common.Device
import com.studies.skinlens.Data.Repository.ClassifierFloatMobileNet
import com.studies.skinlens.Domain.Models.Recognition
import org.tensorflow.lite.Interpreter

import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.MappedByteBuffer
import java.util.PriorityQueue
import kotlin.math.min


/** A classifier specialized to label images using TensorFlow Lite.  */
abstract class Classifier protected constructor(
    activity: Activity?,
    device: Device?,
    numThreads: Int
) {
    /** The runtime device type used for executing classification.  */


    /** The loaded TensorFlow Lite model.  */
    private var tfliteModel: MappedByteBuffer?
    /** Get the image size along the x axis.  */
    /** Image size along the x axis.  */
    val imageSizeX: Int
    /** Get the image size along the y axis.  */
    /** Image size along the y axis.  */
    val imageSizeY: Int

    /** Optional GPU delegate for acceleration.  */ // TODO: Declare a GPU delegate
    private var gpuDelegate: GpuDelegate? = null

    /** An instance of the driver class to run model inference with Tensorflow Lite.  */ // TODO: Declare a TFLite interpreter
    protected var tflite: Interpreter?

    /** Options for configuring the Interpreter.  */
    private val tfliteOptions = Interpreter.Options()

    /** Labels corresponding to the output of the vision model.  */
    private val labels: List<String>

    /** Input image TensorBuffer.  */
    private var inputImageBuffer: TensorImage

    /** Output probability TensorBuffer.  */
    private val outputProbabilityBuffer: TensorBuffer

    /** Processer to apply post processing of the output probability.  */
    private val probabilityProcessor: TensorProcessor



    /** Initializes a `Classifier`.  */
    init {
        tfliteModel = FileUtil.loadMappedFile(
            activity!!,
            modelPath!!
        )
        when (device) {
            Device.GPU -> {
                // TODO: Create a GPU delegate instance and add it to the interpreter options
                gpuDelegate = GpuDelegate()
                tfliteOptions.addDelegate(gpuDelegate)
            }

            Device.CPU -> {}
            else->{}
        }
        tfliteOptions.setNumThreads(numThreads)
        // TODO: Create a TFLite interpreter instance
        tflite = Interpreter(tfliteModel!!, tfliteOptions)

        // Loads labels out from the label file.
        labels = FileUtil.loadLabels(
            activity,
            labelPath!!
        )

        // Reads type and shape of input and output tensors, respectively.
        val imageTensorIndex = 0
        val imageShape = tflite!!.getInputTensor(imageTensorIndex).shape() // {1, height, width, 3}
        imageSizeY = imageShape[1]
        imageSizeX = imageShape[2]
        val imageDataType = tflite!!.getInputTensor(imageTensorIndex).dataType()
        val probabilityTensorIndex = 0
        val probabilityShape =
            tflite!!.getOutputTensor(probabilityTensorIndex).shape() // {1, NUM_CLASSES}
        val probabilityDataType = tflite!!.getOutputTensor(probabilityTensorIndex).dataType()

        // Creates the input tensor.
        inputImageBuffer = TensorImage(imageDataType)

        // Creates the output tensor and its processor.
        outputProbabilityBuffer =
            TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)

        // Creates the post processor for the output probability.
        probabilityProcessor = TensorProcessor.Builder().add(
            postprocessNormalizeOp
        ).build()
      //  LOGGER.d("Created a Tensorflow Lite Image Classifier.")
    }

    /** Runs inference and returns the classification results.  */
    fun recognizeImage(bitmap: Bitmap, sensorOrientation: Int): List<Recognition> {
        // Logs this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage")
        Trace.beginSection("loadImage")
        val startTimeForLoadImage = SystemClock.uptimeMillis()
        inputImageBuffer = loadImage(bitmap, sensorOrientation)
        val endTimeForLoadImage = SystemClock.uptimeMillis()
        Trace.endSection()
      //  LOGGER.v("Timecost to load the image: " + (endTimeForLoadImage - startTimeForLoadImage))

        // Runs the inference call.
        Trace.beginSection("runInference")
        val startTimeForReference = SystemClock.uptimeMillis()
        // TODO: Run TFLite inference
        tflite!!.run(inputImageBuffer.buffer, outputProbabilityBuffer.buffer.rewind())
        val endTimeForReference = SystemClock.uptimeMillis()
        Trace.endSection()
     //   LOGGER.v("Timecost to run model inference: " + (endTimeForReference - startTimeForReference))

        // Gets the map of label and probability.
        // TODO: Use TensorLabel from TFLite Support Library to associate the probabilities
        //       with category labels
        val labeledProbability =
            TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
                .mapWithFloatValue
        Trace.endSection()

        // Gets top-k results.
        return getTopKProbability(labeledProbability)
    }




    /** Closes the interpreter and model to release resources.  */
    fun close() {
        if (tflite != null) {
            // TODO: Close the interpreter
            tflite!!.close()
            tflite = null
        }
        // TODO: Close the GPU delegate
        if (gpuDelegate != null) {
            gpuDelegate!!.close()
            gpuDelegate = null
        }
        tfliteModel = null
    }

    /** Loads input image, and applies preprocessing.  */
    private fun loadImage(bitmap: Bitmap, sensorOrientation: Int): TensorImage {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap)

        // Creates processor for the TensorImage.
        val cropSize =
            min(bitmap.getWidth().toDouble(), bitmap.getHeight().toDouble()).toInt()
        val numRoration = sensorOrientation / 90
        // TODO(b/143564309): Fuse ops inside ImageProcessor.
        // TODO: Define an ImageProcessor from TFLite Support Library to do preprocessing
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(Rot90Op(numRoration))
            .add(preprocessNormalizeOp)
            .build()
        return imageProcessor.process(inputImageBuffer)
    }

    protected abstract val modelPath: String?
    protected abstract val labelPath: String?
    protected abstract val preprocessNormalizeOp: TensorOperator?
    protected abstract val postprocessNormalizeOp: TensorOperator?

    companion object {
      //  private val LOGGER: Logger = Logger()

        /** Number of results to show in the UI.  */
        private const val MAX_RESULTS = 3

        /**
         * Creates a classifier with the provided configuration.
         *
         * @param activity The current Activity.
         * @param device The device to use for classification.
         * @param numThreads The number of threads to use for classification.
         * @return A classifier with the desired configuration.
         */
        @Throws(IOException::class)
        fun create(activity: Activity?, device: Device?, numThreads: Int): Classifier {
            return ClassifierFloatMobileNet(activity, device, numThreads)
        }

        /** Gets the top-k results.  */
        private fun getTopKProbability(labelProb: Map<String, Float>): List<Recognition> {
            // Find the best classifications.
            val pq = PriorityQueue(
                MAX_RESULTS,
                object : Comparator<Recognition?> {
                    override fun compare(lhs: Recognition?, rhs: Recognition?): Int {
                        // Intentionally reversed to put high confidence at the head of the queue.
                        return java.lang.Float.compare(rhs?.confidence!!, lhs?.confidence!!)
                    }


                })
            for ((key, value) in labelProb) {
                pq.add(Recognition("" + key, key, value, null))
            }
            val recognitions = ArrayList<Recognition>()
            val recognitionsSize =
                min(pq.size.toDouble(), MAX_RESULTS.toDouble()).toInt()
            for (i in 0 until recognitionsSize) {
                recognitions.add(pq.poll()!!)
            }
            return recognitions
        }
    }
}

