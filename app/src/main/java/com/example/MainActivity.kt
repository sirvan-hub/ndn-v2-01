package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.DeviceHardwareHelper
import com.example.viewmodel.NdnViewModel

class MainActivity : FragmentActivity() {
    private val viewModel: NdnViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current

            // Request All Core Hardware Permissions (Camera, GPS Location, Telephony, SMS)
            val permissionsToRequest = remember {
                val list = mutableListOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.SEND_SMS
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    list.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                list.toTypedArray()
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                val cameraGranted = perms[Manifest.permission.CAMERA] ?: false
                val locGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                if (locGranted) {
                    DeviceHardwareHelper.fetchDeviceGpsLocation(context, { lat, lng ->
                        // Real GPS acquired
                    })
                }
            }

            LaunchedEffect(Unit) {
                permissionLauncher.launch(permissionsToRequest)
                DeviceHardwareHelper.fetchDeviceGpsLocation(context, { lat, lng ->
                    // Live Location Updated
                })
            }

            // Toast feedback
            LaunchedEffect(uiState.toastMessage) {
                uiState.toastMessage?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearToast()
                }
            }

            // Persian RTL layout support
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MyApplicationTheme(appThemeMode = uiState.appTheme) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (uiState.authFlowStep == AuthFlowStep.AUTHENTICATED) {
                                TopHeaderBar(
                                    uiState = uiState,
                                    onLogout = { viewModel.logout() }
                                )
                            }
                        },
                        bottomBar = {
                            if (uiState.authFlowStep == AuthFlowStep.AUTHENTICATED) {
                                BottomNavBar(
                                    activeTab = uiState.activeTab,
                                    onTabSelected = { viewModel.setTab(it) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (uiState.authFlowStep != AuthFlowStep.AUTHENTICATED) {
                                AuthScreen(
                                    uiState = uiState,
                                    onSetTempRole = { viewModel.setTempAuthRole(it) },
                                    onSetStep = { viewModel.setAuthStep(it) },
                                    onLoginWithPassword = { phone, pass, captcha, exp ->
                                        viewModel.loginWithPassword(phone, pass, captcha, exp)
                                    },
                                    onVerifyOtp = { viewModel.verifyOtp(it) },
                                    onLoginWithBiometrics = {
                                        // Real Hardware Biometric Trigger
                                        DeviceHardwareHelper.authenticateWithBiometrics(
                                            activity = this@MainActivity,
                                            onSuccess = {
                                                viewModel.loginWithBiometrics()
                                            },
                                            onError = { err ->
                                                Toast.makeText(this@MainActivity, err, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    onSelectRoleAfterBiometric = { viewModel.selectActiveRoleAfterBiometric(it) },
                                    onRegisterUser = { viewModel.registerUser(it) },
                                    onRequestPasswordRecovery = { viewModel.requestPasswordRecovery(it) }
                                )
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    if (uiState.isLiveAlertVisible) {
                                        LiveUpdateBanner(text = uiState.liveAlertText)
                                    }

                                    when (uiState.activeTab) {
                                        NavigationTab.DASHBOARD -> {
                                            when (uiState.currentRole) {
                                                UserRole.CUSTOMER -> {
                                                    CustomerScreen(
                                                        uiState = uiState,
                                                        onSelectPackage = { viewModel.selectPackage(it) },
                                                        onOpenPaymentModal = { viewModel.openPaymentModal(it) },
                                                        onOpenManualTracking = { viewModel.openManualTrackingModal() },
                                                        onOpenChatModal = { viewModel.openChatModal(it) },
                                                        onOpenNavigationChoice = { viewModel.openNavigationChoice(it) },
                                                        onAcceptCourierRequest = { viewModel.acceptPackageByCustomer(it) },
                                                        onRejectCourierRequest = { viewModel.rejectPackageByCustomer(it) },
                                                        onOpenScanner = { viewModel.openScanner("customer_verify", null) }
                                                    )
                                                }
                                                UserRole.COURIER -> {
                                                    CourierScreen(
                                                        uiState = uiState,
                                                        onRegisterPackage = { title, receiver, phone, size, hubId ->
                                                            viewModel.registerNewPackageByCourier(title, receiver, phone, size, hubId)
                                                        },
                                                        onVerifyCustomerPackage = { viewModel.verifyAndAcceptPackageByCourier(it) },
                                                        onOpenScanner = { pkg ->
                                                            viewModel.openScanner("courier_register", pkg)
                                                        },
                                                        onOpenChatModal = { viewModel.openChatModal(it) },
                                                        onOpenNavigationChoice = { viewModel.openNavigationChoice(it) }
                                                    )
                                                }
                                                UserRole.HUB_MANAGER -> {
                                                    HubManagerScreen(
                                                        uiState = uiState,
                                                        onToggleHubOpen = { viewModel.toggleHubOpenStatus(it) },
                                                        onOpenScanner = { act, pkg ->
                                                            viewModel.openScanner(act, pkg)
                                                        },
                                                        onOpenPhotoModal = { viewModel.openPhotoModal(it) },
                                                        onDeliverPackage = { viewModel.deliverPackageToCustomer(it) },
                                                        onOpenChatModal = { viewModel.openChatModal(it) }
                                                    )
                                                }
                                                UserRole.ADMIN -> {
                                                    CustomerScreen(
                                                        uiState = uiState,
                                                        onSelectPackage = { viewModel.selectPackage(it) },
                                                        onOpenPaymentModal = { viewModel.openPaymentModal(it) },
                                                        onOpenManualTracking = { viewModel.openManualTrackingModal() },
                                                        onOpenChatModal = { viewModel.openChatModal(it) },
                                                        onOpenNavigationChoice = { viewModel.openNavigationChoice(it) },
                                                        onAcceptCourierRequest = { viewModel.acceptPackageByCustomer(it) },
                                                        onRejectCourierRequest = { viewModel.rejectPackageByCustomer(it) },
                                                        onOpenScanner = { viewModel.openScanner("customer_verify", null) }
                                                    )
                                                }
                                            }
                                        }

                                        NavigationTab.PACKAGES -> {
                                            PackagesScreen(
                                                uiState = uiState,
                                                onSelectPackage = { viewModel.selectPackage(it) },
                                                onOpenPaymentModal = { viewModel.openPaymentModal(it) },
                                                onOpenChatModal = { viewModel.openChatModal(it) },
                                                onOpenNavigationChoice = { viewModel.openNavigationChoice(it) },
                                                onOpenManualTracking = { viewModel.openManualTrackingModal() }
                                            )
                                        }

                                        NavigationTab.SCAN -> {
                                            LaunchedEffect(Unit) {
                                                viewModel.openScanner(
                                                    when (uiState.currentRole) {
                                                        UserRole.COURIER -> "courier_register"
                                                        UserRole.HUB_MANAGER -> "hub_receive"
                                                        UserRole.CUSTOMER, UserRole.ADMIN -> "customer_verify"
                                                    },
                                                    uiState.selectedPackage
                                                )
                                            }
                                            when (uiState.currentRole) {
                                                UserRole.CUSTOMER, UserRole.ADMIN -> CustomerScreen(
                                                    uiState = uiState,
                                                    onSelectPackage = { viewModel.selectPackage(it) },
                                                    onOpenPaymentModal = { viewModel.openPaymentModal(it) },
                                                    onOpenManualTracking = { viewModel.openManualTrackingModal() },
                                                    onOpenChatModal = { viewModel.openChatModal(it) },
                                                    onOpenNavigationChoice = { viewModel.openNavigationChoice(it) },
                                                    onAcceptCourierRequest = { viewModel.acceptPackageByCustomer(it) },
                                                    onRejectCourierRequest = { viewModel.rejectPackageByCustomer(it) },
                                                    onOpenScanner = { viewModel.openScanner("customer_verify", null) }
                                                )
                                                UserRole.COURIER -> CourierScreen(
                                                    uiState = uiState,
                                                    onRegisterPackage = { title, receiver, phone, size, hubId ->
                                                        viewModel.registerNewPackageByCourier(title, receiver, phone, size, hubId)
                                                    },
                                                    onVerifyCustomerPackage = { viewModel.verifyAndAcceptPackageByCourier(it) },
                                                    onOpenScanner = { pkg -> viewModel.openScanner("courier_register", pkg) },
                                                    onOpenChatModal = { viewModel.openChatModal(it) },
                                                    onOpenNavigationChoice = { viewModel.openNavigationChoice(it) }
                                                )
                                                UserRole.HUB_MANAGER -> HubManagerScreen(
                                                    uiState = uiState,
                                                    onToggleHubOpen = { viewModel.toggleHubOpenStatus(it) },
                                                    onOpenScanner = { act, pkg -> viewModel.openScanner(act, pkg) },
                                                    onOpenPhotoModal = { viewModel.openPhotoModal(it) },
                                                    onDeliverPackage = { viewModel.deliverPackageToCustomer(it) },
                                                    onOpenChatModal = { viewModel.openChatModal(it) }
                                                )
                                            }
                                        }

                                        NavigationTab.MAP -> {
                                            HubsMapScreen(
                                                uiState = uiState,
                                                onSelectHub = { viewModel.selectHub(it) },
                                                onOpenNavigationChoice = { viewModel.openNavigationChoice(it) }
                                            )
                                        }

                                        NavigationTab.SETTINGS, NavigationTab.AUTH -> {
                                            SettingsScreen(
                                                uiState = uiState,
                                                onToggleTheme = { viewModel.setTheme(it) },
                                                onOpenAdminPortal = { viewModel.openAdminPortal() },
                                                onSyncWithGoogleDrive = { viewModel.syncWithGoogleDrive() },
                                                onLogout = { viewModel.logout() },
                                                onRequestMobileChange = { viewModel.requestMobileChange(it) },
                                                onChangePassword = { current, new -> viewModel.changePassword(current, new) }
                                            )
                                        }
                                    }
                                }
                            }

                            // Modals & Dialogs
                            if (uiState.isScannerOpen) {
                                BarcodeScannerModal(
                                    actionType = uiState.scannerActionType,
                                    targetPackage = uiState.selectedPackage,
                                    availablePackages = uiState.packages,
                                    onDismiss = { viewModel.closeScanner() },
                                    onConfirmScan = { scannedCode ->
                                        when (uiState.scannerActionType) {
                                            "courier_register", "hub_receive" -> {
                                                viewModel.receivePackageAtHub(scannedCode)
                                            }
                                            "hub_deliver" -> {
                                                viewModel.deliverPackageToCustomer(scannedCode)
                                            }
                                            else -> {
                                                viewModel.receivePackageAtHub(scannedCode)
                                            }
                                        }
                                    }
                                )
                            }

                            if (uiState.isPhotoModalOpen) {
                                uiState.photoModalTargetPackage?.let { pkg ->
                                    PackagePhotoModal(
                                        targetPackage = pkg,
                                        onDismiss = { viewModel.closePhotoModal() },
                                        onConfirmPhoto = { photoUrl ->
                                            viewModel.receivePackageAtHub(pkg.id, photoUrl)
                                        }
                                    )
                                }
                            }

                            if (uiState.isManualTrackingModalOpen) {
                                ManualTrackingDialog(
                                    uiState = uiState,
                                    onDismiss = { viewModel.closeManualTrackingModal() },
                                    onSubmitManualTracking = { code, title, sender, courierId, hubId ->
                                        viewModel.registerManualPackageByCustomer(code, title, sender, courierId, hubId)
                                    }
                                )
                            }

                            if (uiState.isPaymentModalOpen) {
                                uiState.paymentTargetPackage?.let { pkg ->
                                    PaymentGatewayDialog(
                                        targetPackage = pkg,
                                        onDismiss = { viewModel.closePaymentModal() },
                                        onConfirmPayment = { viewModel.payPackageFee(it) }
                                    )
                                }
                            }

                            if (uiState.isChatModalOpen) {
                                uiState.activeChatPackage?.let { pkg ->
                                    PackageChatDialog(
                                        uiState = uiState,
                                        targetPackage = pkg,
                                        onDismiss = { viewModel.closeChatModal() },
                                        onSendMessage = { packageId, msg ->
                                            viewModel.sendInternalMessage(packageId, msg)
                                        }
                                    )
                                }
                            }

                            if (uiState.isNavigationChoiceOpen) {
                                uiState.navigationTargetHub?.let { hub ->
                                    NavigationAppDialog(
                                        targetHub = hub,
                                        onDismiss = { viewModel.closeNavigationChoice() }
                                    )
                                }
                            }

                            if (uiState.isAdminDialogOpen) {
                                AdminPortalDialog(
                                    uiState = uiState,
                                    onDismiss = { viewModel.closeAdminPortal() },
                                    onVerifyPin = { viewModel.verifyAdminPin(it) },
                                    onCreateAdmin = { name, email, phone, role, perms ->
                                        viewModel.createAdminUser(name, email, phone, role, perms)
                                    },
                                    onToggleAdminStatus = { viewModel.toggleAdminStatus(it) },
                                    onDeleteAdmin = { viewModel.deleteAdminUser(it) },
                                    onUpdateSystemContact = { viewModel.updateSystemContact(it) },
                                    onSyncWithGoogleDrive = { viewModel.syncWithGoogleDrive() },
                                    onToggleAutoSync = { viewModel.toggleAutoCloudSync() },
                                    onCreateUser = { fullName, username, password, mobile, nationalId, role, extra ->
                                        viewModel.createUserByAdmin(fullName, username, password, mobile, nationalId, role, extra)
                                    },
                                    onDeleteUser = { viewModel.deleteUserByAdmin(it) },
                                    onSwitchToUser = { viewModel.switchToUserAccount(it) },
                                    onApproveUser = { viewModel.approveUserByAdmin(it) },
                                    onRejectUser = { viewModel.rejectUserByAdmin(it) },
                                    onApproveMobileChange = { viewModel.approveMobileChange(it) },
                                    onRejectMobileChange = { id, reason -> viewModel.rejectMobileChange(id, reason) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
