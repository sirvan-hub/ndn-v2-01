package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuthFlowStep
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.theme.DangerRed
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.PostOrange
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.NdnUiState
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    uiState: NdnUiState,
    onSetTempRole: (UserRole) -> Unit,
    onSetStep: (AuthFlowStep) -> Unit,
    onLoginWithPassword: (String, String, String, String) -> Unit,
    onVerifyOtp: (String) -> Unit,
    onLoginWithBiometrics: () -> Unit,
    onSelectRoleAfterBiometric: (UserRole) -> Unit = {},
    onRegisterUser: (UserProfile) -> Unit,
    onRequestPasswordRecovery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Form fields
    var phoneInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var captchaInput by remember { mutableStateOf("") }
    var captchaCode by remember { mutableStateOf("7492") }
    var otpInput by remember { mutableStateOf("") }

    // Registration specific fields
    var regFullName by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regNationalId by remember { mutableStateOf("") }
    var regPostalCode by remember { mutableStateOf("") }
    var regAddress by remember { mutableStateOf("") }
    var regStoreName by remember { mutableStateOf("") }
    var regGuildType by remember { mutableStateOf("") }
    var regBankCard by remember { mutableStateOf("") }
    var regWorkingHours by remember { mutableStateOf("۰۸:۰۰ الی ۲۲:۰۰") }
    var regLandline by remember { mutableStateOf("") }
    var regServicesDesc by remember { mutableStateOf("") }
    var regPostalDistrict by remember { mutableStateOf("منطقه ۲ پستی تهران") }
    var regCoverageRadius by remember { mutableStateOf("8") }

    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with App Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ndn_logo_1787134518636),
                        contentDescription = "لوگوی NDN",
                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(11.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                Column {
                    Text(
                        text = "سامانه پستی محله (NDN)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ورود و ثبت‌نام در درگاه یکپارچه خدمات پستی",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Step Content
            when (uiState.authFlowStep) {
                AuthFlowStep.PANEL_SELECT -> {
                    Text(
                        text = "لطفاً نقش کاربری خود را انتخاب نمایید:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // All 4 user panels and roles are selectable
                    UserRole.values().forEach { role ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSetTempRole(role)
                                    onSetStep(AuthFlowStep.AUTH_ACTION_SELECT)
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
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
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (role) {
                                                UserRole.CUSTOMER -> Icons.Default.AccountCircle
                                                UserRole.COURIER -> Icons.Default.ElectricScooter
                                                UserRole.HUB_MANAGER -> Icons.Default.Storefront
                                                UserRole.ADMIN -> Icons.Default.Security
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column {
                                        Text(role.titleFa, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(role.subtitleFa, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Icon(Icons.Default.ArrowBackIosNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Biometric Quick Login Button
                    Button(
                        onClick = { onLoginWithBiometrics() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ورود سریع با اثر انگشت / بایومتریک", fontWeight = FontWeight.Bold)
                    }
                }

                AuthFlowStep.AUTH_ACTION_SELECT -> {
                    Text(
                        text = "پنل انتخابی: ${uiState.tempAuthRole.titleFa}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = { onSetStep(AuthFlowStep.LOGIN_FORM) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ورود به حساب کاربری موجود", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onSetStep(AuthFlowStep.REGISTER_FORM) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ثبت‌نام و عضویت جدید", fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { onSetStep(AuthFlowStep.PANEL_SELECT) }) {
                        Text("بازگشت به انتخاب نقش")
                    }
                }

                AuthFlowStep.LOGIN_FORM -> {
                    Text(
                        text = "ورود به حساب کاربری",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("نام کاربری یا شماره تلفن همراه") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("کلمه عبور") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Visual Captcha
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = captchaInput,
                            onValueChange = { captchaInput = it },
                            label = { Text("کد امنیتی") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    captchaCode = ((1000..9999).random()).toString()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = captchaCode,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 4.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onLoginWithPassword(phoneInput, passwordInput, captchaInput, captchaCode)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ورود به سامانه", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onLoginWithBiometrics() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ورود با اثر انگشت / بایومتریک", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { onSetStep(AuthFlowStep.PASSWORD_RECOVERY) }) {
                            Text("فراموشی رمز عبور؟", fontSize = 12.sp)
                        }
                        TextButton(onClick = { onSetStep(AuthFlowStep.AUTH_ACTION_SELECT) }) {
                            Text("بازگشت", fontSize = 12.sp)
                        }
                    }
                }

                AuthFlowStep.TWO_FACTOR_FORM -> {
                    Text(
                        text = "احراز هویت دو مرحله‌ای (۲FA)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Text(
                        text = "کد تایید ۴ رقمی ارسال شده به شماره $phoneInput را وارد فرمایید:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { if (it.length <= 4) otpInput = it },
                        label = { Text("کد ۴ رقمی تایید پیامکی (OTP)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = { onVerifyOtp(otpInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تایید نهایی و ورود به سامانه", fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { onSetStep(AuthFlowStep.LOGIN_FORM) }) {
                        Text("ویرایش شماره همراه")
                    }
                }

                AuthFlowStep.REGISTER_FORM -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "فرم ثبت‌نام ${uiState.tempAuthRole.titleFa}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        OutlinedTextField(
                            value = regFullName,
                            onValueChange = { regFullName = it },
                            label = { Text("نام و نام خانوادگی") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = regUsername,
                            onValueChange = { regUsername = it },
                            label = { Text("نام کاربری (Username) — یکتا") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it },
                            label = { Text("رمز عبور (حداقل ۶ کاراکتر)") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = { regPhone = it },
                            label = { Text("شماره همراه") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = regNationalId,
                            onValueChange = { regNationalId = it },
                            label = { Text("کد ملی ۱۰ رقمی (شناسه هویتی یکتا)") },
                            placeholder = { Text("مثال: ۰۰۱۲۳۴۵۶۷۸") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (uiState.tempAuthRole == UserRole.CUSTOMER) {
                            OutlinedTextField(
                                value = regPostalCode,
                                onValueChange = { regPostalCode = it },
                                label = { Text("کد پستی ۱۰ رقمی") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = regAddress,
                                onValueChange = { regAddress = it },
                                label = { Text("آدرس دقیق محل سکونت") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (uiState.tempAuthRole == UserRole.HUB_MANAGER) {
                            OutlinedTextField(
                                value = regStoreName,
                                onValueChange = { regStoreName = it },
                                label = { Text("نام فروشگاه یا هاب محلی") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = regGuildType,
                                onValueChange = { regGuildType = it },
                                label = { Text("نوع صنف (سوپرمارکت، کتابفروشی و...)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = regBankCard,
                                onValueChange = { regBankCard = it },
                                label = { Text("شماره کارت بانکی جهت تسویه کارمزد") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else if (uiState.tempAuthRole == UserRole.COURIER) {
                            OutlinedTextField(
                                value = regPostalDistrict,
                                onValueChange = { regPostalDistrict = it },
                                label = { Text("منطقه پستی فعالیت") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = regBankCard,
                                onValueChange = { regBankCard = it },
                                label = { Text("شماره کارت بانکی واریز دستمزد") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        if (regUsername.isBlank() || regPassword.length < 6) {
                            Text(
                                text = "نام کاربری و رمز عبور (حداقل ۶ کاراکتر) برای امکان ورود بعدی الزامی است.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Button(
                            onClick = {
                                val newProfile = UserProfile(
                                    id = "user-${UUID.randomUUID().toString().take(6)}",
                                    role = uiState.tempAuthRole,
                                    fullName = regFullName.ifBlank { "کاربر جدید" },
                                    username = regUsername,
                                    password = regPassword,
                                    phone = regPhone.ifBlank { "09120000000" },
                                    nationalId = regNationalId,
                                    postalCode = regPostalCode,
                                    address = regAddress,
                                    storeName = regStoreName,
                                    guildType = regGuildType,
                                    bankCardNumber = regBankCard
                                )
                                onRegisterUser(newProfile)
                            },
                            enabled = regUsername.isNotBlank() && regPassword.length >= 6 && regFullName.isNotBlank() && regPhone.isNotBlank() && regNationalId.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تکمیل ثبت نام و ایجاد پنل", fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { onSetStep(AuthFlowStep.AUTH_ACTION_SELECT) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("انصراف و بازگشت")
                        }
                    }
                }

                AuthFlowStep.PASSWORD_RECOVERY -> {
                    Text(
                        text = "درخواست بازیابی رمز عبور",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Text(
                        text = "جهت بازیابی رمز عبور، شماره همراه خود را وارد کنید. کد احراز هویت موقت از طریق سیستم و مدیر پلتفرم ارسال خواهد شد.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("شماره همراه ثبت شده") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            onRequestPasswordRecovery(phoneInput)
                            onSetStep(AuthFlowStep.LOGIN_FORM)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ارسال درخواست به مدیر سیستم", fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { onSetStep(AuthFlowStep.LOGIN_FORM) }) {
                        Text("بازگشت به صفحه ورود")
                    }
                }

                AuthFlowStep.REGISTRATION_PENDING_APPROVAL -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(LuxuryGold.copy(alpha = 0.2f))
                                .border(2.dp, LuxuryGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = LuxuryGold,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = "درخواست عضویت در نوبت تایید مدیر سیستم",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "اطلاعات هویتی شما در پایگاه داده ابری سامانه با موفقیت ثبت شد. جهت امنیت شبکه، فعال‌سازی حساب پس از بررسی و تایید مدیر ارشد (Reza Gh) انجام می‌گردد.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "نام کاربر: ${uiState.currentUser.fullName.ifBlank { "کاربر جدید" }}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        color = LuxuryGold.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = uiState.currentUser.role.titleFa,
                                            color = LuxuryGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "شماره همراه: ${uiState.currentUser.phone.ifBlank { "ثبت شده" }}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "وضعیت حساب:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                        color = PostOrange.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "⏳ در انتظار تایید ادمین",
                                            color = PostOrange,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { onSetStep(AuthFlowStep.LOGIN_FORM) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ورود به حساب کاربری پس از تایید", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onSetStep(AuthFlowStep.PANEL_SELECT) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("بازگشت به صفحه انتخاب نقش")
                        }
                    }
                }

                AuthFlowStep.ROLE_SELECTOR_POST_BIOMETRIC -> {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                            Text(
                                text = "احراز هویت بیومتریک با موفقیت تایید شد",
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Text(
                        text = "انتخاب نقش فعال نشست کاربری:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "برای امنیت حساب، فقط نقش واقعی ثبت‌شده برای این حساب قابل انتخاب است:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // SECURITY: only the account's own real role is offered. Showing every role
                    // here (as before) invited tapping ADMIN after a non-admin biometric unlock;
                    // the ViewModel also independently rejects any role other than the account's
                    // real one, so this list is UI-level clarity, not the authorization boundary.
                    listOf(uiState.currentUser.role).forEach { role ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectRoleAfterBiometric(role)
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
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
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (role) {
                                                UserRole.CUSTOMER -> Icons.Default.AccountCircle
                                                UserRole.COURIER -> Icons.Default.ElectricScooter
                                                UserRole.HUB_MANAGER -> Icons.Default.Storefront
                                                UserRole.ADMIN -> Icons.Default.Security
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column {
                                        Text(role.titleFa, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(role.subtitleFa, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Icon(Icons.Default.ArrowBackIosNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { onSetStep(AuthFlowStep.PANEL_SELECT) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("انصراف و بازگشت")
                    }
                }

                AuthFlowStep.LOCKED_OUT -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.HourglassDisabled,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(54.dp)
                        )

                        Text(
                            text = "حساب کاربری موقتاً مسدود شد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DangerRed
                        )

                        Text(
                            text = "به دلیل ۴ بار تلاش ناموفق برای ورود، دسترسی شما برای ۵ دقیقه مسدود شده است.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            color = DangerRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "زمان باقیمانده: ${uiState.lockoutRemainingSeconds / 60}:${String.format("%02d", uiState.lockoutRemainingSeconds % 60)}",
                                color = DangerRed,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }

                        Button(
                            onClick = { onSetStep(AuthFlowStep.PASSWORD_RECOVERY) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("درخواست بازیابی فوری از طریق مدیر")
                        }
                    }
                }

                else -> {}
            }
        }
    }
}
