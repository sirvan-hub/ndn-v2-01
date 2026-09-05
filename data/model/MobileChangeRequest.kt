package com.example.data.model

data class MobileChangeRequest(
    val id: String,
    val userId: String,
    val userFullName: String,
    val currentPhone: String,
    val requestedPhone: String,
    val nationalId: String,
    val status: MobileChangeStatus = MobileChangeStatus.PENDING_ADMIN_APPROVAL,
    val requestedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null,
    val reviewNotes: String? = null
)

enum class MobileChangeStatus(val titleFa: String) {
    PENDING_ADMIN_APPROVAL("در انتظار تایید مدیر"),
    APPROVED("تایید شده"),
    REJECTED("رد شده")
}
