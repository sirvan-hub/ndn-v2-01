package com.example.data.model

enum class ParcelSize(val labelFa: String, val multiplier: Double, val defaultBaseFee: Long) {
    SMALL("کوچک (پاکت تا ۱ کیلو)", 1.0, 18000L),
    MEDIUM("متوسط (کارتن تا ۳ کیلو)", 1.4, 25000L),
    LARGE("بزرگ (کارتن تا ۱۰ کیلو)", 1.8, 35000L),
    HEAVY("سنگین (بیش از ۱۰ کیلو)", 2.5, 50000L)
}
