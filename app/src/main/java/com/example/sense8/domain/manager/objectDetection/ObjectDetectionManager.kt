package com.example.sense8.domain.manager.objectDetection

import android.graphics.Bitmap
import com.example.sense8.domain.model.Detection

/**
 * Interface responsible for managing object detection operations.
 */
interface ObjectDetectionManager {
    fun detectObjectsInCurrentFrame(
        bitmap: Bitmap,
        rotation: Int,
        confidenceThreshold: Float
    ): List<Detection>
}