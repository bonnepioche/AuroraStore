package com.aurora.store.util

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialCipherTest {
    @Test fun roundTripAndRandomIv() {
        val first = CredentialCipher.encrypt("fake-test-token", "test:aas")
        val second = CredentialCipher.encrypt("fake-test-token", "test:aas")
        assertTrue(first.startsWith("enc:v1:"))
        assertFalse(first.contains("fake-test-token"))
        assertNotEquals(first, second)
        assertEquals("fake-test-token", CredentialCipher.decrypt(first, "test:aas"))
    }
    @Test fun rejectsWrongPurpose() {
        val encrypted = CredentialCipher.encrypt("fake", "test:aas")
        assertThrows(java.security.GeneralSecurityException::class.java) {
            CredentialCipher.decrypt(encrypted, "test:auth")
        }
    }
    @Test fun rejectsTampering() {
        val encrypted = CredentialCipher.encrypt("fake", "test:aas")
        val bytes = Base64.decode(encrypted.removePrefix("enc:v1:"), Base64.NO_WRAP)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        assertThrows(java.security.GeneralSecurityException::class.java) {
            CredentialCipher.decrypt("enc:v1:" + Base64.encodeToString(bytes, Base64.NO_WRAP), "test:aas")
        }
    }
    @Test fun rejectsPlaintext() {
        assertThrows(IllegalArgumentException::class.java) { CredentialCipher.decrypt("fake", "test:aas") }
    }
}
