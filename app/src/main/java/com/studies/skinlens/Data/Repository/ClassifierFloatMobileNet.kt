package com.studies.skinlens.Data.Repository

import android.app.Activity
import com.studies.skinlens.Common.Device
import com.studies.skinlens.Domain.Repository.Classifier

import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.ops.NormalizeOp


/** This TensorFlowLite classifier works with the float MobileNet model.  */
class ClassifierFloatMobileNet
/**
 * Initializes a `ClassifierFloatMobileNet`.
 *
 * @param activity
 */
    (activity: Activity?, device: Device?, numThreads: Int) :
    Classifier(activity, device, numThreads) {
    protected override val modelPath: String
        // TODO: Specify model.tflite as the model file and labels.txt as the label file
        protected get() = "model.tflite"


    protected override val labelPath: String
        protected get() = "labels.txt"
    protected override val preprocessNormalizeOp: TensorOperator
        protected get() = NormalizeOp(IMAGE_MEAN, IMAGE_STD)
    protected override val postprocessNormalizeOp: TensorOperator
        protected get() = NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)

    companion object {
        /** Float MobileNet requires additional normalization of the used input.  */
        private const val IMAGE_MEAN = 0f
        private const val IMAGE_STD = 255f

        /**
         * Float model does not need dequantization in the post-processing. Setting mean and std as 0.0f
         * and 1.0f, repectively, to bypass the normalization.
         */
        private const val PROBABILITY_MEAN = 0.0f
        private const val PROBABILITY_STD = 1.0f
    }
}

