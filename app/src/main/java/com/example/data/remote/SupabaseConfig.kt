package com.example.data.remote

import com.example.BuildConfig

/**
 * Remote configuration for Supabase integration.
 * Injected strictly through BuildConfig via .env / AI Studio Secrets panel.
 * 
 * SECURITY MANDATE:
 * - Never expose or reference service_role keys.
 * - Never print publishable keys in application logs.
 * - Default placeholders indicate unconfigured remote state.
 */
data class SupabaseConfig(
    val url: String = BuildConfig.SUPABASE_URL,
    val publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY
) {
    /**
     * Checks if Supabase URL and Publishable Key are configured and valid.
     */
    fun isConfigured(): Boolean {
        val hasValidUrl = url.isNotBlank() &&
                !url.contains("placeholder-pudo-project", ignoreCase = true) &&
                (url.startsWith("https://") || url.startsWith("http://"))
        val hasValidKey = publishableKey.isNotBlank() &&
                !publishableKey.contains("placeholder", ignoreCase = true)
        return hasValidUrl && hasValidKey
    }

    /**
     * Returns sanitized representation without leaking publishable key secrets in logs.
     */
    override fun toString(): String {
        val maskedKey = if (publishableKey.length > 8) {
            "${publishableKey.take(4)}...${publishableKey.takeLast(4)}"
        } else {
            "***"
        }
        return "SupabaseConfig(url='$url', publishableKey='$maskedKey', isConfigured=${isConfigured()})"
    }

    companion object {
        fun default(): SupabaseConfig = SupabaseConfig()
    }
}
