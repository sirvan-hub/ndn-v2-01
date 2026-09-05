package com.example.camera

/**
 * Operational context for barcode scanning.
 * Decouples scanning intent (e.g. initial registration vs. hub receipt)
 * while preserving the single physical CameraX/ML Kit pipeline.
 */
enum class ScanContext {
    PARCEL_REGISTRATION,
    COURIER_HANDOVER,
    HUB_RECEIPT,
    CUSTOMER_COLLECTION
}
