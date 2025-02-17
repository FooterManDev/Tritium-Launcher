package auth

import getEnv
import userHome
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyStore
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Centralized AES/GCM encryption/decryption
object CryptoMngr {
    private const val AES_KEY_SIZE = 256
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private val secureRandom = SecureRandom()

    private const val KEYSTORE_TYPE = "PKCS12"

    private const val KEY_ALIAS = "tritiumLauncherSecret"

    private val KEYSTORE_FILE: Path = Paths.get(userHome, ".tritiumLauncher", "keystore.p12").also {
        if(!Files.exists(it.parent)) Files.createDirectories(it.parent)
    }

    private val KEYSTORE_PASSWORD: String = getEnv("TRITIUM_LAUNCHER_KEYSTORE_PASSWORD")
        ?: throw IllegalStateException("Environment variable TRITIUM_LAUNCHER_KEYSTORE_PASSWORD is not set.")

    val secretKey: SecretKey by lazy {
        val keyStore = KeyStore.getInstance(KEYSTORE_TYPE)
        if(Files.exists(KEYSTORE_FILE)) {
            FileInputStream(KEYSTORE_FILE.toFile()).use { fis ->
                keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray())
            }
            keyStore.getKey(KEY_ALIAS, KEYSTORE_PASSWORD.toCharArray()) as? SecretKey
                ?: throw IllegalStateException("Secret key not found in keystore")
        } else {
            keyStore.load(null, KEYSTORE_PASSWORD.toCharArray())
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(AES_KEY_SIZE, secureRandom)
            val newKey = keyGen.generateKey()
            keyStore.setEntry(KEY_ALIAS, KeyStore.SecretKeyEntry(newKey), KeyStore.PasswordProtection(KEYSTORE_PASSWORD.toCharArray()))

            FileOutputStream(KEYSTORE_FILE.toFile()).use { fos ->
                keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray())
            }
            newKey
        }
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(IV_LENGTH_BYTE)
        secureRandom.nextBytes(iv)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val combined = iv + cipherText
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(encryptedData: String): String {
        val combined = Base64.getDecoder().decode(encryptedData)
        val iv = combined.copyOfRange(0, IV_LENGTH_BYTE)
        val cipherText = combined.copyOfRange(IV_LENGTH_BYTE, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val plainBytes = cipher.doFinal(cipherText)
        return String(plainBytes, StandardCharsets.UTF_8)
    }
}