package com.uniquemakeover.admin.core.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

private const val ITERATIONS = 100_000
private const val KEY_LENGTH = 256
private const val SALT_BYTES = 16
private const val HEX_RADIX = 16
private const val BITS_PER_NIBBLE = 4

@Singleton
class PinHasher @Inject constructor() {

    private val random = SecureRandom()

    fun hashPin(pin: String): String {
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)
        val hash = pbkdf2(pin.toCharArray(), salt)
        return "${salt.toHex()}:${hash.toHex()}"
    }

    fun verifyPin(pin: String, storedSaltAndHash: String): Boolean {
        val parts = storedSaltAndHash.split(":")
        if (parts.size != 2) return false
        val salt = parts[0].fromHex()
        val expectedHash = parts[1].fromHex()
        val computedHash = pbkdf2(pin.toCharArray(), salt)
        return constantTimeEquals(expectedHash, computedHash)
    }

    private fun pbkdf2(chars: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray {
        val len = length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            val highNibble = Character.digit(this[i], HEX_RADIX) shl BITS_PER_NIBBLE
            val lowNibble = Character.digit(this[i + 1], HEX_RADIX)
            data[i / 2] = (highNibble + lowNibble).toByte()
            i += 2
        }
        return data
    }
}
