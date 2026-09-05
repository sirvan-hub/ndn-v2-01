package com.example.util

/**
 * Authoritative normalizer and validator for identity fields (Phone / Mobile & National ID / کد ملی).
 * Handles Persian/Arabic digits conversion, formatting stripping, and Iranian National ID checksum verification.
 */
object IdentityNormalizer {

    /**
     * Converts Persian (۰-۹) and Arabic (٠-٩) digits to standard ASCII English digits (0-9).
     */
    fun normalizeDigits(input: String?): String {
        if (input == null) return ""
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '۰'..'۹' -> sb.append(('0'.code + (ch - '۰')).toChar())
                in '٠'..'٩' -> sb.append(('0'.code + (ch - '٠')).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Authoritative normalization for Iranian Mobile Numbers:
     * 1. Converts Persian/Arabic digits to English.
     * 2. Strips spaces, dashes, parentheses, dots.
     * 3. Normalizes prefixes:
     *    - "+989..." -> "09..."
     *    - "00989..." -> "09..."
     *    - "989..." -> "09..."
     *    - "9..." (10 digits) -> "09..."
     * 4. Returns standard 11-digit Iranian mobile number starting with "09", or clean digits if non-conforming.
     */
    fun normalizePhone(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        val digitsOnly = normalizeDigits(phone).replace(Regex("[^0-9+]"), "")
        
        var clean = digitsOnly
        if (clean.startsWith("+98")) {
            clean = "0" + clean.substring(3)
        } else if (clean.startsWith("0098")) {
            clean = "0" + clean.substring(4)
        } else if (clean.startsWith("98") && clean.length == 12) {
            clean = "0" + clean.substring(2)
        } else if (clean.startsWith("9") && clean.length == 10) {
            clean = "0$clean"
        }
        
        return clean
    }

    fun normalizeIranianPhone(phone: String?): String = normalizePhone(phone)

    /**
     * Validates if a phone string is a valid Iranian 11-digit mobile number (09xxxxxxxxx).
     */
    fun isValidIranianMobile(phone: String?): Boolean {
        val normalized = normalizePhone(phone)
        return normalized.matches(Regex("^09[0-9]{9}$"))
    }

    fun isValidIranianPhone(phone: String?): Boolean = isValidIranianMobile(phone)

    /**
     * Authoritative normalization for Iranian National ID (کد ملی):
     * 1. Converts Persian/Arabic digits to English.
     * 2. Strips spaces, dashes, dots, and non-digit characters.
     * 3. Left-pads with zeroes up to 10 digits if 8 or 9 digits long.
     */
    fun normalizeNationalId(nationalId: String?): String {
        if (nationalId.isNullOrBlank()) return ""
        val digits = normalizeDigits(nationalId).replace(Regex("[^0-9]"), "")
        if (digits.length in 8..9) {
            return digits.padStart(10, '0')
        }
        return digits
    }

    /**
     * Standard Iranian National ID (کد ملی) checksum validation algorithm.
     * Must be exactly 10 digits and pass modulo-11 weighted check.
     */
    fun isValidNationalId(nationalId: String?): Boolean {
        val code = normalizeNationalId(nationalId)
        if (code.length != 10 || !code.all { it.isDigit() }) {
            return false
        }

        // Check for all identical digits (e.g., 0000000000, 1111111111) which are invalid
        val allSame = (0..9).any { digit -> code == digit.toString().repeat(10) }
        if (allSame) return false

        var sum = 0
        for (i in 0 until 9) {
            sum += code[i].digitToInt() * (10 - i)
        }
        val remainder = sum % 11
        val checkDigit = code[9].digitToInt()

        return if (remainder < 2) {
            checkDigit == remainder
        } else {
            checkDigit == (11 - remainder)
        }
    }

    /**
     * Authoritative normalization for Usernames:
     * 1. Converts Persian/Arabic digits to English digits.
     * 2. Trims leading/trailing whitespace.
     * 3. Converts to lowercase.
     */
    fun normalizeUsername(username: String?): String {
        if (username.isNullOrBlank()) return ""
        return normalizeDigits(username).trim().lowercase()
    }

    /**
     * Validates if a username meets security and format requirements:
     * Length between 3 and 30 characters, alphanumeric and underscores/dots only.
     */
    fun isValidUsername(username: String?): Boolean {
        val normalized = normalizeUsername(username)
        return normalized.matches(Regex("^[a-z0-9_.]{3,30}$"))
    }

    private const val PBKDF2_ITERATIONS = 10000
    private const val PBKDF2_KEY_LENGTH = 256 // in bits
    private const val LEGACY_GLOBAL_SALT = "ndn_pudo_secure_salt_v2"

    /**
     * Secure PBKDF2-HMAC-SHA256 password hashing with a cryptographic per-user random salt.
     * Stored format: PBKDF2$<iterations>$<saltHex>$<hashHex>
     * Plaintext passwords are NEVER stored, logged, or serialized.
     */
    fun hashPassword(password: String): String {
        if (password.isBlank()) return ""
        val random = java.security.SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val saltHex = salt.joinToString("") { "%02x".format(it) }

        val spec = javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            PBKDF2_KEY_LENGTH
        )
        val skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = skf.generateSecret(spec).encoded
        val hashHex = hash.joinToString("") { "%02x".format(it) }

        return "PBKDF2\$$PBKDF2_ITERATIONS\$$saltHex\$$hashHex"
    }

    /**
     * Verifies a candidate password against the stored password hash.
     * Supports both modern PBKDF2 (per-user salt) and legacy salted SHA-256 for backward compatibility.
     * Uses constant-time MessageDigest.isEqual to prevent timing attacks.
     * A blank/missing stored hash means the account has no usable password and must NEVER authenticate.
     */
    fun verifyPassword(candidatePassword: String, storedHash: String): Boolean {
        if (storedHash.isBlank() || candidatePassword.isBlank()) return false

        return if (storedHash.startsWith("PBKDF2$")) {
            val parts = storedHash.split("$")
            if (parts.size != 4) return false
            val iterations = parts[1].toIntOrNull() ?: return false
            val saltHex = parts[2]
            val expectedHashHex = parts[3]

            val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val spec = javax.crypto.spec.PBEKeySpec(
                candidatePassword.toCharArray(),
                salt,
                iterations,
                PBKDF2_KEY_LENGTH
            )
            val skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val candidateHash = skf.generateSecret(spec).encoded
            val candidateHashHex = candidateHash.joinToString("") { "%02x".format(it) }

            java.security.MessageDigest.isEqual(
                candidateHashHex.toByteArray(Charsets.UTF_8),
                expectedHashHex.toByteArray(Charsets.UTF_8)
            )
        } else {
            // Legacy salted SHA-256 fallback for existing accounts
            val legacyBytes = java.security.MessageDigest.getInstance("SHA-256")
                .digest((LEGACY_GLOBAL_SALT + candidatePassword).toByteArray(Charsets.UTF_8))
            val legacyCandidateHex = legacyBytes.joinToString("") { "%02x".format(it) }

            java.security.MessageDigest.isEqual(
                legacyCandidateHex.toByteArray(Charsets.UTF_8),
                storedHash.toByteArray(Charsets.UTF_8)
            )
        }
    }
}

