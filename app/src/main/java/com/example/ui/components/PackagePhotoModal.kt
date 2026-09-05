package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import com.example.model.PackageItem
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PackagePhotoModal(
    targetPackage: PackageItem?,
    onDismiss: () -> Unit,
    onConfirmPhoto: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val timeStamp = remember { SimpleDateFormat("HH:mm:ss - yyyy/MM/dd", Locale("fa", "IR")).format(Date()) }

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
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(isFlashOn, activeCamera) {
        try {
            activeCamera?.cameraControl?.enableTorch(isFlashOn)
        } catch (_: Exception) {
        }
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
                .background(Color.Black),
            color = Color.Black.copy(alpha = 0.95f)
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ثبت تصویر و بررسی سلامت بسته",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "مرسوله: ${targetPackage?.title ?: "بسته پستی"} (${targetPackage?.trackingCode ?: "NDN-PKG"})",
                            color = LuxuryGold,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        enabled = hasCameraPermission && activeCamera != null,
                        modifier = Modifier
                            .size(40.dp)
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

                // Camera viewfinder or captured preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF101318))
                        .border(2.dp, if (capturedBitmap != null) SuccessGreen else LuxuryGold, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = capturedBitmap
                    if (bmp != null) {
                        // Display captured photo
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "تصویر ثبت‌شده از مرسوله",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Watermark banner
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "واترمارک امنیتی: هاب ${targetPackage?.hubName ?: "مرکزی"} | $timeStamp",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "تایید سلامت فیزیکی بسته",
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else if (hasCameraPermission) {
                        // Real Camera Live Preview
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
                                        activeCamera = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview
                                        )
                                    } catch (_: Exception) {
                                    }
                                }, ContextCompat.getMainExecutor(ctx))

                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Focus guide overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        )
                    } else {
                        // Permission Needed View
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = LuxuryGold,
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "برای باز شدن تصویر زنده، نیاز به تایید مجوز دوربین است.",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = Color.Black)
                            ) {
                                Text("تایید مجوز دوربین", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Bottom actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (capturedBitmap == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Take photo with native system camera directly
                                    nativeCameraLauncher.launch(null)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LuxuryGold,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Camera, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ثبت عکس با دوربین", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    // Quick confirm photo
                                    onConfirmPhoto("package_${targetPackage?.trackingCode ?: "TRK"}_${System.currentTimeMillis()}.jpg")
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تایید وضعیت سلامت", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { capturedBitmap = null },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("عکاسی مجدد")
                            }

                            Button(
                                onClick = {
                                    onConfirmPhoto("package_verified_${System.currentTimeMillis()}.jpg")
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(2f)
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SuccessGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تایید و ذخیره تصویر در سامانه", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
