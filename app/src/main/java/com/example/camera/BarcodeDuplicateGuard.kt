package com.example.camera

import java.util.concurrent.ConcurrentHashMap

/**
 * Scanner-level duplicate guard to prevent identical physical barcode reads
 * across multiple camera frames within a scan session.
 *
 * UX duplicate protection works in tandem with RegistrationTransaction idempotency
 * at the repository layer.
 */
class BarcodeDuplicateGuard(
    private val debounceCooldownMs: Long = 2000L
) {
    private val lastScannedTimestamps = ConcurrentHashMap<String, Long>()
    private val sessionAcceptedCodes = ConcurrentHashMap.newKeySet<String>()

    /**
     * Evaluates whether a detected barcode should trigger an event.
     */
    fun shouldProcess(rawCode: String?): Boolean {
        if (rawCode.isNullOrBlank()) return false
        val cleanCode = rawCode.trim()
        val now = System.currentTimeMillis()

        val lastTime = lastScannedTimestamps[cleanCode]
        if (lastTime != null && (now - lastTime) < debounceCooldownMs) {
            return false
        }

        return true
    }

    /**
     * Registers a code as processed to initiate the debounce window.
     */
    fun markProcessed(rawCode: String) {
        val cleanCode = rawCode.trim()
        val now = System.currentTimeMillis()
        lastScannedTimestamps[cleanCode] = now
        sessionAcceptedCodes.add(cleanCode)
    }

    /**
     * Checks if the code was ever accepted in this active session.
     */
    fun isAlreadyAcceptedInSession(rawCode: String): Boolean {
        return sessionAcceptedCodes.contains(rawCode.trim())
    }

    /**
     * Resets session state.
     */
    fun reset() {
        lastScannedTimestamps.clear()
        sessionAcceptedCodes.clear()
    }
}
