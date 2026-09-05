package com.example.camera

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX ImageAnalysis.Analyzer integrating Google ML Kit Barcode Scanning.
 * Optimized for logistics & postal barcodes: QR_CODE, CODE_128, EAN_13, EAN_8, UPC_A, UPC_E, DATA_MATRIX.
 */
class BarcodeAnalyzer(
    private val duplicateGuard: BarcodeDuplicateGuard = BarcodeDuplicateGuard(),
    private val onBarcodeDetected: (barcodeValue: String, format: String) -> Unit,
    private val onError: (Exception) -> Unit = {}
) : ImageAnalysis.Analyzer {

    private val scannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_DATA_MATRIX
        )
        .build()

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(scannerOptions)

    @OptIn(ExperimentalGetImage::class)
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue?.trim() ?: continue
                    if (rawValue.isBlank()) continue

                    val formatName = getFormatName(barcode.format)

                    if (duplicateGuard.shouldProcess(rawValue)) {
                        duplicateGuard.markProcessed(rawValue)
                        onBarcodeDetected(rawValue, formatName)
                    }
                }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
            .addOnCompleteListener {
                // ImageProxy MUST be closed to prevent CameraX frame pipeline stalling
                try {
                    imageProxy.close()
                } catch (_: Exception) {
                }
            }
    }

    private fun getFormatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_UPC_A -> "UPC_A"
            Barcode.FORMAT_UPC_E -> "UPC_E"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            else -> "BARCODE_$format"
        }
    }

    /**
     * Release scanner resources when analysis pipeline is destroyed.
     */
    fun close() {
        try {
            scanner.close()
        } catch (_: Exception) {
        }
    }
}
