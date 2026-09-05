package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MobileChangeRequest
import com.example.data.model.MobileChangeStatus
import com.example.model.AppThemeMode
import com.example.model.UserRole
import com.example.viewmodel.NdnUiState

@Composable
fun SettingsScreen(
    uiState: NdnUiState,
    onToggleTheme: (AppThemeMode) -> Unit,
    onOpenAdminPortal: () -> Unit,
    onSyncWithGoogleDrive: () -> Unit,
    onLogout: () -> Unit,
    onRequestMobileChange: (String) -> Unit = {},
    onChangePassword: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var isBiometricsEnabled by remember { mutableStateOf(uiState.currentUser.hasBiometricsEnabled) }
    var isNotificationsEnabled by remember { mutableStateOf(true) }
    var isSmsAlertsEnabled by remember { mutableStateOf(true) }
    var isChangePhoneDialogOpen by remember { mutableStateOf(false) }
    var isChangePasswordDialogOpen by remember { mutableStateOf(false) }
    var newPhoneInput by remember { mutableStateOf("") }

    val pendingMobileChange = uiState.mobileChangeRequests.firstOrNull {
        it.userId == uiState.currentUser.id && it.status == com.example.data.model.MobileChangeStatus.PENDING_ADMIN_APPROVAL
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
    ) {
        // Admin Portal Entry Banner — only rendered for accounts whose real database role
        // is ADMIN/SYSTEM_ADMIN. Authorization is still enforced server-side (repository layer)
        // regardless of this UI check.
        if (uiState.currentUser.role == UserRole.ADMIN) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAdminPortal() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ورود به پرتال ادمین و امنیت",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "همگام‌سازی ابری Google Drive و مانیتورینگ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ArrowBack, // RTL back arrow pointing left
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        }

        // Active Profile Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "مشخصات حساب کاربری فعال",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("نام و نام خانوادگی:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(uiState.currentUser.fullName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Text("کد ملی (یکتا و غیرقابل تغییر):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(uiState.currentUser.nationalId.ifBlank { "ثبت‌نشده" }, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("شماره تلفن همراه:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(uiState.currentUser.phone, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    if (pendingMobileChange != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "درخواست تغییر به ${pendingMobileChange.requestedPhone} در انتظار تایید مدیر سیستم است.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { isChangePhoneDialogOpen = true },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("درخواست تغییر شماره تلفن همراه", fontSize = 11.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("نقش کاربری:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(uiState.currentUser.role.titleFa, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedButton(
                        onClick = { isChangePasswordDialogOpen = true },
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تغییر رمز عبور", fontSize = 11.sp)
                    }
                }
            }
        }

        // Synchronization Status Section (Minimal, Honest Status)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = "وضعیت همگام‌سازی",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "وضعیت همگام‌سازی",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "آخرین همگام‌سازی: ${uiState.driveSyncState.lastSyncTimestamp.ifBlank { "همین حالا" }}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = if (uiState.isOnline) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (uiState.isOnline) "پایدار" else "محلی",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isOnline) Color(0xFF047857) else Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Admin Portal Entry (Role-Gated for System Admin / Admin)
        if (uiState.currentRole == UserRole.ADMIN) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column {
                                    Text("پرتال مدیریت ارشد سیستم", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("مدیریت کاربران، تراکنش‌ها، هاب‌ها و گزارشات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Button(
                            onClick = onOpenAdminPortal,
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ورود به پنل مدیریت ارشد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Theme Switcher Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "انتخاب تم و جلوه بصری",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Luxury Theme
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleTheme(AppThemeMode.LUXURY) },
                            color = if (uiState.appTheme == AppThemeMode.LUXURY) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (uiState.appTheme == AppThemeMode.LUXURY) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("مشکی و طلایی (Obsidian & Gold)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Classic Theme
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleTheme(AppThemeMode.CLASSIC) },
                            color = if (uiState.appTheme == AppThemeMode.CLASSIC) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (uiState.appTheme == AppThemeMode.CLASSIC) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("آبی سازمانی پست", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Security & Biometrics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "امنیت و ورود دو مرحله‌ای",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ورود سریع با اثر انگشت / بایومتریک", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("امکان ورود به پنل بدون رمز عبور", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isBiometricsEnabled,
                            onCheckedChange = { isBiometricsEnabled = it }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("پیامک‌های اطلاع‌رسانی ورود و بسته‌ها", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("ارسال پیامک با تغییر وضعیت بسته در هاب", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isSmsAlertsEnabled,
                            onCheckedChange = { isSmsAlertsEnabled = it }
                        )
                    }
                }
            }
        }

        // Logout Button
        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("خروج از حساب کاربری", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (isChangePhoneDialogOpen) {
        AlertDialog(
            onDismissRequest = { isChangePhoneDialogOpen = false },
            title = {
                Text(
                    text = "درخواست تغییر شماره تلفن همراه",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "توجه: کد ملی (${uiState.currentUser.nationalId.ifBlank { "ثبت‌نشده" }}) غیرقابل تغییر است. شماره جدید پس از بررسی و تایید توسط مدیر سیستم فعال خواهد شد.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newPhoneInput,
                        onValueChange = { newPhoneInput = it },
                        label = { Text("شماره همراه جدید (۱۱ رقم)") },
                        placeholder = { Text("مثال: 09121112233") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPhoneInput.isNotBlank()) {
                            onRequestMobileChange(newPhoneInput)
                            newPhoneInput = ""
                            isChangePhoneDialogOpen = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ثبت درخواست تغییر")
                }
            },
            dismissButton = {
                TextButton(onClick = { isChangePhoneDialogOpen = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (isChangePasswordDialogOpen) {
        var currentPasswordInput by remember { mutableStateOf("") }
        var newPasswordInput by remember { mutableStateOf("") }
        var confirmPasswordInput by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { isChangePasswordDialogOpen = false },
            title = {
                Text(text = "تغییر رمز عبور", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = currentPasswordInput,
                        onValueChange = { currentPasswordInput = it; errorText = null },
                        label = { Text("رمز عبور فعلی") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it; errorText = null },
                        label = { Text("رمز عبور جدید (حداقل ۶ کاراکتر)") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = { confirmPasswordInput = it; errorText = null },
                        label = { Text("تکرار رمز عبور جدید") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val err = errorText
                    if (err != null) {
                        Text(err, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            newPasswordInput.length < 6 -> errorText = "رمز عبور جدید باید حداقل ۶ کاراکتر باشد."
                            newPasswordInput != confirmPasswordInput -> errorText = "تکرار رمز عبور جدید مطابقت ندارد."
                            else -> {
                                onChangePassword(currentPasswordInput, newPasswordInput)
                                isChangePasswordDialogOpen = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ثبت رمز عبور جدید")
                }
            },
            dismissButton = {
                TextButton(onClick = { isChangePasswordDialogOpen = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}
