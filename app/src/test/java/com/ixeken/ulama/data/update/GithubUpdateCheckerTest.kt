package com.ixeken.ulama.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para validar la logica de comparacion de versiones SemVer
 * y evaluacion de publicaciones en GithubUpdateChecker.
 */
class GithubUpdateCheckerTest {

    @Test
    fun compareSemVer_equalVersions_returnsZero() {
        assertEquals(0, GithubUpdateChecker.compareSemVer("2.0", "v2.0"))
        assertEquals(0, GithubUpdateChecker.compareSemVer("v2.0.0", "2.0"))
        assertEquals(0, GithubUpdateChecker.compareSemVer("2.0.0", "v2.0.0"))
    }

    @Test
    fun compareSemVer_installedLowerThanRemote_returnsNegative() {
        assertTrue(GithubUpdateChecker.compareSemVer("2.0", "v2.1") < 0)
        assertTrue(GithubUpdateChecker.compareSemVer("2.0", "v2.0.1") < 0)
        assertTrue(GithubUpdateChecker.compareSemVer("1.9.0", "v2.0.0") < 0)
        assertTrue(GithubUpdateChecker.compareSemVer("2.0.5", "v2.1.0") < 0)
    }

    @Test
    fun compareSemVer_installedHigherThanRemote_returnsPositive() {
        assertTrue(GithubUpdateChecker.compareSemVer("2.0", "v1.9") > 0)
        assertTrue(GithubUpdateChecker.compareSemVer("2.0.1", "v2.0") > 0)
        assertTrue(GithubUpdateChecker.compareSemVer("3.0", "v2.9.9") > 0)
        assertTrue(GithubUpdateChecker.compareSemVer("1.10.0", "v1.9.0") > 0)
    }

    @Test
    fun evaluateRelease_whenHigher_returnsNewVersionAvailable() {
        val checker = GithubUpdateChecker(
            installedVersionProvider = { "2.0" }
        )
        val release = GithubReleaseDto(
            tagName = "v2.1",
            htmlUrl = "https://github.com/Ixeken-Studios/ulama-app/releases/tag/v2.1",
            name = "Ulama 2.1",
            body = "Mejoras de rendimiento"
        )
        val result = checker.evaluateRelease(release)

        assertTrue(result is UpdateCheckResult.NewVersionAvailable)
        val newVersion = result as UpdateCheckResult.NewVersionAvailable
        assertEquals("2.1", newVersion.version)
        assertEquals("https://github.com/Ixeken-Studios/ulama-app/releases/tag/v2.1", newVersion.releaseUrl)
        assertEquals("Mejoras de rendimiento", newVersion.releaseNotes)
    }

    @Test
    fun evaluateRelease_whenEqual_returnsUpToDate() {
        val checker = GithubUpdateChecker(
            installedVersionProvider = { "2.0" }
        )
        val release = GithubReleaseDto(
            tagName = "v2.0",
            htmlUrl = "https://github.com/Ixeken-Studios/ulama-app/releases/tag/v2.0",
            name = "Ulama 2.0"
        )
        val result = checker.evaluateRelease(release)

        assertTrue(result is UpdateCheckResult.UpToDate)
        assertEquals("2.0", (result as UpdateCheckResult.UpToDate).version)
    }

    @Test
    fun evaluateRelease_whenLower_returnsAheadOfRelease() {
        val checker = GithubUpdateChecker(
            installedVersionProvider = { "2.1" }
        )
        val release = GithubReleaseDto(
            tagName = "v2.0",
            htmlUrl = "https://github.com/Ixeken-Studios/ulama-app/releases/tag/v2.0"
        )
        val result = checker.evaluateRelease(release)

        assertTrue(result is UpdateCheckResult.AheadOfRelease)
        val ahead = result as UpdateCheckResult.AheadOfRelease
        assertEquals("2.1", ahead.installedVersion)
        assertEquals("2.0", ahead.releaseVersion)
    }
}
