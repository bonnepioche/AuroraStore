package com.aurora.store.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.aurora.store.BuildConfig
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class AasUpdateFileProvider : FileProvider()

/** Separate, anonymous HTTP client. Never sends Play credentials to GitHub. */
internal object AasUpdater {
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()
    private val downloadHosts = setOf("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com")

    suspend fun check(): AasRelease.Candidate? = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/${AasRelease.REPO}/releases/latest"
        client.newCall(Request.Builder().url(url).header("Accept", "application/vnd.github+json").build())
            .execute().use { response ->
                if (response.code == 404) return@withContext null
                if (!response.isSuccessful) throw IOException("Release metadata unavailable")
                val source = requireNotNull(response.body).source()
                // Request at most 1 MiB plus one byte; reject oversized responses.
                require(!source.request(1048577L))
                val body = source.readUtf8()
                AasRelease.parse(body, BuildConfig.VERSION_CODE.toLong())
            }
    }

    private fun downloadResponse(initial: String): Response {
        var url = initial.toHttpUrl()
        repeat(5) {
            require(url.isHttps && url.host in downloadHosts && url.port == 443 &&
                url.username.isEmpty() && url.password.isEmpty())
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            if (response.code in 300..399) {
                val next: HttpUrl? = response.header("Location")?.let { url.resolve(it) }
                response.close()
                url = requireNotNull(next)
            } else {
                if (!response.isSuccessful) {
                    response.close()
                    throw IOException("Update download failed")
                }
                return response
            }
        }
        throw IOException("Too many redirects")
    }

    suspend fun download(context: Context, candidate: AasRelease.Candidate): File =
        withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "aas-updates").apply { mkdirs() }
            // Keep recent files until the system installer has consumed the URI.
            directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 86400000L }
                ?.forEach { it.delete() }
            val file = File.createTempFile("update-", ".apk", directory)
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                var total = 0L
                downloadResponse(candidate.url).use { response ->
                    requireNotNull(response.body).byteStream().use { input ->
                        file.outputStream().use { out ->
                            val buffer = ByteArray(32768)
                            while (true) {
                                ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                require(total <= candidate.size && total <= AasRelease.MAX_APK)
                                digest.update(buffer, 0, count)
                                out.write(buffer, 0, count)
                            }
                        }
                    }
                }
                require(total == candidate.size && hex(digest.digest()) == candidate.sha)
                verifyPackage(context, file, candidate.version)
                file
            } catch (error: Throwable) {
                file.delete()
                throw error
            }
        }

    @Suppress("DEPRECATION")
    private fun verifyPackage(context: Context, file: File, version: Long) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
            else PackageManager.GET_SIGNATURES
        val archive = requireNotNull(pm.getPackageArchiveInfo(file.absolutePath, flags))
        val installed = pm.getPackageInfo(context.packageName, flags)
        require(context.packageName == "com.bonnepioche.aurorastore")
        require(archive.packageName == context.packageName)
        require(PackageInfoCompat.getLongVersionCode(archive) == version)
        require(version > PackageInfoCompat.getLongVersionCode(installed))
        require(certificate(archive) == AasRelease.CERT && certificate(installed) == AasRelease.CERT)
    }

    @Suppress("DEPRECATION")
    private fun certificate(info: PackageInfo): String {
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            val signing = requireNotNull(info.signingInfo)
            require(!signing.hasMultipleSigners())
            signing.apkContentsSigners
        } else info.signatures
        val signature = requireNotNull(signatures).single()
        return hex(MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()))
    }

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }

    fun installIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.aasUpdateProvider", file)
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
