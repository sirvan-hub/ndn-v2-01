package com.example.data

import com.example.model.*

object InitialData {

    // Admin-defined Couriers - initially empty, defined by Admin
    val initialCouriers: List<CourierCandidate> = emptyList()

    val initialRegionalSaturations = listOf(
        RegionalSaturation("منطقه ۲ (سعادت‌آباد و غرب)", "ZONE-02", hubCount = 0, courierCount = 0, activePackages = 0, saturationPercent = 0, bottleneckStatus = "روان و آماده"),
        RegionalSaturation("منطقه ۳ (ونک و میرداماد)", "ZONE-03", hubCount = 0, courierCount = 0, activePackages = 0, saturationPercent = 0, bottleneckStatus = "روان و آماده"),
        RegionalSaturation("منطقه ۵ (پونک و جنت‌آباد)", "ZONE-05", hubCount = 0, courierCount = 0, activePackages = 0, saturationPercent = 0, bottleneckStatus = "روان و آماده"),
        RegionalSaturation("منطقه ۱ (شمران و ولنجک)", "ZONE-01", hubCount = 0, courierCount = 0, activePackages = 0, saturationPercent = 0, bottleneckStatus = "روان و آماده"),
        RegionalSaturation("منطقه ۴ (پاسداران و تهرانپارس)", "ZONE-04", hubCount = 0, courierCount = 0, activePackages = 0, saturationPercent = 0, bottleneckStatus = "روان و آماده")
    )

    // No demo/test users and no auto-login accounts ship with the app.
    // The ONLY account created automatically is the real, database-backed System Admin
    // (username: Reza), seeded once by AuthRepository.seedSystemAdminIfMissing() on first run.
    // See NdnViewModel.init { ... }.
    val initialUsers: List<UserProfile> = emptyList()

    // Fine-grained RBAC permission-tier catalog (separate from login accounts). Starts empty;
    // the System Admin defines additional admin-tier roles from the Admin Portal if needed.
    val initialAdmins: List<AdminUser> = emptyList()

    // Admin-defined Hubs - initially empty, defined by Admin
    val initialHubs: List<HubItem> = emptyList()

    // Clean operational packages
    val initialPackages: List<PackageItem> = emptyList()

    val initialLogs = listOf(
        ActivityLog(
            id = "log-init",
            text = "سامانه پستی محله (NDN-V2.01.0) با مدیریت Reza Gh با موفقیت راه‌اندازی شد.",
            timestamp = "هم‌اکنون",
            source = "system",
            trackingCode = null
        )
    )
}
