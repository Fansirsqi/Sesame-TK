package fansirsqi.xposed.sesame.net

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecureApiClient(
    private val baseUrl: String = "http://127.0.0.1:8008",
    private val signatureKey: String = "fuckyou"
) {
    private var publicKey: PublicKey? = null
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

    fun getPublicKey(): Boolean {
        val content = ByteArray(0)
        val request =
            Request.Builder()
                .url("$baseUrl/api/public_key")
                .post(content.toRequestBody(null, 0, content.size))
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return false

            val body = response.body.string()
            val json = JSONObject(body)
            if (json.optInt("status") == 100) {
                val pem = json.optString("public_key")
                publicKey = loadRSAPublicKey(pem)
                return true
            }
        }
        return false
    }

    fun secureVerify(
        deviceId: String? = null,
        alipayId: String? = null,
        token: String? = null,
        path: String
    ): JSONObject? {
        return try {
            if (publicKey == null && !getPublicKey()) return null

            val aesKey = generateAESKey()
            val requestJson = JSONObject().apply {
                put("device_id", deviceId)
                if (alipayId != null) put("alipay_id", alipayId)
                if (token != null) put("authorization", "Bearer $token")
            }

            val (ciphertext, iv, tag) = aesGcmEncrypt(requestJson.toString(), aesKey)
            val encryptedKey = rsaEncryptAESKey(aesKey, publicKey!!)

            val keyB64 = Base64.encodeToString(encryptedKey, Base64.NO_WRAP)
            val dataB64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val tagB64 = Base64.encodeToString(tag, Base64.NO_WRAP)
            val timestamp = System.currentTimeMillis() / 1000

            val sig = generateSignature(keyB64, dataB64, ivB64, tagB64, timestamp, signatureKey)

            val payload = JSONObject().apply {
                put("key", keyB64)
                put("data", dataB64)
                put("iv", ivB64)
                put("tag", tagB64)
                put("ts", timestamp)
                put("sig", sig)
            }

            val body = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder().url("$baseUrl$path").post(body).build()

            val result = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    JSONObject().put("error", "HTTP Error: ${response.code}")
                } else {
                    val encryptedBody = response.body.string()
                    val encryptedJson = JSONObject(encryptedBody)
                    val ivDec = Base64.decode(encryptedJson.getString("iv"), Base64.NO_WRAP)
                    val tagDec = Base64.decode(encryptedJson.getString("tag"), Base64.NO_WRAP)
                    val dataDec = Base64.decode(encryptedJson.getString("data"), Base64.NO_WRAP)
                    val decryptedPayload = aesGcmDecrypt(aesKey, ivDec, tagDec, dataDec)
                    JSONObject(decryptedPayload)
                }
            }

            return result
        } catch (e: Exception) {
            e.printStackTrace()
            JSONObject().put("error", e.message)
        }
    }


    private fun loadRSAPublicKey(pem: String): PublicKey {
        val clean =
            pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\n", "")

        val decoded = Base64.decode(clean, Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(decoded)
        return KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }

    private fun generateAESKey(): SecretKey {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(256)
        return generator.generateKey()
    }

    private fun aesGcmEncrypt(
        plainText: String,
        key: SecretKey
    ): Triple<ByteArray, ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val encrypted = cipher.doFinal(plainText.toByteArray())
        val tag = encrypted.takeLast(16).toByteArray()
        val actualCiphertext = encrypted.dropLast(16).toByteArray()
        return Triple(actualCiphertext, iv, tag)
    }

    private fun aesGcmDecrypt(
        key: SecretKey,
        iv: ByteArray,
        tag: ByteArray,
        ciphertext: ByteArray
    ): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv) // 128 is the tag length in bits
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        // The JCE provider expects the ciphertext and tag to be concatenated for decryption
        val encryptedData = ciphertext + tag
        val decryptedBytes = cipher.doFinal(encryptedData)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun rsaEncryptAESKey(key: SecretKey, publicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(key.encoded)
    }

    private fun generateSignature(
        keyB64: String,
        dataB64: String,
        ivB64: String,
        tagB64: String,
        ts: Long,
        secretKey: String
    ): String {
        val input = "$keyB64$dataB64$ivB64$tagB64$ts"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretKey.toByteArray(), "HmacSHA256"))
        return mac.doFinal(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
