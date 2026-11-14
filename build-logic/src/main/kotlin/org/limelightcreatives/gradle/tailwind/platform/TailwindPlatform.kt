package org.limelightcreatives.gradle.tailwind.platform

import org.gradle.api.GradleException
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

/**
 * An OS and CPU combination that Tailwind publishes a standalone CLI for.
 */
data class TailwindPlatform(val os: Os, val cpu: Cpu) {

    enum class Os(val id: String, val extension: String = "") {
        LINUX("linux"),
        MACOS("macos"),
        WINDOWS("windows", ".exe")
    }

    enum class Cpu(val id: String) {
        X64("x64"),
        ARM64("arm64")
    }

    val assetName: String = "tailwindcss-${os.id}-${cpu.id}${os.extension}"

    companion object {
        fun of(os: String, arch: String): TailwindPlatform {
            val osName = os.lowercase()
            val twOs = when {
                osName.startsWith("win") -> Os.WINDOWS
                "mac" in osName || "darwin" in osName -> Os.MACOS
                "linux" in osName -> Os.LINUX
                else -> throw GradleException("No Tailwind CLI build for OS $os")
            }
            val twCpu = when (arch.lowercase()) {
                "amd64", "x86_64" -> Cpu.X64
                "aarch64", "arm64" -> Cpu.ARM64
                else -> throw GradleException("No Tailwind CLI build for architecture $arch")
            }
            return TailwindPlatform(twOs, twCpu)
        }
    }
}

fun ProviderFactory.tailwindPlatform(): Provider<TailwindPlatform> =
    systemProperty("os.name")
        .zip(systemProperty("os.arch"), TailwindPlatform::of)
