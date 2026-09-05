package com.example.data.model

enum class ParcelStatus(val titleFa: String, val isSettlementEligible: Boolean) {
    OUT_FOR_DELIVERY("در حال توزیع توسط سفیر", false),
    DELIVERY_ATTEMPTED("عدم حضور گیرنده / تلاش برای تحویل", false),
    ELIGIBLE_FOR_HUB("مجاز برای تحویل به هاب PUDO", false),
    HUB_SELECTED("هاب مقصد انتخاب شد", false),
    HANDOVER_IN_PROGRESS("در حال انتقال و تحویل به هاب", false),
    AWAITING_HUB_CONFIRMATION("در انتظار تایید دریافت توسط متصدی هاب", false),
    TRANSFERRED_TO_HUB("تحویل فیزیکی به هاب انجام شد", true),
    STORED_AT_HUB("در انبار هاب پذیرش و قفسه‌بندی شد", true),
    DELIVERED_TO_CUSTOMER("تحویل نهایی به گیرنده انجام شد", true),
    RETURNED_TO_SENDER("مرجوع به فرستنده", false),
    REJECTED("رد شده / لغو شده", false)
}
