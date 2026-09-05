package com.example.data.model

enum class AccountApprovalStatus(val titleFa: String) {
    PENDING("در انتظار بررسی و تایید مدیر ارشد"),
    APPROVED("تایید شده و فعال در سامانه"),
    REJECTED("رد شده / غیرمجاز برای دسترسی")
}
