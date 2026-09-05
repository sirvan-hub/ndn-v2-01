package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MobileChangeRequest
import com.example.data.model.MobileChangeStatus
import com.example.model.*
import com.example.viewmodel.NdnUiState
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalDialog(
    uiState: NdnUiState,
    onDismiss: () -> Unit,
    onVerifyPin: (String) -> Unit,
    onCreateAdmin: (String, String, String, AdminRoleLevel, List<AdminPermission>) -> Unit,
    onToggleAdminStatus: (String) -> Unit,
    onDeleteAdmin: (String) -> Unit,
    onUpdateSystemContact: (SystemAdminContact) -> Unit,
    onSyncWithGoogleDrive: () -> Unit,
    onToggleAutoSync: () -> Unit,
    onCreateUser: (fullName: String, username: String, password: String, mobile: String, nationalId: String, role: UserRole, extra: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteUser: (String) -> Unit = {},
    onSwitchToUser: (UserProfile) -> Unit = {},
    onApproveUser: (String) -> Unit = {},
    onRejectUser: (String) -> Unit = {},
    onApproveMobileChange: (String) -> Unit = {},
    onRejectMobileChange: (String, String) -> Unit = { _, _ -> }
) {
    var pinInput by remember { mutableStateOf("") }
    var activeAdminTab by remember { mutableStateOf(0) } // 0: Google Drive Sync, 1: Roles & Users, 2: Finances, 3: Saturation, 4: RBAC, 5: Contacts
    var isCreateAdminOpen by remember { mutableStateOf(false) }

    // Form states for creating admin
    var newAdminName by remember { mutableStateOf("") }
    var newAdminEmail by remember { mutableStateOf("") }
    var newAdminPhone by remember { mutableStateOf("") }
    var newAdminRoleLevel by remember { mutableStateOf(AdminRoleLevel.OPS_MANAGER) }
    var selectedPermissions by remember { mutableStateOf(setOf(AdminPermission.APPROVE_HUBS, AdminPermission.MANAGE_COURIERS)) }

    // Form states for creating general user
    var isAddUserExpanded by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf("") }
    var newUserUsername by remember { mutableStateOf("") }
    var newUserPassword by remember { mutableStateOf("") }
    var newUserPhone by remember { mutableStateOf("") }
    var newUserNationalId by remember { mutableStateOf("") }
    var newUserEmail by remember { mutableStateOf("") }
    var newUserAddress by remember { mutableStateOf("") }
    var newUserRole by remember { mutableStateOf(UserRole.CUSTOMER) }
    var newUserExtra by remember { mutableStateOf("") }

    // Recovery contacts form state
    var primaryPhone by remember { mutableStateOf(uiState.systemAdminContact.primaryPhone) }
    var emergencyPhone by remember { mutableStateOf(uiState.systemAdminContact.emergencyPhone) }
    var primaryEmail by remember { mutableStateOf(uiState.systemAdminContact.primaryEmail) }
    var supportEmail by remember { mutableStateOf(uiState.systemAdminContact.supportEmail) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ndn_logo_1787134518636),
                                contentDescription = "NDN Logo",
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "پرتال نظارت و امنیت NDN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "V2.01.0",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "مدیر ارشد پلتفرم: Reza Gh (09123407615 - تهران، باغ فیض)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                if (!uiState.isAdminAuthenticated) {
                    // PIN / Password Authentication Challenge
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.95f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )

                                Text(
                                    text = "احراز هویت مدیر ارشد سیستم (Reza Gh)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "جهت ورود به پنل نظارت و تعریف کاربران، رمز عبور یا پین امنیتی مدیر را وارد نمایید.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )

                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = { pinInput = it },
                                    label = { Text("رمز عبور یا پین امنیتی") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = { onVerifyPin(pinInput) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ورود به پنل ادمین", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                } else {
                    // Authenticated Admin Dashboard Tabs
                    ScrollableTabRow(
                        selectedTabIndex = activeAdminTab,
                        edgePadding = 0.dp,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        Tab(
                            selected = activeAdminTab == 0,
                            onClick = { activeAdminTab = 0 },
                            text = { Text("همگام‌سازی Drive", fontSize = 12.sp, fontWeight = if (activeAdminTab == 0) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = activeAdminTab == 1,
                            onClick = { activeAdminTab = 1 },
                            text = { Text("مدیریت کاربران و نقش‌ها", fontSize = 12.sp, fontWeight = if (activeAdminTab == 1) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = activeAdminTab == 2,
                            onClick = { activeAdminTab = 2 },
                            text = { Text("تسهیم مالی V1.01", fontSize = 12.sp, fontWeight = if (activeAdminTab == 2) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = activeAdminTab == 3,
                            onClick = { activeAdminTab = 3 },
                            text = { Text("اشباع منطقه‌ای", fontSize = 12.sp, fontWeight = if (activeAdminTab == 3) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = activeAdminTab == 4,
                            onClick = { activeAdminTab = 4 },
                            text = { Text("سطوح دسترسی RBAC", fontSize = 12.sp, fontWeight = if (activeAdminTab == 4) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = activeAdminTab == 5,
                            onClick = { activeAdminTab = 5 },
                            text = { Text("تماس اضطراری", fontSize = 12.sp, fontWeight = if (activeAdminTab == 5) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (activeAdminTab) {
                            0 -> GoogleDriveSyncTab(
                                uiState = uiState,
                                onSyncNow = onSyncWithGoogleDrive,
                                onToggleAutoSync = onToggleAutoSync
                            )
                            1 -> RoleAndUserManagementTab(
                                uiState = uiState,
                                isAddUserExpanded = isAddUserExpanded,
                                onToggleAddUser = { isAddUserExpanded = it },
                                newUserName = newUserName,
                                onNameChange = { newUserName = it },
                                newUserUsername = newUserUsername,
                                onUsernameChange = { newUserUsername = it },
                                newUserPassword = newUserPassword,
                                onPasswordChange = { newUserPassword = it },
                                newUserPhone = newUserPhone,
                                onPhoneChange = { newUserPhone = it },
                                newUserNationalId = newUserNationalId,
                                onNationalIdChange = { newUserNationalId = it },
                                newUserEmail = newUserEmail,
                                onEmailChange = { newUserEmail = it },
                                newUserAddress = newUserAddress,
                                onAddressChange = { newUserAddress = it },
                                newUserRole = newUserRole,
                                onRoleSelected = { newUserRole = it },
                                newUserExtra = newUserExtra,
                                onExtraChange = { newUserExtra = it },
                                onCreateUser = {
                                    onCreateUser(newUserName, newUserUsername, newUserPassword, newUserPhone, newUserNationalId, newUserRole, newUserExtra)
                                    newUserName = ""
                                    newUserUsername = ""
                                    newUserPassword = ""
                                    newUserPhone = ""
                                    newUserNationalId = ""
                                    newUserEmail = ""
                                    newUserAddress = ""
                                    newUserExtra = ""
                                    isAddUserExpanded = false
                                },
                                onDeleteUser = onDeleteUser,
                                onSwitchToUser = onSwitchToUser,
                                onApproveUser = onApproveUser,
                                onRejectUser = onRejectUser,
                                onApproveMobileChange = onApproveMobileChange,
                                onRejectMobileChange = onRejectMobileChange
                            )
                            2 -> FinanceSplitTab(uiState = uiState)
                            3 -> RegionalSaturationTab(uiState = uiState)
                            4 -> RbacAdminTab(
                                uiState = uiState,
                                isCreateAdminOpen = isCreateAdminOpen,
                                onToggleCreateModal = { isCreateAdminOpen = it },
                                newAdminName = newAdminName,
                                onNameChange = { newAdminName = it },
                                newAdminEmail = newAdminEmail,
                                onEmailChange = { newAdminEmail = it },
                                newAdminPhone = newAdminPhone,
                                onPhoneChange = { newAdminPhone = it },
                                newAdminRoleLevel = newAdminRoleLevel,
                                onRoleLevelChange = { newAdminRoleLevel = it },
                                selectedPermissions = selectedPermissions,
                                onTogglePermission = { perm ->
                                    selectedPermissions = if (selectedPermissions.contains(perm)) {
                                        selectedPermissions - perm
                                    } else {
                                        selectedPermissions + perm
                                    }
                                },
                                onCreateAdmin = {
                                    onCreateAdmin(newAdminName, newAdminEmail, newAdminPhone, newAdminRoleLevel, selectedPermissions.toList())
                                    newAdminName = ""
                                    newAdminEmail = ""
                                    newAdminPhone = ""
                                    isCreateAdminOpen = false
                                },
                                onToggleAdminStatus = onToggleAdminStatus,
                                onDeleteAdmin = onDeleteAdmin
                            )
                            5 -> EmergencyContactsTab(
                                primaryPhone = primaryPhone,
                                onPrimaryPhoneChange = { primaryPhone = it },
                                emergencyPhone = emergencyPhone,
                                onEmergencyPhoneChange = { emergencyPhone = it },
                                primaryEmail = primaryEmail,
                                onPrimaryEmailChange = { primaryEmail = it },
                                supportEmail = supportEmail,
                                onSupportEmailChange = { supportEmail = it },
                                onSave = {
                                    onUpdateSystemContact(
                                        SystemAdminContact(
                                            primaryPhone = primaryPhone,
                                            emergencyPhone = emergencyPhone,
                                            primaryEmail = primaryEmail,
                                            supportEmail = supportEmail
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleAndUserManagementTab(
    uiState: NdnUiState,
    isAddUserExpanded: Boolean,
    onToggleAddUser: (Boolean) -> Unit,
    newUserName: String,
    onNameChange: (String) -> Unit,
    newUserUsername: String,
    onUsernameChange: (String) -> Unit,
    newUserPassword: String,
    onPasswordChange: (String) -> Unit,
    newUserPhone: String,
    onPhoneChange: (String) -> Unit,
    newUserNationalId: String,
    onNationalIdChange: (String) -> Unit,
    newUserEmail: String,
    onEmailChange: (String) -> Unit,
    newUserAddress: String,
    onAddressChange: (String) -> Unit,
    newUserRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    newUserExtra: String,
    onExtraChange: (String) -> Unit,
    onCreateUser: () -> Unit,
    onDeleteUser: (String) -> Unit,
    onSwitchToUser: (UserProfile) -> Unit,
    onApproveUser: (String) -> Unit,
    onRejectUser: (String) -> Unit,
    onApproveMobileChange: (String) -> Unit,
    onRejectMobileChange: (String, String) -> Unit
) {
    val pendingUsers = uiState.allUsers.filter { it.approvalStatus == AccountApprovalStatus.PENDING }
    val approvedUsers = uiState.allUsers.filter { it.approvalStatus == AccountApprovalStatus.APPROVED }
    val pendingMobileChanges = uiState.mobileChangeRequests.filter { it.status == com.example.data.model.MobileChangeStatus.PENDING_ADMIN_APPROVAL }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Admin profile identity banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "مدیر ارشد سیستم (Super Admin): Reza Gh",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "user name: Admin",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "شماره تماس مستقیم: 09123407615", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "ایمیل هماهنگی: reza.gh@ndn-pudo.ir", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Text(
                        text = "آدرس پستی: تهران، باغ فیض",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "احراز هویت دومرحله‌ای و ممیزی نظارتی فعال است",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        // Section 1: Pending User Approvals
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.PendingActions,
                        contentDescription = null,
                        tint = if (pendingUsers.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "کاربران در نوبت تایید مدیر سیستم (${pendingUsers.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                if (pendingUsers.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "نیاز به بررسی و تایید",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        if (pendingUsers.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "تمامی درخواست‌های عضویت بررسی شده‌اند و کاربر منتظر تاییدی وجود ندارد.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(pendingUsers) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(text = user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = "درخواست نقش: ${user.role.titleFa}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "تاریخ ثبت: ${user.registrationDate}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "شماره همراه: ${user.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            if (user.postalCode.isNotBlank()) {
                                Text(text = "کد پستی: ${user.postalCode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (user.storeName.isNotBlank() || user.guildType.isNotBlank()) {
                            Text(
                                text = "نام فروشگاه / صنف: ${user.storeName} (${user.guildType})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (user.address.isNotBlank()) {
                            Text(
                                text = "آدرس: ${user.address}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (user.bankCardNumber.isNotBlank()) {
                            Text(
                                text = "شماره شبا / کارت: ${user.bankCardNumber}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Action Buttons: Approve / Reject
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onApproveUser(user.id) },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(42.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تایید و فعال‌سازی ورود", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { onRejectUser(user.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("رد درخواست", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section 1.5: Pending Mobile Number Change Requests
        if (pendingMobileChanges.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.PhoneIphone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "درخواست‌های تغییر شماره همراه (${pendingMobileChanges.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "مستلزم تایید مدیر",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            items(pendingMobileChanges, key = { "mcr-${it.id}" }) { req ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = req.userFullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "کد ملی: ${req.nationalId.ifBlank { "ثبت‌نشده" }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "شماره فعلی: ${req.currentPhone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "شماره جدید درخواستی: ${req.requestedPhone}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onApproveMobileChange(req.id) },
                                modifier = Modifier.weight(1.5f).height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تایید شماره جدید", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { onRejectMobileChange(req.id, "عدم احراز هویت") },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("رد درخواست", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Action to Add New User with Role
        item {
            Button(
                onClick = { onToggleAddUser(!isAddUserExpanded) },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (isAddUserExpanded) Icons.Default.ExpandLess else Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAddUserExpanded) "بستن فرم تعریف کاربر" else "تعریف و افزودن کاربر جدید به سیستم",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Inline Form to Define New User
        if (isAddUserExpanded) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "مشخصات کاربر و نقش جدید:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = newUserName,
                            onValueChange = onNameChange,
                            label = { Text("نام و نام خانوادگی") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = newUserUsername,
                            onValueChange = onUsernameChange,
                            label = { Text("نام کاربری (Username) — یکتا") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = newUserPassword,
                            onValueChange = onPasswordChange,
                            label = { Text("رمز عبور (حداقل ۶ کاراکتر)") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = newUserPhone,
                            onValueChange = onPhoneChange,
                            label = { Text("شماره همراه (مانند 0912...) — یکتا") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = newUserNationalId,
                            onValueChange = onNationalIdChange,
                            label = { Text("کد ملی — یکتا") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = newUserEmail,
                            onValueChange = onEmailChange,
                            label = { Text("ایمیل (اختیاری)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Text("انتخاب نقش کاربر / سطح دسترسی:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            UserRole.values().forEach { role ->
                                val isSelected = newUserRole == role
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onRoleSelected(role) },
                                    label = { Text(role.titleFa, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = newUserAddress,
                            onValueChange = onAddressChange,
                            label = { Text(if (newUserRole == UserRole.COURIER) "محدوده پوشش فعالیت" else if (newUserRole == UserRole.HUB_MANAGER) "آدرس دقیق فروشگاه / هاب" else "آدرس پستی مشتری") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        if (newUserRole == UserRole.HUB_MANAGER || newUserRole == UserRole.COURIER) {
                            OutlinedTextField(
                                value = newUserExtra,
                                onValueChange = onExtraChange,
                                label = { Text(if (newUserRole == UserRole.HUB_MANAGER) "نام فروشگاه / هاب (مثلاً سوپرمارکت یاران)" else "وسیله نقلیه / منطقه") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        val isFormValid = newUserName.isNotBlank() && newUserUsername.isNotBlank() &&
                            newUserPassword.length >= 6 && newUserPhone.isNotBlank() && newUserNationalId.isNotBlank()

                        if (!isFormValid) {
                            Text(
                                text = "نام، نام کاربری، رمز عبور (حداقل ۶ کاراکتر)، موبایل و کد ملی الزامی هستند.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Button(
                            onClick = onCreateUser,
                            enabled = isFormValid,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ثبت و تایید کاربر در سیستم", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }

        // NOTE: The "Active Role Quick Switch" feature was intentionally removed.
        // A logged-in user's role must come exclusively from their authenticated database
        // account (currentUser.role) — it must never be changed locally by any UI control.

        // Section: Defined Users in the system
        item {
            Text(
                text = "کاربران تعریف‌شده در سیستم (${uiState.allUsers.size} کاربر):",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(uiState.allUsers) { user ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(text = user.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "${user.role.titleFa} • ${user.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            if (user.address.isNotBlank()) {
                                Text(text = user.address, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onSwitchToUser(user) }) {
                            Text("ورود", fontSize = 11.sp)
                        }
                        if (user.id != "user-reza-admin") {
                            IconButton(onClick = { onDeleteUser(user.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleDriveSyncTab(
    uiState: NdnUiState,
    onSyncNow: () -> Unit,
    onToggleAutoSync: () -> Unit
) {
    val syncState = uiState.driveSyncState
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Text(
                                text = "معماری ذخیره‌سازی سرورلس Google Drive",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Serverless Cloud DB",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "تمامی بسته‌ها، هاب‌ها، کاربران و سوابق در فایل اختصاصی ${syncState.driveFileName} حساب ابری شما ذخیره و همگام می‌شوند.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "آخرین همگام‌سازی:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = syncState.lastSyncTimestamp, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column {
                            Text(text = "تعداد عملیات همگام:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${syncState.totalSyncOperations} بار", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "همگام‌سازی خودکار در هر تغییر:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = syncState.isAutoSyncEnabled,
                            onCheckedChange = { onToggleAutoSync() }
                        )
                    }

                    Button(
                        onClick = onSyncNow,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("همگام‌سازی فوری با Google Drive", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceSplitTab(uiState: NdnUiState) {
    val totalRevenue = uiState.packages.filter { it.isPaid }.sumOf { it.baseFee }
    val courierPool = (totalRevenue * 0.30).toLong()
    val hubPool = (totalRevenue * 0.30).toLong()
    val systemPool = totalRevenue - courierPool - hubPool

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "مدل اقتصادی سامانه NDN (تسهیم درآمد V1.01):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FinanceRow("سهم سفیران PUDO", courierPool, "جمع‌آوری از مبدا و تحویل")
                            FinanceRow("سهم هاب‌های محلی", hubPool, "انبارداری موقت و تحویل به مشتری")
                            FinanceRow("سهم سیستم و پلتفرم NDN", systemPool, "مدیریت، زیرساخت و سرورلس ابری")
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("کل گردش مالی شبکه:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("${NumberFormat.getNumberInstance(Locale.US).format(totalRevenue)} ریال", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceRow(title: String, amount: Long, desc: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = "${NumberFormat.getNumberInstance(Locale.US).format(amount)} ریال", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RegionalSaturationTab(uiState: NdnUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        items(uiState.regionalSaturations) { zone ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = zone.regionName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Surface(
                            color = if (zone.saturationPercent > 80) Color(0x33EF4444) else Color(0x3310B981),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = zone.bottleneckStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (zone.saturationPercent > 80) Color(0xFFEF4444) else Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "هاب‌ها: ${zone.hubCount} • سفیران: ${zone.courierCount} • بسته‌های فعال: ${zone.activePackages}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${zone.saturationPercent}٪ اشباع", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    LinearProgressIndicator(
                        progress = { zone.saturationPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (zone.saturationPercent > 80) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}

@Composable
private fun RbacAdminTab(
    uiState: NdnUiState,
    isCreateAdminOpen: Boolean,
    onToggleCreateModal: (Boolean) -> Unit,
    newAdminName: String,
    onNameChange: (String) -> Unit,
    newAdminEmail: String,
    onEmailChange: (String) -> Unit,
    newAdminPhone: String,
    onPhoneChange: (String) -> Unit,
    newAdminRoleLevel: AdminRoleLevel,
    onRoleLevelChange: (AdminRoleLevel) -> Unit,
    selectedPermissions: Set<AdminPermission>,
    onTogglePermission: (AdminPermission) -> Unit,
    onCreateAdmin: () -> Unit,
    onToggleAdminStatus: (String) -> Unit,
    onDeleteAdmin: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        item {
            Button(
                onClick = { onToggleCreateModal(true) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("افزودن کاربر مدیر / ادمین جدید", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        items(uiState.adminUsers) { admin ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(text = admin.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "${admin.roleTitle} • ${admin.email}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Switch(checked = admin.isActive, onCheckedChange = { onToggleAdminStatus(admin.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyContactsTab(
    primaryPhone: String,
    onPrimaryPhoneChange: (String) -> Unit,
    emergencyPhone: String,
    onEmergencyPhoneChange: (String) -> Unit,
    primaryEmail: String,
    onPrimaryEmailChange: (String) -> Unit,
    supportEmail: String,
    onSupportEmailChange: (String) -> Unit,
    onSave: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        item {
            OutlinedTextField(
                value = primaryPhone,
                onValueChange = onPrimaryPhoneChange,
                label = { Text("شماره تماس مستقیم دیسپچ") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }
        item {
            OutlinedTextField(
                value = emergencyPhone,
                onValueChange = onEmergencyPhoneChange,
                label = { Text("تلفن کشیک اضطراری") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }
        item {
            OutlinedTextField(
                value = primaryEmail,
                onValueChange = onPrimaryEmailChange,
                label = { Text("ایمیل اصلی امنیتی") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }
        item {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("ذخیره تغییرات تماس اضطراری", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
