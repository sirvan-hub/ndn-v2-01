package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.model.NavigationApp
import com.google.android.gms.location.LocationServices
import java.util.concurrent.Executor

object DeviceHardwareHelper {

    /**
     * Dial phone number using device dialer
     */
    fun makePhoneCall(context: Context, phoneNumber: String) {
        val cleanPhone = phoneNumber.trim().replace(" ", "").replace("-", "")
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "خطا در برقراری تماس: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Send SMS to specific phone number
     */
    fun sendSms(context: Context, phoneNumber: String, messageText: String = "") {
        val cleanPhone = phoneNumber.trim().replace(" ", "").replace("-", "")
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanPhone")
                putExtra("sms_body", messageText)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "امکان باز کردن برنامه پیامک وجود ندارد.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Launch Navigation App (Google Maps, Waze, Neshan, Balad) with real Coordinates
     */
    fun launchNavigationApp(
        context: Context,
        app: NavigationApp,
        lat: Double,
        lng: Double,
        destinationTitle: String = "هاب NDN"
    ) {
        when (app) {
            NavigationApp.BALAD -> openBalad(context, lat, lng)
            NavigationApp.NESHAN -> openNeshan(context, lat, lng)
            NavigationApp.WAZE -> openWaze(context, lat, lng)
            NavigationApp.GOOGLE_MAPS -> openGoogleMaps(context, lat, lng, destinationTitle)
        }
    }

    private fun openBalad(context: Context, lat: Double, lng: Double) {
        // Balad deep link: balad://location?latitude=...&longitude=...
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("balad://location?latitude=$lat&longitude=$lng")).apply {
                setPackage("ir.balad.navigation")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web or app store
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://balad.ir/location?latitude=$lat&longitude=$lng")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (err: Exception) {
                Toast.makeText(context, "مسیریاب بلد یافت نشد.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openNeshan(context: Context, lat: Double, lng: Double) {
        // Neshan deep link: neshan://navigate?lat=...&lng=... or geo:lat,lng
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("neshan://navigate?lat=$lat&lng=$lng")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://neshan.org/maps/@$lat,$lng,16z")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (err: Exception) {
                Toast.makeText(context, "مسیریاب نشان یافت نشد.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openWaze(context: Context, lat: Double, lng: Double) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("waze://?ll=$lat,$lng&navigate=yes")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?ll=$lat,$lng&navigate=yes")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (err: Exception) {
                Toast.makeText(context, "مسیریاب Waze یافت نشد.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGoogleMaps(context: Context, lat: Double, lng: Double, label: String) {
        try {
            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            try {
                val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (err: Exception) {
                Toast.makeText(context, "برنامه نقشه یافت نشد.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Real Android Biometric Prompt (Fingerprint / Face ID)
     */
    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor: Executor = ContextCompat.getMainExecutor(activity)
                val biometricPrompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                onError(errString.toString())
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            onError("اثر انگشت شناسایی نشد. مجدداً تلاش نمایید.")
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("احراز هویت بیومتریک سامانه NDN")
                    .setSubtitle("لطفاً اثر انگشت خود را روی حسگر قرار دهید")
                    .setDescription("جهت ورود سریع و امن به پنل کاربری")
                    .setAllowedAuthenticators(authenticators)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onError("هیچ اثر انگشت یا بیومتریکی روی این دستگاه ثبت نشده است. لطفاً با نام کاربری و رمز عبور وارد شوید.")
            }
            else -> {
                // Hardware missing/unavailable/temporarily unavailable — do NOT treat as success.
                // A missing or unusable sensor must never be silently accepted as "authenticated".
                onError("ورود بیومتریک روی این دستگاه در دسترس نیست. لطفاً با نام کاربری و رمز عبور وارد شوید.")
            }
        }
    }

    /**
     * Get Real Device GPS Location (with LocationManager / FusedLocation fallback)
     */
    @SuppressLint("MissingPermission")
    fun fetchDeviceGpsLocation(
        context: Context,
        onLocationReceived: (lat: Double, lng: Double) -> Unit,
        onFallback: () -> Unit = {}
    ) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            onFallback()
            return
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onLocationReceived(location.latitude, location.longitude)
                } else {
                    // Fallback to LocationManager
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val gpsLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    val netLoc = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    val bestLoc = gpsLoc ?: netLoc

                    if (bestLoc != null) {
                        onLocationReceived(bestLoc.latitude, bestLoc.longitude)
                    } else {
                        onFallback()
                    }
                }
            }.addOnFailureListener {
                onFallback()
            }
        } catch (e: Exception) {
            onFallback()
        }
    }
}
