package com.aurora.store.data.update

import org.junit.Assert.*
import org.junit.Test

class AasReleaseTest {
    private fun payload(version: Int = 80, url: String = "https://github.com/bonnepioche/AuroraStore/releases/download/aas-v0.5/AuroraAAS-$version-signed.apk", digest: String = "sha256:" + "a".repeat(64), size: Long = 1234): String =
        """{"tag_name":"aas-v0.5","draft":false,"prerelease":false,"assets":[{"name":"AuroraAAS-$version-signed.apk","size":$size,"digest":"$digest","browser_download_url":"$url"}]}"""

    @Test fun acceptsNewRelease() { assertEquals(80L, AasRelease.parse(payload(), 79)?.version) }
    @Test fun refusesRollback() { assertNull(AasRelease.parse(payload(78), 79)) }
    @Test fun refusesSameVersion() { assertNull(AasRelease.parse(payload(79), 79)) }
    @Test fun refusesForeignUrl() { assertThrows(IllegalArgumentException::class.java) { AasRelease.parse(payload(url="https://example.com/a.apk"), 79) } }
    @Test fun refusesMissingHash() { assertThrows(IllegalArgumentException::class.java) { AasRelease.parse(payload(digest=""), 79) } }
    @Test fun refusesOversize() { assertThrows(IllegalArgumentException::class.java) { AasRelease.parse(payload(size=AasRelease.MAX_APK+1), 79) } }
    @Test fun refusesPrerelease() { assertThrows(IllegalArgumentException::class.java) { AasRelease.parse(payload().replace("\"prerelease\":false", "\"prerelease\":true"), 79) } }
}
