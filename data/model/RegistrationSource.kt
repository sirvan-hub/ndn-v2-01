package com.example.data.model

enum class RegistrationSource(val titleFa: String, val isPrimaryWorkflow: Boolean) {
    CUSTOMER_REQUEST("درخواست تحویل به هاب توسط مشتری در طول مسیر", true),
    FAILED_HOME_DELIVERY("عدم حضور گیرنده در محل و توافق PUDO", true),
    END_OF_SHIFT_RECOVERY("ثبت تجمیعی پایان شیفت (استثنا / بازیابی)", false)
}
