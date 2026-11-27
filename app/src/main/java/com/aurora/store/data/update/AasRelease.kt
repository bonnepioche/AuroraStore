package com.aurora.store.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal object AasRelease {
    const val REPO = "bonnepioche/AuroraStore"
    const val MAX_APK = 64L * 1024 * 1024
    const val CERT = "2cf99dbb9cd5f2f5e34ce28b383ee198824bac85b1d5ca129cc1a2552dd6da4f"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable private class Release(
        @SerialName("tag_name") val tag: String,
        val draft: Boolean,
        val prerelease: Boolean,
        val assets: List<Asset>
    )
    @Serializable private class Asset(
        val name: String,
        val size: Long,
        val digest: String? = null,
        @SerialName("browser_download_url") val url: String
    )
    data class Candidate(val version: Long, val tag: String, val size: Long, val sha: String, val url: String)

    fun parse(body: String, installed: Long): Candidate? {
        val release = json.decodeFromString<Release>(body)
        require(!release.draft && !release.prerelease)
        require(Regex("aas-v[0-9]+[.][0-9]+(?:[.][0-9]+)?").matches(release.tag))
        val pattern = Regex("AuroraAAS-([1-9][0-9]{0,9})-signed[.]apk")
        val matches = release.assets.filter { pattern.matches(it.name) }
        require(matches.size == 1)
        val asset = matches.single()
        val version = pattern.matchEntire(asset.name)!!.groupValues[1].toLong()
        require(version <= Int.MAX_VALUE)
        if (version <= installed) return null
        require(asset.size in 1..MAX_APK)
        val hash = requireNotNull(asset.digest).removePrefix("sha256:")
        require(asset.digest.startsWith("sha256:") && Regex("[a-f0-9]{64}").matches(hash))
        val expectedUrl = "https://github.com/$REPO/releases/download/${release.tag}/${asset.name}"
        require(asset.url == expectedUrl)
        return Candidate(version, release.tag, asset.size, hash, expectedUrl)
    }
}
