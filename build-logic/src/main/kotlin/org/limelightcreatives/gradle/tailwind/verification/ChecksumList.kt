package org.limelightcreatives.gradle.tailwind.verification

import java.io.File
import java.security.MessageDigest

class ChecksumList private constructor(private val byAsset: Map<String, String>) {

    operator fun get(asset: String): String? = byAsset[asset]

    companion object {
        fun parse(text: String): ChecksumList = ChecksumList(
            text.lineSequence()
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val parts = line.split(Regex("\\s+"), limit = 2)
                    if (parts.size == 2) parts[1].removePrefix("*").removePrefix("./") to parts[0].lowercase()
                    else null
                }.toMap()
        )

        fun read(file: File): ChecksumList = parse(file.readText())
    }
}

fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().toHexString()
}
