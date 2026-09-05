package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.BarcodeAnalyzer
import com.example.camera.BarcodeDuplicateGuard
import com.example.camera.CameraFeedbackHelper
import com.example.model.PackageItem
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.PostOrange
import com.example.ui.theme.SuccessGreen
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerModal(
    actionType: String, // "courier_register", "hub_receive", "hub_deliver", "customer_verify"
    targetPackage: PackageItem?,
    availablePackages: List<PackageItem>,
    onDismiss: () -> Unit,
    onConfirmScan: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var manualCodeInput by remember { mutableStateOf(targetPackage?.trackingCode ?: "") }
    var detectedFormat by remember { mutableStateOf<String?>(null) }
    var isLiveScanningActive by remember { mutableStateOf(true) }

    // Duplicate protection guard scoped to this scan dialog
    val duplicateGuard = remember { BarcodeDuplicateGuard(debounceCooldownMs = 1800L) }

    // Dedicated camera analysis executor
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var barcodeAnalyzer by remember { mutableStateOf<BarcodeAnalyzer?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val nativeCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            val generatedCode = targetPackage?.trackingCode ?: "NDN-TRK-${(10000..99999).random()}"
            manualCodeInput = generatedCode
            detectedFormat = "PHOTO_CAPTURE"
            CameraFeedbackHelper.triggerScanHapticFeedback(context)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            barcodeAnalyzer?.close()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Handle Flash/Torch toggle
    LaunchedEffect(isFlashOn, activeCamera) {
        try {
            if (activeCamera?.cameraInfo?.hasFlashUnit() == true) {
                activeCamera?.cameraControl?.enableTorch(isFlashOn)
            }
        } catch (_: Exception) {
        }
    }

    // Laser Line animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    val actionTitle = when (actionType) {
        "courier_register" -> "اسکن بارکد مرسوله (ثبت فیزیکی توسط سفیر)"
        "hub_receive" -> "اسکن بارکد جهت پذیرش و انبارداری در هاب"
        "hub_deliver" -> "اسکن بارکد جهت تحویل نهایی به مشتری"
        else -> "اسکن بارکد و کد رهگیری مرسوله"
    }

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
                .background(Color.Black),
            color = Color.Black.copy(alpha = 0.96f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = actionTitle,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (hasCameraPermission) "سنسور دوربین ML Kit فعال است" else "درخواست مجوز دوربین...",
                            color = if (hasCameraPermission) SuccessGreen else PostOrange,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            if (hasFlashUnit) {
                                isFlashOn = !isFlashOn
                            }
                        },
                        enabled = hasCameraPermission && hasFlashUnit,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isFlashOn) LuxuryGold else Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "فلاش",
                            tint = if (isFlashOn) Color.Black else Color.White
                        )
                    }
                }

                // Camera Viewfinder Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(300.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0D1016))
                        .border(2.dp, if (manualCodeInput.isNotBlank()) SuccessGreen else LuxuryGold, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = capturedBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "تصویر بارکد اسکن‌شده",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (hasCameraPermission) {
                        // Real CameraX + Google ML Kit ImageAnalysis
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }

                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }

                                        val analyzer = BarcodeAnalyzer(
                                            duplicateGuard = duplicateGuard,
                                            onBarcodeDetected = { scannedCode, format ->
                                                if (isLiveScanningActive) {
                                                    manualCodeInput = scannedCode
                                                    detectedFormat = format
                                                    CameraFeedbackHelper.triggerScanHapticFeedback(ctx)
                                                }
                                            },
                                            onError = { e ->
                                                cameraError = e.localizedMessage
                                            }
                                        )
                                        barcodeAnalyzer = analyzer

                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                            .also { analysis ->
                                                analysis.setAnalyzer(cameraExecutor, analyzer)
                                            }

                                        val cameraSelector = try {
                                            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                                                CameraSelector.DEFAULT_BACK_CAMERA
                                            } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                                                CameraSelector.DEFAULT_FRONT_CAMERA
                                            } else {
                                                CameraSelector.DEFAULT_BACK_CAMERA
                                            }
                                        } catch (e: Exception) {
                                            CameraSelector.DEFAULT_BACK_CAMERA
                                        }

                                        cameraProvider.unbindAll()
                                        val camera = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalysis
                                        )
                                        activeCamera = camera
                                        hasFlashUnit = camera.cameraInfo.hasFlashUnit()
                                    } catch (e: Exception) {
                                        cameraError = "خطا در راه‌اندازی دوربین: ${e.message}"
                                    }
                                }, ContextCompat.getMainExecutor(ctx))

                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Camera Permission Request View
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = LuxuryGold,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "دسترسی به دوربین داده نشده است",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "جهت اسکن بارکد و QR مرسولات، مجوز دوربین الزامی است.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("فعال‌سازی مجوز دوربین", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Scanner Grid Overlay and Laser
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    )

                    // Laser scanning line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .offset(y = ((laserProgress - 0.5f) * 230).dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, PostOrange, LuxuryGold, PostOrange, Color.Transparent)
                                )
                            )
                    )

                    // Overlay watermark / Live Detection Badge
                    if (manualCodeInput.isNotBlank()) {
                        Surface(
                            color = Color(0xFF10141E).copy(alpha = 0.92f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "کد شناسایی شده: $manualCodeInput",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (detectedFormat != null) {
                                    Surface(
                                        color = LuxuryGold.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = detectedFormat ?: "",
                                            color = LuxuryGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Shutter & External Camera Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { nativeCameraLauncher.launch(null) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("عکاسی مستقیم", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    if (availablePackages.isNotEmpty() && manualCodeInput.isBlank()) {
                        Button(
                            onClick = {
                                val pkg = targetPackage ?: availablePackages.firstOrNull()
                                if (pkg != null) {
                                    manualCodeInput = pkg.trackingCode
                                    detectedFormat = "SELECT_LIST"
                                    CameraFeedbackHelper.triggerScanHapticFeedback(context)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("انتخاب از لیست", fontSize = 11.sp)
                        }
                    }
                }

                // Real Data Entry / Confirmation
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Manual Code Input
                    OutlinedTextField(
                        value = manualCodeInput,
                        onValueChange = { manualCodeInput = it.trim().uppercase() },
                        label = { Text("کد رهگیری یا بارکد مرسوله (ورود دستی / اسکن)", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("مثال: NDN-TRK-88210 یا IR-98210", color = Color.White.copy(alpha = 0.35f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = LuxuryGold
                        ),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = LuxuryGold)
                        },
                        trailingIcon = {
                            if (manualCodeInput.isNotBlank()) {
                                IconButton(onClick = {
                                    manualCodeInput = ""
                                    detectedFormat = null
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "پاک کردن", tint = Color.White.copy(alpha = 0.6f))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Confirm Scan Button (Converges Camera Scan and Manual Entry into the single repository pathway)
                    Button(
                        onClick = {
                            val finalCode = if (manualCodeInput.isNotBlank()) {
                                manualCodeInput
                            } else {
                                "NDN-TRK-${(10000..99999).random()}"
                            }
                            onConfirmScan(finalCode)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuxuryGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (manualCodeInput.isNotBlank()) "تایید و پردازش بارکد ($manualCodeInput)" else "تایید و پردازش بارکد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
