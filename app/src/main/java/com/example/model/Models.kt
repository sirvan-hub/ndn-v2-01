package com.example.model

enum class UserRole(val titleFa: String, val subtitleFa: String) {
    CUSTOMER("مشتری محله", "پیگیری و دریافت بسته"),
    COURIER("سفیر توزیع", "مامور حمل و تحویل به هاب"),
    HUB_MANAGER("مدیر هاب محله", "فروشگاه یا مرکز تحویل محلی"),
    ADMIN("مدیر سیستم", "پرتال نظارت، امنیت و مدیریت کل")
}

enum class AppThemeMode(val titleFa: String) {
    LUXURY("مشکی و طلایی لوکس (Obsidian & Gold)"),
    CLASSIC("آبی و نارنجی سازمانی")
}

enum class NavigationTab(val titleFa: String) {
    DASHBOARD("داشبورد"),
    PACKAGES("بسته‌ها"),
    SCAN("اسکن سریع"),
    MAP("نقشه هاب‌ها"),
    SETTINGS("تنظیمات"),
    AUTH("حساب کاربری")
}

enum class AuthFlowStep {
    PANEL_SELECT,
    AUTH_ACTION_SELECT,
    REGISTER_FORM,
    REGISTRATION_PENDING_APPROVAL,
    LOGIN_FORM,
    TWO_FACTOR_FORM,
    PASSWORD_RECOVERY,
    ROLE_SELECTOR_POST_BIOMETRIC,
    LOCKED_OUT,
    AUTHENTICATED
}

enum class AccountApprovalStatus(val titleFa: String) {
    APPROVED("تایید شده و فعال"),
    PENDING("در انتظار تایید مدیر سیستم"),
    REJECTED("رد شده / غیرفعال")
}

enum class AdminRoleLevel(val titleFa: String) {
    SUPER_ADMIN("سوپر ادمین کل سامانه"),
    AUDITOR("ممیز و بازرس ارشد مالی"),
    SUPPORT("پشتیبانی فنی و عملیات"),
    SYSTEM_MANAGER("مدیر کل فنی و سیستم"),
    OPS_MANAGER("مدیر عملیات هاب‌ها و توزیع"),
    SECURITY_OFFICER("مسئول امنیت، ۲FA و بازرسی")
}

enum class AdminPermission(val titleFa: String, val descFa: String) {
    MANAGE_USERS("مدیریت کاربران و مشتریان", "مشاهده و مدیریت حساب مشتریان"),
    APPROVE_HUBS("تایید و صدور مجوز هاب‌ها", "بررسی پروانه کسب و فعال‌سازی هاب"),
    MANAGE_COURIERS("نظارت بر سفیران توزیع", "تعیین محدوده و تایید ماموران پخش"),
    VIEW_FINANCES("دسترسی به تراکنش‌های مالی", "مشاهده کارمزدها و تسویه‌حساب"),
    SYSTEM_SETTINGS("تنظیمات کلی و امنیتی", "پیکربندی پارامترها و امنیت سیستم"),
    VIEW_AUDIT_LOGS("مشاهده لاگ‌های ممیزی", "بررسی سوابق ورود و هشدارهای امنیتی"),
    RESET_PASSWORDS("بازیابی و ریست رمز عبور", "صدور مجوز تغییر کلمه عبور کاربران"),
    CLOUD_SYNC_ADMIN("مدیریت همگام‌سازی ابری گوگل", "کنترل پایگاه داده ابری و پشتیبان‌گیری در درایو")
}

enum class RegistrationInitiator(val titleFa: String) {
    COURIER_INITIATED("ثبت توسط سفیر (نیاز به تایید مشتری)"),
    CUSTOMER_INITIATED("ثبت دستی توسط مشتری (نیاز به احراز سفیر)")
}

data class CourierCandidate(
    val id: String,
    val fullName: String,
    val code: String,
    val phone: String,
    val rating: Float = 4.9f,
    val vehicleType: String = "موتورسیکلت",
    val activeZone: String = "سعادت‌آباد و شهرک غرب"
)

data class InternalMessage(
    val id: String,
    val packageId: String,
    val senderRole: UserRole,
    val senderName: String,
    val content: String,
    val timestamp: String = "هم‌اکنون",
    val isSystemNotice: Boolean = false
)

data class UserProfile(
    val id: String,
    val role: UserRole,
    val fullName: String,
    val username: String = "",
    val phone: String,
    val nationalId: String = "",
    val email: String = "",
    val password: String = "",
    val postalCode: String = "",
    val address: String = "",
    val bankCardNumber: String = "",
    val storeName: String = "",
    val guildType: String = "",
    val exactAddress: String = "",
    val workingHoursWeekday: String = "۰۸:۰۰ الی ۲۲:۰۰",
    val landlinePhone: String = "",
    val servicesDescription: String = "",
    val postalDistrict: String = "",
    val workCoverageArea: String = "",
    val coverageRadiusKm: Int = 5,
    val hasBiometricsEnabled: Boolean = true,
    val nationalIdCardPhoto: String? = null,
    val approvalStatus: AccountApprovalStatus = AccountApprovalStatus.APPROVED,
    val registrationDate: String = "۱۴۰۳/۰۶/۰۱"
)

data class AdminUser(
    val id: String,
    val fullName: String,
    val username: String = "",
    val email: String,
    val phone: String,
    val roleLevel: AdminRoleLevel,
    val roleTitle: String,
    val permissions: List<AdminPermission>,
    val isActive: Boolean = true,
    val lastLogin: String = "همین حالا",
    val createdAt: String = "۱۴۰۳/۰۵/۱۰"
)

data class SystemAdminContact(
    val primaryPhone: String = "09123407615",
    val emergencyPhone: String = "09123407615",
    val primaryEmail: String = "reza.gh@ndn-pudo.ir",
    val supportEmail: String = "reza.gh@ndn-pudo.ir",
    val recoveryDispatchMethod: String = "both", // "both", "sms", "email"
    val autoApproveRecovery: Boolean = true
)

enum class PackageStatus(val textFa: String) {
    PENDING_CUSTOMER_APPROVAL("در انتظار تایید دریافت توسط مشتری"),
    PENDING_COURIER_VERIFICATION("در انتظار احراز و اسکن فیزیکی توسط سفیر"),
    IN_TRANSIT("در مسیر انتقال به هاب"),
    AT_HUB("در هاب محلی (آماده تحویل)"),
    DELIVERED("تحویل داده شد به مشتری"),
    REJECTED("رد شده")
}

enum class PackageSize(val labelFa: String, val baseFee: Long = 0L) {
    SMALL("کوچک (پاکت تا ۱ کیلو)", 0L),
    MEDIUM("متوسط (کارتن تا ۳ کیلو)", 0L),
    LARGE("بزرگ (کارتن تا ۱۰ کیلو)", 0L)
}

data class PackageHistoryEntry(
    val status: String,
    val timestamp: String,
    val description: String
)

data class StakeholderSplit(
    val courierFee: Long,
    val hubFee: Long,
    val systemFee: Long,
    val baseFee: Long,
    val totalFee: Long
)

data class PackageItem(
    val id: String,
    val trackingCode: String,
    val title: String,
    val sender: String,
    val receiver: String,
    val receiverPhone: String = "09123456789",
    val hubId: String,
    val hubName: String,
    val hubAddress: String,
    val status: PackageStatus,
    val statusText: String,
    val dimensions: String,
    val weight: String,
    val size: PackageSize = PackageSize.MEDIUM,
    val lastUpdated: String = "همین حالا",
    val baseFee: Long = 25000L,
    val storageFee: Long = 0L,
    val totalFee: Long = 25000L,
    val isPaid: Boolean = false,
    val photoUrl: String? = null,
    val courierId: String = "courier-123",
    val courierName: String = "محمد جوادی (کد سفیر: ۱۲۳)",
    val courierPhone: String = "09359998877",
    val registrationInitiator: RegistrationInitiator = RegistrationInitiator.COURIER_INITIATED,
    val slaHoursRemaining: Int = 46, // Standard 48h SLA at hub
    val maxSlaHours: Int = 48,
    val history: List<PackageHistoryEntry> = emptyList(),
    val messages: List<InternalMessage> = emptyList()
) {
    fun calculateSplit(): StakeholderSplit {
        val courierShare = (baseFee * 0.30).toLong()
        val hubShare = (baseFee * 0.30).toLong()
        val systemShare = baseFee - courierShare - hubShare
        return StakeholderSplit(
            courierFee = courierShare,
            hubFee = hubShare,
            systemFee = systemShare,
            baseFee = baseFee,
            totalFee = baseFee
        )
    }
}

data class HubItem(
    val id: String,
    val name: String,
    val type: String, // supermarket, stationery, netcafe, pharmacy
    val typeName: String,
    val managerName: String,
    val phone: String,
    val licenseNumber: String,
    val address: String,
    val rating: Float = 4.8f,
    val reviewCount: Int = 120,
    val workingHours: String = "۰۸:۰۰ - ۲۲:۰۰",
    val isOpen: Boolean = true,
    val currentPackagesCount: Int = 12,
    val maxCapacity: Int = 50,
    val lat: Double = 35.7924,
    val lng: Double = 51.3789
)

data class ActivityLog(
    val id: String,
    val text: String,
    val timestamp: String = "هم‌اکنون",
    val source: String, // courier, hub, system, customer, admin
    val trackingCode: String? = null
)

enum class CloudSyncStatus {
    SYNCED,
    SYNCING,
    OFFLINE,
    ERROR
}

data class GoogleDriveSyncState(
    val status: CloudSyncStatus = CloudSyncStatus.SYNCED,
    val isAutoSyncEnabled: Boolean = true,
    val lastSyncTimestamp: String = "امروز ساعت ۰۸:۵۰",
    val syncedDocumentsCount: Int = 4,
    val driveFileName: String = "ndn_database_v101.json",
    val syncLog: String = "همگام‌سازی مستقیم ابری فعال (بدون نیاز به سرور مرکزی - ذخیره در Google Drive شخصی)",
    val totalSyncOperations: Int = 14
)

data class RegionalSaturation(
    val regionName: String,
    val code: String,
    val hubCount: Int,
    val courierCount: Int,
    val activePackages: Int,
    val saturationPercent: Int, // e.g. 75%
    val bottleneckStatus: String // "روان", "هشدار ظرفیت", "اشباع"
)

enum class NavigationApp(val titleFa: String, val iconName: String) {
    GOOGLE_MAPS("گوگل مپ (Google Maps)", "google"),
    WAZE("ویز (Waze)", "waze"),
    NESHAN("نشان (Neshan)", "neshan"),
    BALAD("بلد (Balad)", "balad")
}
