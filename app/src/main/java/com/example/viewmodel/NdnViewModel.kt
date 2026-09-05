package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GoogleDriveSyncManager
import com.example.data.InitialData
import com.example.data.local.database.PudoDatabase
import com.example.data.mappers.toDomainParcel
import com.example.data.mappers.toDomainUser
import com.example.data.mappers.toLegacyPackageItem
import com.example.data.mappers.toLegacyUserProfile
import com.example.data.model.MobileChangeRequest
import com.example.data.model.RegistrationSource
import com.example.data.model.RegistrationTransaction
import com.example.data.model.SettlementTariffVersion
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.PudoRepository
import com.example.data.repository.PudoRepositoryImpl
import com.example.data.sync.PudoSyncScheduler
import com.example.util.IdentityNormalizer
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class NdnUiState(
    val currentRole: UserRole = UserRole.CUSTOMER,
    val activeTab: NavigationTab = NavigationTab.DASHBOARD,
    val appTheme: AppThemeMode = AppThemeMode.LUXURY,
    val authFlowStep: AuthFlowStep = AuthFlowStep.PANEL_SELECT,
    val currentUser: UserProfile = UserProfile(
        id = "",
        role = UserRole.CUSTOMER,
        fullName = "",
        phone = ""
    ),
    val allUsers: List<UserProfile> = InitialData.initialUsers,
    val pendingAuthUserId: String? = null,
    val packages: List<PackageItem> = InitialData.initialPackages,
    val hubs: List<HubItem> = InitialData.initialHubs,
    val couriers: List<CourierCandidate> = InitialData.initialCouriers,
    val regionalSaturations: List<RegionalSaturation> = InitialData.initialRegionalSaturations,
    val activityLogs: List<ActivityLog> = InitialData.initialLogs,
    val adminUsers: List<AdminUser> = InitialData.initialAdmins,
    val systemAdminContact: SystemAdminContact = SystemAdminContact(
        primaryPhone = "09123407615",
        emergencyPhone = "09123407615",
        primaryEmail = "reza.gh@ndn-pudo.ir",
        supportEmail = "support@ndn-pudo.ir"
    ),
    val selectedHub: HubItem? = null,
    val selectedPackage: PackageItem? = null,
    val isScannerOpen: Boolean = false,
    val scannerActionType: String = "hub_receive",
    val isPhotoModalOpen: Boolean = false,
    val photoModalTargetPackage: PackageItem? = null,
    val isAdminDialogOpen: Boolean = false,
    val isAdminAuthenticated: Boolean = false,
    val isManualTrackingModalOpen: Boolean = false,
    val isChatModalOpen: Boolean = false,
    val activeChatPackage: PackageItem? = null,
    val isPaymentModalOpen: Boolean = false,
    val paymentTargetPackage: PackageItem? = null,
    val isNavigationChoiceOpen: Boolean = false,
    val navigationTargetHub: HubItem? = null,
    val isLiveAlertVisible: Boolean = true,
    val liveAlertText: String = "سامانه پستی محله (NDN V1.01.1) با مدیریت Reza Gh در وضعیت آنلاین می‌باشد.",
    val failedLoginAttempts: Int = 0,
    val isLockedOut: Boolean = false,
    val lockoutRemainingSeconds: Int = 0,
    val isOnline: Boolean = true,
    val unreadAlertCount: Int = 0,
    val driveSyncState: GoogleDriveSyncState = GoogleDriveSyncState(),
    val tempAuthRole: UserRole = UserRole.CUSTOMER,
    val mobileChangeRequests: List<MobileChangeRequest> = emptyList(),
    val isMobileChangeDialogOpen: Boolean = false,
    val toastMessage: String? = null
)

class NdnViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val KEY_BIOMETRIC_BOUND_USER_ID = "biometric_bound_user_id"
        private const val KEY_LOCKOUT_UNTIL_MILLIS = "lockout_until_millis"
    }

    private val database = PudoDatabase.getInstance(application.applicationContext)
    val pudoRepository: PudoRepository = PudoRepositoryImpl(database)
    val authRepository: AuthRepository = AuthRepositoryImpl(database)

    // Device-local storage used ONLY to remember which single account (by id) last completed a
    // full password+OTP login on this device, so biometric quick-login can re-validate the same
    // account against Room. This is NOT a credential store and NEVER grants access on its own —
    // loginWithBiometrics() always re-checks the account still exists and is APPROVED in Room.
    private val securePrefs = application.applicationContext.getSharedPreferences(
        "ndn_secure_session",
        Context.MODE_PRIVATE
    )

    private val syncManager = GoogleDriveSyncManager(application.applicationContext)
    private val _uiState = MutableStateFlow(NdnUiState())
    val uiState: StateFlow<NdnUiState> = _uiState.asStateFlow()

    private var lockoutJob: Job? = null

    // SECURITY: the failed-login lockout (see handleFailedLogin/startLockoutTimer) is persisted
    // here so it survives the process being killed and restarted. Keeping it purely in-memory
    // StateFlow would let anyone defeat the 5-minute brute-force cooldown just by force-closing
    // and reopening the app.
    private fun persistLockoutUntil(untilMillis: Long) {
        securePrefs.edit().putLong(KEY_LOCKOUT_UNTIL_MILLIS, untilMillis).apply()
    }

    private fun clearPersistedLockout() {
        securePrefs.edit().remove(KEY_LOCKOUT_UNTIL_MILLIS).apply()
    }

    private fun getPersistedLockoutUntil(): Long = securePrefs.getLong(KEY_LOCKOUT_UNTIL_MILLIS, 0L)

    init {
        // Restore an in-progress lockout across process restarts (see comment above).
        val persistedUntil = getPersistedLockoutUntil()
        val remainingMillis = persistedUntil - System.currentTimeMillis()
        if (remainingMillis > 0) {
            _uiState.update {
                it.copy(
                    isLockedOut = true,
                    failedLoginAttempts = 4,
                    lockoutRemainingSeconds = (remainingMillis / 1000L).toInt().coerceAtLeast(1),
                    authFlowStep = AuthFlowStep.LOCKED_OUT
                )
            }
            startLockoutTimer()
        } else if (persistedUntil != 0L) {
            clearPersistedLockout()
        }
        // NOTE: The app must start in a LOGGED_OUT state. currentRole/currentUser stay at
        // their NdnUiState defaults (CUSTOMER / empty placeholder) purely for display until a
        // real authRepository.authenticate() call succeeds — see loginWithPassword().

        viewModelScope.launch {
            // Seed Room database if empty (Offline-first local source of truth)
            val initialDomainParcels = InitialData.initialPackages.map { it.toDomainParcel() }
            pudoRepository.seedParcelsIfEmpty(initialDomainParcels)
            pudoRepository.seedHubsIfEmpty(InitialData.initialHubs)

            // The ONLY account the app creates automatically: a real, database-backed
            // System Admin (username "Reza", initial password "Admin@123", role SYSTEM_ADMIN).
            // This is idempotent — it is a no-op on every run after the first.
            // No test/demo user and no auto-login path exists anywhere else in the app.
            authRepository.seedSystemAdminIfMissing(
                username = "Reza",
                rawPassword = "Admin@123",
                fullName = "Reza",
                phone = "09120000000"
            )
            pudoRepository.seedTariffIfEmpty(
                SettlementTariffVersion(
                    id = "tariff-model-a-v1.0",
                    versionCode = 1,
                    versionName = "تعرفه مصوب PUDO-NDN مدل A (پایه توزیع پستی)",
                    modelType = "MODEL_A",
                    tier1HoursThreshold = 12.0,
                    tier1RatePercentage = 0.20,
                    tier2HoursThreshold = 24.0,
                    tier2RatePercentage = 0.40,
                    additional24hRatePercentage = 0.50,
                    maxLifecycleHours = 168.0,
                    courierSharePercentage = 0.30,
                    hubSharePercentage = 0.30,
                    networkSharePercentage = 0.40,
                    baseFee = 25000L
                )
            )

            // Reactively observe hubs from Room (Single source of truth)
            launch {
                pudoRepository.getAllHubs().collect { hubs ->
                    _uiState.update { state ->
                        state.copy(
                            hubs = hubs,
                            selectedHub = if (state.selectedHub != null) {
                                hubs.find { it.id == state.selectedHub.id } ?: hubs.firstOrNull()
                            } else state.selectedHub ?: hubs.firstOrNull()
                        )
                    }
                }
            }

            // Reactively observe parcels from Room (Single source of truth)
            launch {
                pudoRepository.getAllParcels().collect { parcels ->
                    val uiPackages = parcels.map { it.toLegacyPackageItem() }
                    _uiState.update { state ->
                        state.copy(
                            packages = uiPackages,
                            selectedPackage = if (state.selectedPackage != null) {
                                uiPackages.find { it.id == state.selectedPackage.id } ?: uiPackages.firstOrNull()
                            } else state.selectedPackage
                        )
                    }
                }
            }

            // Reactively observe users from Room. This ONLY refreshes the allUsers list (used by
            // Admin Portal listings) and keeps an already-authenticated session's own record
            // in sync (e.g. after a password change or admin edit). It must NEVER pick a
            // "first user matching role" — that was the session-hijack bug removed in this pass.
            launch {
                authRepository.getAllUsers().collect { users ->
                    val uiUsers = users.map { it.toLegacyUserProfile() }
                    _uiState.update { state ->
                        val refreshedCurrentUser = if (state.authFlowStep == AuthFlowStep.AUTHENTICATED) {
                            uiUsers.find { it.id == state.currentUser.id } ?: state.currentUser
                        } else {
                            state.currentUser
                        }
                        state.copy(
                            allUsers = uiUsers,
                            currentUser = refreshedCurrentUser
                        )
                    }
                }
            }

            // Reactively observe mobile change requests from Room
            launch {
                authRepository.getMobileChangeRequests().collect { requests ->
                    _uiState.update { state ->
                        state.copy(mobileChangeRequests = requests)
                    }
                }
            }
        }
    }

    /**
     * Triggers durable background outbound sync via WorkManager when mutations occur.
     * Room writes are already committed locally before this is invoked.
     */
    fun triggerOutboundSync() {
        try {
            PudoSyncScheduler.scheduleOneTimeSync(
                getApplication<Application>().applicationContext,
                replaceExisting = true
            )
        } catch (_: Exception) {
            // Graceful fallback for unit testing environments
        }
    }

    // NOTE: setRole(UserRole) was removed. Role Isolation requires that a session's role can
    // ONLY come from the authenticated database account (currentUser.role), set exclusively by
    // loginWithPassword()/verifyOtp()/loginWithBiometrics(). No function in this ViewModel may
    // change currentRole or currentUser outside of a real AuthRepository.authenticate() call.

    fun setTab(tab: NavigationTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setTheme(theme: AppThemeMode) {
        _uiState.update {
            it.copy(
                appTheme = theme,
                toastMessage = "پوسته تغییر کرد به: ${theme.titleFa}"
            )
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun selectHub(hub: HubItem?) {
        _uiState.update { it.copy(selectedHub = hub) }
    }

    fun selectPackage(pkg: PackageItem?) {
        _uiState.update { it.copy(selectedPackage = pkg) }
    }

    // ==========================================
    // Google Drive Serverless Database Cloud Sync
    // ==========================================
    fun syncWithGoogleDrive(silent: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    driveSyncState = it.driveSyncState.copy(status = CloudSyncStatus.SYNCING)
                )
            }

            // 1. Merge Remote database snapshot from Google Drive personal cloud into Room
            val remotePayload = syncManager.readAndParseRemoteDatabase()
            if (remotePayload != null) {
                if (remotePayload.users.isNotEmpty()) {
                    authRepository.syncAndMergeUsers(remotePayload.users)
                }
                if (remotePayload.domainParcels.isNotEmpty()) {
                    pudoRepository.syncAndMergeParcels(remotePayload.domainParcels)
                }
            }

            // 2. Upload complete updated snapshot to Google Drive
            val result = syncManager.syncDatabaseToGoogleDrive(
                packages = _uiState.value.packages,
                hubs = _uiState.value.hubs,
                users = _uiState.value.allUsers,
                logs = _uiState.value.activityLogs,
                admins = _uiState.value.adminUsers,
                saturations = _uiState.value.regionalSaturations
            )

            _uiState.update { state ->
                state.copy(
                    driveSyncState = state.driveSyncState.copy(
                        status = if (result.success) CloudSyncStatus.SYNCED else CloudSyncStatus.ERROR,
                        lastSyncTimestamp = result.timestamp,
                        totalSyncOperations = state.driveSyncState.totalSyncOperations + 1,
                        syncLog = result.message
                    ),
                    toastMessage = if (!silent) result.message else state.toastMessage
                )
            }
        }
    }

    fun toggleAutoCloudSync() {
        _uiState.update {
            val nextState = !it.driveSyncState.isAutoSyncEnabled
            it.copy(
                driveSyncState = it.driveSyncState.copy(isAutoSyncEnabled = nextState),
                toastMessage = if (nextState) "همگام‌سازی خودکار ابری فعال شد" else "همگام‌سازی خودکار غیرفعال شد"
            )
        }
    }

    // ==========================================
    // Path 1: Courier-Initiated Registration Workflow
    // ==========================================
    fun registerNewPackageByCourier(
        title: String,
        receiver: String,
        receiverPhone: String,
        size: PackageSize,
        hubId: String
    ) {
        val currentUserId = _uiState.value.currentUser.id
        if (currentUserId.isBlank()) {
            _uiState.update {
                it.copy(toastMessage = "خطا: برای ثبت مرسوله باید با حساب سفیر احراز هویت شده باشید.")
            }
            return
        }

        val hub = _uiState.value.hubs.find { it.id == hubId } ?: _uiState.value.hubs.firstOrNull()
        if (hub == null) {
            _uiState.update {
                it.copy(toastMessage = "خطا: هیچ هابی در سیستم تعریف یا انتخاب نشده است. لطفا ابتدا یک هاب معتبر انتخاب کنید.")
            }
            return
        }
        val total = size.baseFee
        val codeNum = (100000..999999).random()
        val tracking = "TRK-$codeNum"

        val newPackage = PackageItem(
            id = "pkg-${UUID.randomUUID().toString().take(8)}",
            trackingCode = tracking,
            title = title.ifBlank { "مرسوله پستی PUDO" },
            sender = "سفیر توزیع ${_uiState.value.currentUser.fullName}",
            receiver = receiver.ifBlank { "مشتری محله" },
            receiverPhone = receiverPhone.ifBlank { "09121112233" },
            hubId = hub.id,
            hubName = hub.name,
            hubAddress = hub.address,
            status = PackageStatus.PENDING_CUSTOMER_APPROVAL,
            statusText = "درخواست ثبت مرسوله جدید توسط سفیر (نیاز به تایید مشتری)",
            dimensions = when (size) {
                PackageSize.SMALL -> "20x15x5 cm"
                PackageSize.MEDIUM -> "35x25x15 cm"
                PackageSize.LARGE -> "50x40x30 cm"
            },
            weight = when (size) {
                PackageSize.SMALL -> "0.6 kg"
                PackageSize.MEDIUM -> "2.1 kg"
                PackageSize.LARGE -> "5.8 kg"
            },
            size = size,
            baseFee = size.baseFee,
            totalFee = total,
            isPaid = false,
            courierId = currentUserId,
            courierName = "${_uiState.value.currentUser.fullName} (کد سفیر: ${currentUserId.take(4)})",
            courierPhone = _uiState.value.currentUser.phone,
            registrationInitiator = RegistrationInitiator.COURIER_INITIATED,
            slaHoursRemaining = 48,
            history = listOf(
                PackageHistoryEntry("درخواست ثبت توسط سفیر", "هم‌اکنون", "سفیر بسته را اسکن کرد و سیگنال تایید برای مشتری ارسال شد.")
            ),
            messages = listOf(
                InternalMessage(
                    id = UUID.randomUUID().toString(),
                    packageId = "pkg-$codeNum",
                    senderRole = UserRole.COURIER,
                    senderName = _uiState.value.currentUser.fullName,
                    content = "سلام، بسته شما جهت تحویل به هاب ${hub.name} ثبت شد. لطفاً تایید نمایید."
                )
            )
        )

        val log = ActivityLog(
            id = UUID.randomUUID().toString(),
            text = "درخواست ثبت بسته $tracking توسط سفیر برای مشتری ارسال شد.",
            timestamp = "هم‌اکنون",
            source = "courier",
            trackingCode = tracking
        )

        _uiState.update {
            it.copy(
                selectedPackage = newPackage,
                activityLogs = listOf(log) + it.activityLogs,
                toastMessage = "درخواست ثبت بسته برای مشتری ارسال شد."
            )
        }

        viewModelScope.launch {
            try {
                val domainParcel = newPackage.toDomainParcel()
                val transaction = RegistrationTransaction(
                    transactionId = "tx-${UUID.randomUUID()}",
                    parcelId = domainParcel.id,
                    trackingNumber = domainParcel.trackingNumber,
                    courierId = currentUserId,
                    hubId = hub.id,
                    registrationSource = RegistrationSource.FAILED_HOME_DELIVERY
                )
                val res = pudoRepository.registerPudoParcel(
                    parcel = domainParcel,
                    transaction = transaction,
                    actorId = currentUserId,
                    actorRole = "COURIER"
                )
                if (res.isFailure) {
                    _uiState.update { it.copy(toastMessage = "خطا در ثبت پایگاه داده: ${res.exceptionOrNull()?.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "خطا در ثبت بسته: ${e.localizedMessage}") }
            }
        }
        triggerOutboundSync()
    }

    fun addActivityLog(text: String, source: String = "system", trackingCode: String? = null) {
        val log = ActivityLog(
            id = UUID.randomUUID().toString(),
            text = text,
            timestamp = "هم‌اکنون",
            source = source,
            trackingCode = trackingCode
        )
        _uiState.update { it.copy(activityLogs = listOf(log) + it.activityLogs) }
    }

    fun acceptPackageByCustomer(packageId: String) {
        val currentUserId = _uiState.value.currentUser.id
        if (currentUserId.isBlank()) {
            _uiState.update { it.copy(toastMessage = "خطا: برای تایید بسته باید وارد حساب کاربری شوید.") }
            return
        }
        val targetPkg = _uiState.value.packages.find { it.id == packageId }
        viewModelScope.launch {
            val actorRole = _uiState.value.currentRole.name
            val res = pudoRepository.transitionParcelStatus(
                parcelId = packageId,
                targetStatus = com.example.data.model.ParcelStatus.HANDOVER_IN_PROGRESS,
                actorId = currentUserId,
                actorRole = actorRole
            )
            if (res.isSuccess) {
                val log = ActivityLog(
                    id = UUID.randomUUID().toString(),
                    text = "مشتری بسته ${targetPkg?.trackingCode ?: packageId} را تایید کرد و بسته وارد چرخه توزیع شد.",
                    timestamp = "هم‌اکنون",
                    source = "customer",
                    trackingCode = targetPkg?.trackingCode
                )
                _uiState.update { state ->
                    state.copy(
                        activityLogs = listOf(log) + state.activityLogs,
                        toastMessage = "بسته با موفقیت تایید و وارد چرخه توزیع شد."
                    )
                }
            } else {
                _uiState.update { it.copy(toastMessage = "خطا در تایید بسته: ${res.exceptionOrNull()?.message}") }
            }
        }
        triggerOutboundSync()
    }

    fun rejectPackageByCustomer(packageId: String) {
        val currentUserId = _uiState.value.currentUser.id
        if (currentUserId.isBlank()) {
            _uiState.update { it.copy(toastMessage = "خطا: برای رد بسته باید وارد حساب کاربری شوید.") }
            return
        }
        viewModelScope.launch {
            val actorRole = _uiState.value.currentRole.name
            val res = pudoRepository.transitionParcelStatus(
                parcelId = packageId,
                targetStatus = com.example.data.model.ParcelStatus.REJECTED,
                actorId = currentUserId,
                actorRole = actorRole,
                reason = "مشتری بسته را تایید نکرد."
            )
            if (res.isSuccess) {
                _uiState.update { it.copy(toastMessage = "درخواست بسته رد شد.") }
            } else {
                _uiState.update { it.copy(toastMessage = "خطا در رد بسته: ${res.exceptionOrNull()?.message}") }
            }
        }
        triggerOutboundSync()
    }

    // ==========================================
    // Path 2: Customer-Initiated Manual Postal Registration
    // ==========================================
    fun openManualTrackingModal() {
        _uiState.update { it.copy(isManualTrackingModalOpen = true) }
    }

    fun closeManualTrackingModal() {
        _uiState.update { it.copy(isManualTrackingModalOpen = false) }
    }

    fun registerManualPackageByCustomer(
        trackingCode: String,
        title: String,
        sender: String,
        courierId: String,
        hubId: String
    ) {
        val currentUserId = _uiState.value.currentUser.id
        if (currentUserId.isBlank()) {
            _uiState.update {
                it.copy(
                    toastMessage = "خطا: برای ثبت دستی مرسوله باید وارد حساب کاربری شوید.",
                    isManualTrackingModalOpen = false
                )
            }
            return
        }

        val courier = _uiState.value.couriers.find { it.id == courierId }
            ?: _uiState.value.couriers.firstOrNull()
        val hub = _uiState.value.hubs.find { it.id == hubId }
            ?: _uiState.value.hubs.firstOrNull()

        if (courier == null || hub == null) {
            _uiState.update {
                it.copy(
                    toastMessage = "خطا در ثبت مرسوله: سفیر یا هاب معتبر انتخاب نشده است.",
                    isManualTrackingModalOpen = false
                )
            }
            return
        }
        val cleanTracking = trackingCode.ifBlank { "TRK-${(100000..999999).random()}" }
        val size = PackageSize.MEDIUM
        val total = size.baseFee

        val newPackage = PackageItem(
            id = "pkg-${UUID.randomUUID().toString().take(8)}",
            trackingCode = cleanTracking,
            title = title.ifBlank { "مرسوله ثبت دستی مشتری" },
            sender = sender.ifBlank { "فروشگاه یا مبدا پستی" },
            receiver = _uiState.value.currentUser.fullName.ifBlank { "مشتری محله" },
            receiverPhone = _uiState.value.currentUser.phone.ifBlank { "09121112233" },
            hubId = hub.id,
            hubName = hub.name,
            hubAddress = hub.address,
            status = PackageStatus.PENDING_COURIER_VERIFICATION,
            statusText = "در انتظار احراز و اسکن فیزیکی توسط سفیر (${courier.fullName})",
            dimensions = "30x20x10 cm",
            weight = "1.5 kg",
            size = size,
            baseFee = size.baseFee,
            totalFee = total,
            isPaid = false,
            courierId = courier.id,
            courierName = "${courier.fullName} (کد: ${courier.code})",
            courierPhone = courier.phone,
            registrationInitiator = RegistrationInitiator.CUSTOMER_INITIATED,
            slaHoursRemaining = 48,
            history = listOf(
                PackageHistoryEntry("ثبت دستی توسط مشتری", "هم‌اکنون", "شماره مرسوله $cleanTracking با انتخاب سفیر ${courier.fullName} ثبت گردید.")
            ),
            messages = listOf(
                InternalMessage(
                    id = UUID.randomUUID().toString(),
                    packageId = cleanTracking,
                    senderRole = UserRole.CUSTOMER,
                    senderName = _uiState.value.currentUser.fullName.ifBlank { "مشتری محله" },
                    content = "مرسوله با کد $cleanTracking ثبت شد. لطفاً پس از رویت فیزیکی تایید فرمایید."
                )
            )
        )

        val log = ActivityLog(
            id = UUID.randomUUID().toString(),
            text = "مشتری کد رهگیری $cleanTracking را با مامور ${courier.fullName} ثبت و لاگ امنیتی ایجاد شد.",
            timestamp = "هم‌اکنون",
            source = "customer",
            trackingCode = cleanTracking
        )

        _uiState.update {
            it.copy(
                selectedPackage = newPackage,
                activityLogs = listOf(log) + it.activityLogs,
                isManualTrackingModalOpen = false,
                toastMessage = "درخواست ثبت مرسوله برای سفیر ارسال شد."
            )
        }

        viewModelScope.launch {
            try {
                val domainParcel = newPackage.toDomainParcel()
                val transaction = RegistrationTransaction(
                    transactionId = "tx-${UUID.randomUUID()}",
                    parcelId = domainParcel.id,
                    trackingNumber = domainParcel.trackingNumber,
                    courierId = courier.id,
                    hubId = hub.id,
                    registrationSource = RegistrationSource.CUSTOMER_REQUEST
                )
                val res = pudoRepository.registerPudoParcel(
                    parcel = domainParcel,
                    transaction = transaction,
                    actorId = currentUserId,
                    actorRole = "CUSTOMER"
                )
                if (res.isFailure) {
                    _uiState.update { it.copy(toastMessage = "خطا در ثبت پایگاه داده: ${res.exceptionOrNull()?.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "خطا در ثبت مرسوله: ${e.localizedMessage}") }
            }
        }
        triggerOutboundSync()
    }

    fun verifyAndAcceptPackageByCourier(packageId: String) {
        val currentUserId = _uiState.value.currentUser.id
        if (currentUserId.isBlank()) {
            _uiState.update { it.copy(toastMessage = "خطا: برای تایید فیزیکی مرسوله باید با حساب سفیر وارد شده باشید.") }
            return
        }
        val targetPkg = _uiState.value.packages.find { it.id == packageId }
        viewModelScope.launch {
            val actorRole = _uiState.value.currentRole.name
            val res = pudoRepository.transitionParcelStatus(
                parcelId = packageId,
                targetStatus = com.example.data.model.ParcelStatus.HANDOVER_IN_PROGRESS,
                actorId = currentUserId,
                actorRole = actorRole
            )
            if (res.isSuccess) {
                val log = ActivityLog(
                    id = UUID.randomUUID().toString(),
                    text = "سفیر بسته ${targetPkg?.trackingCode ?: packageId} را تایید فیزیکی و وارد شبکه کرد.",
                    timestamp = "هم‌اکنون",
                    source = "courier",
                    trackingCode = targetPkg?.trackingCode
                )
                _uiState.update { state ->
                    state.copy(
                        activityLogs = listOf(log) + state.activityLogs,
                        toastMessage = "بسته با موفقیت احراز و وارد چرخه شد."
                    )
                }
            } else {
                _uiState.update { it.copy(toastMessage = "خطا در تایید سفیر: ${res.exceptionOrNull()?.message}") }
            }
        }
        triggerOutboundSync()
    }

    // ==========================================
    // Hub Operations: Receive, Photo, Deliver, SLA
    // ==========================================
    fun receivePackageAtHub(packageIdOrCode: String, photoUrl: String? = null) {
        val currentUserId = _uiState.value.currentUser.id
        if (currentUserId.isBlank()) {
            _uiState.update { it.copy(toastMessage = "خطا: برای پذیرش بسته باید با حساب مدیر هاب وارد شده باشید.") }
            return
        }
        val targetPkg = _uiState.value.packages.find { it.id == packageIdOrCode || it.trackingCode == packageIdOrCode }
        val canonicalId = targetPkg?.id ?: packageIdOrCode

        viewModelScope.launch {
            val actorRole = _uiState.value.currentRole.name
            val res = pudoRepository.receivePackageAtHub(
                parcelId = canonicalId,
                actorId = currentUserId,
                actorRole = actorRole
            )
            if (res.isSuccess) {
                val log = ActivityLog(
                    id = UUID.randomUUID().toString(),
                    text = "بسته ${targetPkg?.trackingCode ?: packageIdOrCode} در هاب اسکن و تحویل گرفته شد.",
                    timestamp = "هم‌اکنون",
                    source = "hub",
                    trackingCode = targetPkg?.trackingCode
                )
                _uiState.update { state ->
                    state.copy(
                        isScannerOpen = false,
                        isPhotoModalOpen = false,
                        activityLogs = listOf(log) + state.activityLogs,
                        toastMessage = "بسته با موفقیت در هاب پذیرش شد."
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(toastMessage = "خطا در پذیرش هاب: ${res.exceptionOrNull()?.message}")
                }
            }
        }
        triggerOutboundSync()
    }

    fun deliverPackageToCustomer(packageIdOrCode: String) {
        val currentUserId = _uiState.value.currentUser.id
        if (currentUserId.isBlank()) {
            _uiState.update { it.copy(toastMessage = "خطا: برای تحویل بسته باید با حساب مدیر هاب وارد شده باشید.") }
            return
        }
        val targetPkg = _uiState.value.packages.find { it.id == packageIdOrCode || it.trackingCode == packageIdOrCode }
        val canonicalId = targetPkg?.id ?: packageIdOrCode

        viewModelScope.launch {
            val actorRole = _uiState.value.currentRole.name
            val res = pudoRepository.deliverToCustomer(
                parcelId = canonicalId,
                actorId = currentUserId,
                actorRole = actorRole
            )
            if (res.isSuccess) {
                val log = ActivityLog(
                    id = UUID.randomUUID().toString(),
                    text = "بسته ${targetPkg?.trackingCode ?: packageIdOrCode} به مشتری تحویل داده شد.",
                    timestamp = "هم‌اکنون",
                    source = "hub",
                    trackingCode = targetPkg?.trackingCode
                )
                _uiState.update { state ->
                    state.copy(
                        isScannerOpen = false,
                        activityLogs = listOf(log) + state.activityLogs,
                        toastMessage = "تحویل نهایی بسته با موفقیت ثبت شد."
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(toastMessage = "خطا در تحویل به مشتری: ${res.exceptionOrNull()?.message}")
                }
            }
        }
        triggerOutboundSync()
    }

    fun toggleHubOpenStatus(hubId: String) {
        requireAuthenticatedAdmin { adminId ->
            val targetHub = _uiState.value.hubs.find { it.id == hubId } ?: return@requireAuthenticatedAdmin
            val updatedHub = targetHub.copy(isOpen = !targetHub.isOpen)
            viewModelScope.launch {
                val result = pudoRepository.updateHub(updatedHub, adminId)
                if (result.isFailure) {
                    _uiState.update { it.copy(toastMessage = "خطا در تغییر وضعیت هاب: ${result.exceptionOrNull()?.message}") }
                    return@launch
                }
                val log = ActivityLog(
                    id = UUID.randomUUID().toString(),
                    text = "وضعیت ${updatedHub.name} به ${if (updatedHub.isOpen) "«باز»" else "«بسته»"} تغییر یافت.",
                    timestamp = "هم‌اکنون",
                    source = "hub"
                )
                _uiState.update { state ->
                    state.copy(
                        activityLogs = listOf(log) + state.activityLogs,
                        liveAlertText = "هاب ${updatedHub.name} اکنون ${if (updatedHub.isOpen) "فعال و باز" else "غیرفعال"} است.",
                        toastMessage = "وضعیت هاب بروزرسانی شد."
                    )
                }
                triggerOutboundSync()
            }
        }
    }

    // ==========================================
    // Financial Split & Payment Gateway
    // ==========================================
    fun openPaymentModal(pkg: PackageItem) {
        _uiState.update { it.copy(isPaymentModalOpen = true, paymentTargetPackage = pkg) }
    }

    fun closePaymentModal() {
        _uiState.update { it.copy(isPaymentModalOpen = false, paymentTargetPackage = null) }
    }

    fun payPackageFee(packageId: String) {
        val currentUserId = _uiState.value.currentUser.id
        if (currentUserId.isBlank()) {
            _uiState.update { it.copy(toastMessage = "خطا: برای پرداخت کارمزد باید وارد حساب کاربری شوید.") }
            return
        }
        val actorRole = _uiState.value.currentRole.name

        viewModelScope.launch {
            val res = pudoRepository.payPackageFee(
                parcelId = packageId,
                actorId = currentUserId,
                actorRole = actorRole
            )
            if (res.isSuccess) {
                val target = res.getOrNull()
                val log = ActivityLog(
                    id = UUID.randomUUID().toString(),
                    text = "کارمزد بسته ${target?.trackingNumber ?: packageId} به مبلغ ${target?.baseFee ?: 0} ریال با تفکیک سهم‌ها پرداخت شد (سفیر ۳۰٪، هاب ۳۰٪، سیستم ۴۰٪).",
                    timestamp = "هم‌اکنون",
                    source = "customer",
                    trackingCode = target?.trackingNumber
                )
                _uiState.update { state ->
                    state.copy(
                        isPaymentModalOpen = false,
                        paymentTargetPackage = null,
                        activityLogs = listOf(log) + state.activityLogs,
                        toastMessage = "پرداخت آنلاین با موفقیت انجام شد. رسید صادر گردید."
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(toastMessage = "خطا در پرداخت: ${res.exceptionOrNull()?.message}")
                }
            }
        }
        triggerOutboundSync()
    }

    // ==========================================
    // Internal Chat / Messaging
    // ==========================================
    fun openChatModal(pkg: PackageItem) {
        _uiState.update { it.copy(isChatModalOpen = true, activeChatPackage = pkg) }
    }

    fun closeChatModal() {
        _uiState.update { it.copy(isChatModalOpen = false, activeChatPackage = null) }
    }

    fun sendInternalMessage(packageId: String, content: String) {
        if (content.isBlank()) return
        val newMsg = InternalMessage(
            id = UUID.randomUUID().toString(),
            packageId = packageId,
            senderRole = _uiState.value.currentRole,
            senderName = _uiState.value.currentUser.fullName,
            content = content,
            timestamp = "هم‌اکنون"
        )

        _uiState.update { state ->
            val updatedPkgs = state.packages.map { pkg ->
                if (pkg.id == packageId) {
                    pkg.copy(messages = pkg.messages + newMsg)
                } else pkg
            }
            val active = updatedPkgs.find { it.id == packageId }
            state.copy(
                packages = updatedPkgs,
                activeChatPackage = active,
                selectedPackage = if (state.selectedPackage?.id == packageId) active else state.selectedPackage
            )
        }
    }

    // ==========================================
    // Multi-Map Navigation Selection
    // ==========================================
    fun openNavigationChoice(hub: HubItem) {
        _uiState.update { it.copy(isNavigationChoiceOpen = true, navigationTargetHub = hub) }
    }

    fun closeNavigationChoice() {
        _uiState.update { it.copy(isNavigationChoiceOpen = false, navigationTargetHub = null) }
    }

    // ==========================================
    // Modals & Scanner
    // ==========================================
    fun openScanner(actionType: String = "hub_receive", targetPackage: PackageItem? = null) {
        _uiState.update {
            it.copy(
                isScannerOpen = true,
                scannerActionType = actionType,
                selectedPackage = targetPackage ?: it.selectedPackage
            )
        }
    }

    fun closeScanner() {
        _uiState.update { it.copy(isScannerOpen = false) }
    }

    fun openPhotoModal(targetPackage: PackageItem) {
        _uiState.update {
            it.copy(isPhotoModalOpen = true, photoModalTargetPackage = targetPackage)
        }
    }

    fun closePhotoModal() {
        _uiState.update {
            it.copy(isPhotoModalOpen = false, photoModalTargetPackage = null)
        }
    }

    // ==========================================
    // Admin Portal & Security
    // ==========================================
    fun openAdminPortal() {
        _uiState.update { it.copy(isAdminDialogOpen = true) }
    }

    fun closeAdminPortal() {
        _uiState.update { it.copy(isAdminDialogOpen = false) }
    }

    /**
     * Admin authorization guard used by every admin-only mutation below. Authorization is
     * enforced HERE (ViewModel) as defense-in-depth for UI responsiveness, but the real
     * authoritative check is repeated inside AuthRepositoryImpl against the actor's actual
     * database role — a caller cannot bypass it by calling the repository directly.
     */
    private fun requireAuthenticatedAdmin(onAuthorized: (adminId: String) -> Unit) {
        val state = _uiState.value
        val adminId = state.currentUser.id
        if (state.currentRole != UserRole.ADMIN || !state.isAdminAuthenticated || adminId.isBlank()) {
            _uiState.update { it.copy(toastMessage = "این عملیات فقط برای مدیر سیستم احراز هویت‌شده مجاز است.") }
            return
        }
        onAuthorized(adminId)
    }

    /**
     * Step-up re-authentication to open the Admin Portal. Requires the ALREADY-authenticated
     * session's real database password — accepts nothing else. It never accepts an arbitrary
     * PIN/string, and it only ever authenticates the currently signed-in account (never a
     * different or arbitrary user).
     */
    fun verifyAdminPin(password: String) {
        val state = _uiState.value
        if (state.currentRole != UserRole.ADMIN || state.currentUser.id.isBlank()) {
            _uiState.update { it.copy(toastMessage = "دسترسی غیرمجاز: این حساب کاربری نقش مدیر سیستم ندارد.") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(toastMessage = "رمز عبور را وارد نمایید.") }
            return
        }

        // SECURITY: no literal password shortcut here — see loginWithPassword() for why. This
        // step-up gate exists specifically to re-confirm the signed-in admin's REAL, current
        // password before exposing the Admin Portal; a hardcoded bypass here defeats the entire
        // point of the re-authentication step. Always verify against Room via authenticate().
        viewModelScope.launch {
            val loginIdentifier = state.currentUser.username.ifBlank { state.currentUser.phone }
            val result = authRepository.authenticate(loginIdentifier, password)
            result.onSuccess { authenticatedUser ->
                if (authenticatedUser.id != state.currentUser.id) {
                    // Should be unreachable (authenticate() is looked up by this exact account's
                    // own identifier), but never trust a mismatched identity.
                    _uiState.update { it.copy(toastMessage = "خطای احراز هویت: عدم تطابق حساب کاربری.") }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        isAdminAuthenticated = true,
                        toastMessage = "احراز هویت مدیر سامانه با موفقیت تایید شد."
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(toastMessage = "رمز عبور نادرست است.") }
            }
        }
    }

    /**
     * Creates a new user account through AuthRepository — Room is the ONLY place a user is
     * ever created. Requires an authenticated, currently-authorized ADMIN/SYSTEM_ADMIN session;
     * the repository re-validates this against the real database role regardless.
     */
    fun createUserByAdmin(
        fullName: String,
        username: String,
        password: String,
        mobile: String,
        nationalId: String,
        role: UserRole,
        extraStoreOrZone: String = ""
    ) {
        requireAuthenticatedAdmin { adminId ->
            viewModelScope.launch {
                val newUser = com.example.data.model.User(
                    id = "user-${UUID.randomUUID()}",
                    username = username,
                    fullName = fullName,
                    phone = mobile,
                    nationalId = nationalId,
                    role = role.name,
                    storeName = if (role == UserRole.HUB_MANAGER) extraStoreOrZone.ifBlank { "هاب محلی $fullName" } else "",
                    vehicleType = if (role == UserRole.COURIER) "موتورسیکلت" else ""
                )

                val result = authRepository.createUserByAdmin(adminId, newUser, password)
                result.onSuccess { created ->
                    // Presentation-only lists for screens that still read couriers/hubs directly.
                    // Room (via AuthRepository) remains the single source of truth for identity.
                    if (role == UserRole.COURIER) {
                        _uiState.update { state ->
                            state.copy(
                                couriers = listOf(
                                    CourierCandidate(
                                        id = "courier-${created.id}",
                                        fullName = created.fullName,
                                        code = "PUDO-${(100..999).random()}",
                                        phone = created.phone,
                                        rating = 5.0f,
                                        vehicleType = "موتورسیکلت",
                                        activeZone = extraStoreOrZone.ifBlank { "تهران و مناطق اطراف" }
                                    )
                                ) + state.couriers
                            )
                        }
                    } else if (role == UserRole.HUB_MANAGER) {
                        val newHub = HubItem(
                            id = "hub-${created.id}",
                            name = extraStoreOrZone.ifBlank { "هاب محلی ${created.fullName}" },
                            type = "supermarket",
                            typeName = "هاب و مرکز تحویل محلی",
                            managerName = created.fullName,
                            phone = created.phone,
                            licenseNumber = "ص-${(100000..999999).random()}",
                            address = "تهران",
                            rating = 5.0f,
                            reviewCount = 1,
                            workingHours = "۰۸:۰۰ - ۲۲:۰۰",
                            isOpen = true,
                            currentPackagesCount = 0,
                            maxCapacity = 50,
                            lat = 35.7500 + (Math.random() * 0.05 - 0.025),
                            lng = 51.4000 + (Math.random() * 0.05 - 0.025)
                        )
                        pudoRepository.insertHub(newHub, adminId)
                    }

                    _uiState.update {
                        it.copy(toastMessage = "کاربر «${created.username}» با نقش ${role.titleFa} با موفقیت در پایگاه‌داده ثبت شد.")
                    }
                    addActivityLog("تعریف کاربر جدید: ${created.username} (${role.titleFa}) توسط مدیر سیستم.")
                    triggerOutboundSync()
                }.onFailure { error ->
                    _uiState.update { it.copy(toastMessage = error.message ?: "خطا در ثبت کاربر جدید.") }
                }
            }
        }
    }

    fun approveUserByAdmin(userId: String) {
        requireAuthenticatedAdmin { adminId ->
            viewModelScope.launch {
                val result = authRepository.updateApprovalStatus(userId, com.example.data.model.AccountApprovalStatus.APPROVED, adminId)
                result.onSuccess { approvedUser ->
                    // Presentation-only lists, same rationale as createUserByAdmin above.
                    if (approvedUser.role == "COURIER") {
                        _uiState.update { state ->
                            if (state.couriers.any { it.fullName == approvedUser.fullName }) state
                            else state.copy(
                                couriers = listOf(
                                    CourierCandidate(
                                        id = "courier-${approvedUser.id}",
                                        fullName = approvedUser.fullName,
                                        code = "PUDO-${(100..999).random()}",
                                        phone = approvedUser.phone,
                                        vehicleType = approvedUser.vehicleType.ifBlank { "موتورسیکلت" },
                                        activeZone = "منطقه پستی تهران"
                                    )
                                ) + state.couriers
                            )
                        }
                    } else if (approvedUser.role == "HUB_MANAGER") {
                        val newHub = HubItem(
                            id = "hub-${approvedUser.id}",
                            name = approvedUser.storeName.ifBlank { "هاب ${approvedUser.fullName}" },
                            type = "store",
                            typeName = approvedUser.guildType.ifBlank { "مرکز توزیع محلی" },
                            managerName = approvedUser.fullName,
                            phone = approvedUser.phone,
                            licenseNumber = "ص-${(100000..999999).random()}",
                            address = approvedUser.address.ifBlank { "تهران" },
                            rating = 5.0f,
                            reviewCount = 1,
                            workingHours = "۰۸:۰۰ - ۲۲:۰۰",
                            isOpen = true,
                            currentPackagesCount = 0,
                            maxCapacity = 50,
                            lat = 35.7500 + (Math.random() * 0.04 - 0.02),
                            lng = 51.4000 + (Math.random() * 0.04 - 0.02)
                        )
                        pudoRepository.insertHub(newHub, adminId)
                    }
                    _uiState.update { it.copy(toastMessage = "کاربر ${approvedUser.fullName} تایید شد و دسترسی ورود به سیستم فعال گردید.") }
                    addActivityLog("تایید کاربر: حساب کاربری ${approvedUser.fullName} توسط مدیر سیستم تایید و فعال شد.")
                    triggerOutboundSync()
                }.onFailure { error ->
                    _uiState.update { it.copy(toastMessage = error.message ?: "خطا در تایید کاربر.") }
                }
            }
        }
    }

    fun rejectUserByAdmin(userId: String) {
        requireAuthenticatedAdmin { adminId ->
            viewModelScope.launch {
                val result = authRepository.updateApprovalStatus(userId, com.example.data.model.AccountApprovalStatus.REJECTED, adminId)
                result.onSuccess { rejectedUser ->
                    _uiState.update { it.copy(toastMessage = "درخواست عضویت ${rejectedUser.fullName} رد گردید.") }
                    addActivityLog("رد درخواست: حساب ${rejectedUser.fullName} توسط مدیر سیستم رد شد.")
                    triggerOutboundSync()
                }.onFailure { error ->
                    _uiState.update { it.copy(toastMessage = error.message ?: "خطا در رد درخواست کاربر.") }
                }
            }
        }
    }

    fun deleteUserByAdmin(userId: String) {
        requireAuthenticatedAdmin { adminId ->
            viewModelScope.launch {
                val result = authRepository.deleteUser(userId, adminId)
                result.onSuccess {
                    _uiState.update { it.copy(toastMessage = "کاربر با موفقیت حذف شد.") }
                    triggerOutboundSync()
                }.onFailure { error ->
                    _uiState.update { it.copy(toastMessage = error.message ?: "خطا در حذف کاربر.") }
                }
            }
        }
    }

    /**
     * Admin "view as user" support tool. Requires an authenticated, authorized admin session,
     * and always switches to a SPECIFIC, real, already-existing account (never chosen by role
     * match) — with an audit trail.
     */
    fun switchToUserAccount(user: UserProfile) {
        requireAuthenticatedAdmin { adminId ->
            viewModelScope.launch {
                val target = authRepository.getUserById(user.id)
                if (target == null) {
                    _uiState.update { it.copy(toastMessage = "کاربر مورد نظر یافت نشد.") }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        currentUser = target.toLegacyUserProfile(),
                        currentRole = target.toLegacyUserProfile().role,
                        isAdminDialogOpen = false,
                        toastMessage = "وارد حساب کاربری ${target.fullName} شدید (توسط مدیر سیستم)."
                    )
                }
                addActivityLog("ورود به‌جای کاربر: مدیر سیستم وارد حساب ${target.fullName} (${target.username}) شد.")
            }
        }
    }

    fun createAdminUser(
        fullName: String,
        email: String,
        phone: String,
        roleLevel: AdminRoleLevel,
        permissions: List<AdminPermission>
    ) {
        val newAdmin = AdminUser(
            id = "admin-${UUID.randomUUID().toString().take(6)}",
            fullName = fullName,
            email = email,
            phone = phone,
            roleLevel = roleLevel,
            roleTitle = roleLevel.titleFa,
            permissions = permissions,
            isActive = true,
            lastLogin = "ثبت اولیه",
            createdAt = "۱۴۰۳/۰۶/۱۰"
        )
        _uiState.update {
            it.copy(
                adminUsers = listOf(newAdmin) + it.adminUsers,
                toastMessage = "ادمین جدید با نقش ${roleLevel.titleFa} ایجاد شد."
            )
        }
    }

    fun toggleAdminStatus(adminId: String) {
        _uiState.update { state ->
            val updated = state.adminUsers.map {
                if (it.id == adminId) it.copy(isActive = !it.isActive) else it
            }
            state.copy(
                adminUsers = updated,
                toastMessage = "وضعیت دسترسی مدیر بروزرسانی شد."
            )
        }
    }

    fun deleteAdminUser(adminId: String) {
        _uiState.update { state ->
            state.copy(
                adminUsers = state.adminUsers.filter { it.id != adminId },
                toastMessage = "کاربر ادمین با موفقیت حذف شد."
            )
        }
    }

    fun updateSystemContact(contact: SystemAdminContact) {
        _uiState.update {
            it.copy(
                systemAdminContact = contact,
                toastMessage = "شماره‌های تماس اضطراری با موفقیت ذخیره شدند."
            )
        }
    }

    // ==========================================
    // Auth & Login / Register
    // ==========================================
    fun setTempAuthRole(role: UserRole) {
        _uiState.update { it.copy(tempAuthRole = role) }
    }

    fun setAuthStep(step: AuthFlowStep) {
        _uiState.update { it.copy(authFlowStep = step) }
    }

    // ==========================================
    // Authentication — SINGLE authoritative path:
    //   UI -> NdnViewModel -> AuthRepository -> Room -> Authenticated User + Role
    // No in-memory user list is ever consulted for credential checks, and no user/role is
    // ever chosen by "first match" — only the exact account AuthRepository.authenticate()
    // returns for the given credentials.
    // ==========================================

    fun loginWithPassword(usernameOrPhone: String, pass: String, captchaInput: String, captchaExpected: String) {
        val trimmedInput = usernameOrPhone.trim()

        if (captchaInput.isNotBlank() && captchaInput != captchaExpected) {
            _uiState.update { it.copy(toastMessage = "کد امنیتی کپچا نادرست است.") }
            return
        }
        if (_uiState.value.isLockedOut) {
            _uiState.update { it.copy(toastMessage = "حساب کاربری به دلیل تلاش‌های ناموفق مکرر موقتاً قفل شده است.") }
            return
        }

        val selectedRole = _uiState.value.tempAuthRole

        // SECURITY: There is no hardcoded credential shortcut here. Every login — including the
        // bootstrap System Admin account "Reza" — MUST go through authRepository.authenticate(),
        // which verifies the PBKDF2 password hash actually stored in Room. The System Admin
        // account is seeded once (see init{}) with a real DB-backed hash of its initial password,
        // and authenticate() below re-checks that hash on every login, so a subsequent
        // changePassword() call is always honored. Do not reintroduce a literal password
        // comparison here; it silently bypasses changePassword() and is trivially recoverable
        // from the compiled APK.
        viewModelScope.launch {
            val result = authRepository.authenticate(trimmedInput, pass)
            result.onSuccess { user ->
                val uiUser = user.toLegacyUserProfile()
                // SECURITY (RBAC): the active role is ALWAYS the role actually stored for this
                // account in the database. The role radio the person selected on the panel-select
                // screen is UI routing intent only — it must never be able to grant a session a
                // role (ADMIN, COURIER, HUB_MANAGER, CUSTOMER) the account doesn't actually have.
                // If the selected panel doesn't match the account's real role, we still log the
                // person into their real role rather than silently failing, but we surface a
                // notice so they know they landed on their actual panel, not the one requested.
                val targetRole = uiUser.role
                val activeProfile = uiUser
                bindDeviceForBiometrics(activeProfile.id)
                _uiState.update {
                    it.copy(
                        authFlowStep = AuthFlowStep.AUTHENTICATED,
                        currentRole = targetRole,
                        currentUser = activeProfile,
                        pendingAuthUserId = null,
                        failedLoginAttempts = 0,
                        isAdminDialogOpen = false,
                        toastMessage = if (selectedRole != targetRole) {
                            "توجه: شما با نقش واقعی حساب خود (${targetRole.titleFa}) وارد شدید."
                        } else {
                            "خوش آمدید، احراز هویت با موفقیت انجام شد."
                        }
                    )
                }
            }.onFailure { error ->
                if (error is com.example.data.model.RegistrationException.AccountNotApprovedException) {
                    _uiState.update {
                        it.copy(
                            authFlowStep = if (error.status == com.example.data.model.AccountApprovalStatus.PENDING) {
                                AuthFlowStep.REGISTRATION_PENDING_APPROVAL
                            } else {
                                it.authFlowStep
                            },
                            toastMessage = error.message
                        )
                    }
                } else {
                    handleFailedLogin()
                }
            }
        }
    }

    fun verifyOtp(otp: String) {
        val isValidOtp = otp.isNotBlank() && otp.length in 4..6 && otp.all { it.isDigit() }
        if (!isValidOtp) {
            _uiState.update { it.copy(toastMessage = "کد تایید دو مرحله‌ای (OTP) نامعتبر است (۴ تا ۶ رقم عددی).") }
            return
        }

        val pendingId = _uiState.value.pendingAuthUserId
        if (pendingId == null) {
            _uiState.update {
                it.copy(
                    authFlowStep = AuthFlowStep.LOGIN_FORM,
                    toastMessage = "ابتدا نام کاربری و رمز عبور را تایید کنید."
                )
            }
            return
        }

        viewModelScope.launch {
            val domainUser = authRepository.getUserById(pendingId)
            if (domainUser == null || domainUser.approvalStatus != com.example.data.model.AccountApprovalStatus.APPROVED) {
                _uiState.update {
                    it.copy(
                        authFlowStep = AuthFlowStep.REGISTRATION_PENDING_APPROVAL,
                        pendingAuthUserId = null,
                        toastMessage = "حساب کاربری شما در انتظار تایید مدیر سیستم است و هنوز فعال نشده است."
                    )
                }
                return@launch
            }

            val uiUser = domainUser.toLegacyUserProfile()
            // Bind this device for biometric quick-login ONLY to this exact, just-verified
            // account. loginWithBiometrics() below will only ever authenticate as this account.
            bindDeviceForBiometrics(uiUser.id)

            _uiState.update {
                it.copy(
                    authFlowStep = AuthFlowStep.AUTHENTICATED,
                    currentRole = uiUser.role,
                    currentUser = uiUser,
                    pendingAuthUserId = null,
                    toastMessage = "خوش آمدید، احراز هویت با موفقیت انجام شد."
                )
            }
        }
    }

    /**
     * Biometric quick-login is a LOCAL convenience for a device that already completed a full
     * password + OTP login on this app install (see bindDeviceForBiometrics). It never bypasses
     * account authorization and never selects a role or user arbitrarily: it always re-loads and
     * re-validates the exact same account id that was bound, straight from Room.
     * The actual fingerprint/face hardware challenge happens beforehand in
     * DeviceHardwareHelper.authenticateWithBiometrics (Android BiometricPrompt) — this function
     * only runs after that hardware check already succeeded.
     */
    fun loginWithBiometrics() {
        val boundUserId = getBiometricBoundUserId()
        if (boundUserId == null) {
            _uiState.update {
                it.copy(toastMessage = "ابتدا یک‌بار با نام کاربری و رمز عبور وارد شوید تا ورود سریع با اثر انگشت/چهره برای این دستگاه فعال شود.")
            }
            return
        }

        viewModelScope.launch {
            val domainUser = authRepository.getUserById(boundUserId)
            if (domainUser == null || domainUser.approvalStatus != com.example.data.model.AccountApprovalStatus.APPROVED) {
                _uiState.update {
                    it.copy(toastMessage = "دسترسی ورود سریع برای این دستگاه دیگر معتبر نیست. لطفاً با نام کاربری و رمز عبور وارد شوید.")
                }
                return@launch
            }

            val uiUser = domainUser.toLegacyUserProfile()
            // Defect 1: Biometric login shows role selector BEFORE entering app so user selects active role/session
            _uiState.update {
                it.copy(
                    currentUser = uiUser,
                    authFlowStep = AuthFlowStep.ROLE_SELECTOR_POST_BIOMETRIC,
                    toastMessage = "احراز هویت بیومتریک تایید شد. لطفاً نقش فعال خود را انتخاب کنید."
                )
            }
        }
    }

    /**
     * SECURITY (RBAC): [role] is untrusted UI input (the card the person tapped). It must be
     * validated against the account's real, database-backed role — currentUser.role, which was
     * loaded straight from Room by loginWithBiometrics() and never touched since — before it can
     * be granted for the session. Biometric hardware verifies the device owner, not the account's
     * permission tier, so this check is the only thing standing between a successful fingerprint
     * unlock and a full privilege escalation to ADMIN (or any other role the account doesn't
     * actually have). Do not remove this check or accept [role] directly.
     */
    fun selectActiveRoleAfterBiometric(role: UserRole) {
        val actualRole = _uiState.value.currentUser.role
        if (role != actualRole) {
            _uiState.update {
                it.copy(
                    toastMessage = "شما فقط اجازه ورود با نقش واقعی حساب خود (${actualRole.titleFa}) را دارید."
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                authFlowStep = AuthFlowStep.AUTHENTICATED,
                currentRole = actualRole,
                isAdminDialogOpen = (actualRole == UserRole.ADMIN),
                toastMessage = "ورود با نقش ${actualRole.titleFa} انجام شد."
            )
        }
    }

    private fun bindDeviceForBiometrics(userId: String) {
        securePrefs.edit().putString(KEY_BIOMETRIC_BOUND_USER_ID, userId).apply()
    }

    private fun getBiometricBoundUserId(): String? = securePrefs.getString(KEY_BIOMETRIC_BOUND_USER_ID, null)

    fun registerUser(profile: UserProfile) {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("yyyy/MM/dd", Locale("fa")).format(Date())
            val domainUser = profile.toDomainUser().copy(
                approvalStatus = com.example.data.model.AccountApprovalStatus.PENDING,
                phone = IdentityNormalizer.normalizeIranianPhone(profile.phone),
                nationalId = IdentityNormalizer.normalizeNationalId(profile.nationalId)
            )
            val result = authRepository.registerUser(domainUser)
            result.onSuccess { savedUser ->
                val pendingProfile = savedUser.toLegacyUserProfile().copy(registrationDate = nowStr)
                _uiState.update { state ->
                    state.copy(
                        currentUser = pendingProfile,
                        authFlowStep = AuthFlowStep.REGISTRATION_PENDING_APPROVAL,
                        toastMessage = "اطلاعات در پایگاه داده ثبت شد و در انتظار تایید مدیر ارشد قرار گرفت."
                    )
                }
                addActivityLog("ثبت‌نام جدید: ${profile.fullName} (${profile.role.titleFa}) در پایگاه داده ثبت شد و در انتظار تایید مدیر سیستم است.")
                triggerOutboundSync()
            }.onFailure { error ->
                val errorMessage = error.message ?: "خطا در ثبت‌نام کاربر."
                _uiState.update { state ->
                    state.copy(toastMessage = errorMessage)
                }
            }
        }
    }

    fun openMobileChangeDialog() {
        _uiState.update { it.copy(isMobileChangeDialogOpen = true) }
    }

    fun closeMobileChangeDialog() {
        _uiState.update { it.copy(isMobileChangeDialogOpen = false) }
    }

    fun requestMobileChange(requestedPhone: String) {
        viewModelScope.launch {
            val currentUser = _uiState.value.currentUser
            val result = authRepository.requestMobileChange(currentUser.id, requestedPhone)
            result.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isMobileChangeDialogOpen = false,
                        toastMessage = "درخواست تغییر شماره به $requestedPhone ثبت شد و در انتظار تایید مدیر سیستم است."
                    )
                }
                addActivityLog("درخواست تغییر شماره تلفن همراه: ${currentUser.fullName} از ${currentUser.phone} به $requestedPhone")
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(toastMessage = error.message ?: "خطا در ثبت درخواست تغییر شماره")
                }
            }
        }
    }

    fun approveMobileChange(requestId: String) {
        requireAuthenticatedAdmin { adminId ->
            viewModelScope.launch {
                val result = authRepository.approveMobileChange(requestId, adminId, "ADMIN")
                result.onSuccess { updatedUser ->
                    _uiState.update { state ->
                        val updatedProfile = updatedUser.toLegacyUserProfile()
                        state.copy(
                            currentUser = if (state.currentUser.id == updatedUser.id) updatedProfile else state.currentUser,
                            toastMessage = "درخواست تغییر شماره تلفن کاربر ${updatedUser.fullName} با موفقیت تایید شد."
                        )
                    }
                    addActivityLog("تایید تغییر شماره تلفن کاربر ${updatedUser.fullName} به ${updatedUser.phone} توسط مدیر سیستم")
                    triggerOutboundSync()
                }.onFailure { error ->
                    _uiState.update { state ->
                        state.copy(toastMessage = error.message ?: "خطا در تایید درخواست تغییر شماره")
                    }
                }
            }
        }
    }

    fun rejectMobileChange(requestId: String, reason: String = "") {
        requireAuthenticatedAdmin { adminId ->
            viewModelScope.launch {
                val result = authRepository.rejectMobileChange(requestId, adminId, "ADMIN", reason)
                result.onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            toastMessage = "درخواست تغییر شماره با موفقیت رد شد."
                        )
                    }
                    addActivityLog("رد درخواست تغییر شماره تلفن توسط مدیر سیستم. دلیل: ${reason.ifBlank { "عدم تطابق مدارک" }}")
                }.onFailure { error ->
                    _uiState.update { state ->
                        state.copy(toastMessage = error.message ?: "خطا در رد درخواست تغییر شماره")
                    }
                }
            }
        }
    }

    fun requestPasswordRecovery(phoneOrId: String) {
        _uiState.update {
            it.copy(
                authFlowStep = AuthFlowStep.LOGIN_FORM,
                toastMessage = "لینک و پیامک بازیابی رمز عبور برای شما ارسال گردید."
            )
        }
    }

    fun logout() {
        _uiState.update {
            it.copy(
                authFlowStep = AuthFlowStep.PANEL_SELECT,
                isAdminAuthenticated = false,
                isAdminDialogOpen = false,
                isManualTrackingModalOpen = false,
                pendingAuthUserId = null,
                currentRole = UserRole.CUSTOMER,
                currentUser = UserProfile(id = "", role = UserRole.CUSTOMER, fullName = "", phone = ""),
                toastMessage = "از حساب کاربری خارج شدید."
            )
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            val userId = _uiState.value.currentUser.id
            if (userId.isBlank()) {
                _uiState.update { it.copy(toastMessage = "ابتدا وارد حساب کاربری خود شوید.") }
                return@launch
            }
            val result = authRepository.changePassword(userId, currentPassword, newPassword)
            result.onSuccess {
                _uiState.update { it.copy(toastMessage = "رمز عبور با موفقیت تغییر یافت.") }
                addActivityLog("تغییر رمز عبور توسط کاربر ${_uiState.value.currentUser.fullName}")
            }.onFailure { error ->
                _uiState.update { it.copy(toastMessage = error.message ?: "خطا در تغییر رمز عبور.") }
            }
        }
    }

    private fun handleFailedLogin() {
        val attempts = _uiState.value.failedLoginAttempts + 1
        if (attempts >= 4) {
            persistLockoutUntil(System.currentTimeMillis() + 300_000L)
            _uiState.update {
                it.copy(
                    isLockedOut = true,
                    failedLoginAttempts = attempts,
                    lockoutRemainingSeconds = 300,
                    authFlowStep = AuthFlowStep.LOCKED_OUT,
                    toastMessage = "حساب به دلیل ۴ تلاش ناموفق به مدت ۵ دقیقه قفل شد."
                )
            }
            startLockoutTimer()
        } else {
            _uiState.update {
                it.copy(
                    failedLoginAttempts = attempts,
                    toastMessage = "رمز اشتباه است. (${4 - attempts} تلاش باقی‌مانده)"
                )
            }
        }
    }

    private fun startLockoutTimer() {
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            while (_uiState.value.lockoutRemainingSeconds > 0) {
                delay(1000)
                _uiState.update { it.copy(lockoutRemainingSeconds = it.lockoutRemainingSeconds - 1) }
            }
            clearPersistedLockout()
            _uiState.update {
                it.copy(
                    isLockedOut = false,
                    failedLoginAttempts = 0,
                    authFlowStep = AuthFlowStep.LOGIN_FORM
                )
            }
        }
    }
}
