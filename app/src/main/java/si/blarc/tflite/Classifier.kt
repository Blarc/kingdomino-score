/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/
package si.blarc.tflite

import android.graphics.Bitmap
import android.graphics.RectF
import si.blarc.enum.TileEnum
import kotlin.math.abs

/**
 * Generic interface for interacting with different recognition engines.
 */
interface Classifier {
    fun recognizeImage(bitmap: Bitmap?): List<Recognition>?
    fun enableStatLogging(debug: Boolean)
    val statString: String?

    fun close()
    fun setNumThreads(numThreads: Int)
    fun setUseNNAPI(isChecked: Boolean)
    val objThresh: Float

    /**
     * An immutable result returned by a Classifier describing what was recognized.
     */
    class Recognition(
            /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        val id: String?,
            /**
         * Display name for the recognition.
         */
        val title: String?,
            /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        val confidence: Float?,
            /**
         * Optional location within the source image for the location of the recognized object.
         */
        private var location: RectF?,

        var detectedClass: TileEnum,

        var neighbours: List<Recognition> = listOf()
    ) {

        fun getLocation(): RectF {
            return RectF(location)
        }

        fun setLocation(location: RectF?) {
            this.location = location
        }

        fun manhattanDistance(other: Recognition, xRatio: Float, yRatio: Float): Float {
            return abs((this.location!!.centerX() - other.location!!.centerX()) * yRatio) + abs((this.location!!.centerY() - other.location!!.centerY()) * xRatio)
        }

        override fun toString(): String {
            var resultString = ""
            if (id != null) {
                resultString += "[$id] "
            }
            if (title != null) {
                resultString += "$title "
            }
            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f)
            }
            if (location != null) {
                resultString += location.toString() + " "
            }
            resultString += "$detectedClass "
            return resultString.trim { it <= ' ' }
        }
    }
}