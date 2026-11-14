package org.limelightcreatives.gradle.tailwind.repository

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.limelightcreatives.gradle.tailwind.platform.TailwindPlatform
import java.net.URI

/**
 * Provides dependency coordinates for Tailwind CSS GitHub releases.
 */
object TailwindReleases {

    const val GROUP = "com.tailwindcss"
    internal const val URL = "https://github.com/tailwindlabs/tailwindcss/releases/download"

    fun cli(platform: TailwindPlatform, version: String): String = "$GROUP:${platform.assetName}:$version"

    fun checksums(version: String): String = "$GROUP:sha256sums.txt:$version"
}

/**
 * Adds Tailwind CSS GitHub releases as an exclusive Ivy repository.
 *
 * Only dependencies in [TailwindReleases.GROUP] are resolved from this repository
 * and dependencies in this group are resolved exclusively from it.
 */
internal fun RepositoryHandler.tailwindReleases() {
    val repositories = this
    exclusiveContent {
        forRepository {
            repositories.ivy {
                name = "Tailwind CSS releases"
                url = URI.create(TailwindReleases.URL)
                patternLayout { artifact("v[revision]/[module]") }
                metadataSources { artifact() } // no ivy.xml
            }
        }
        filter { includeGroup(TailwindReleases.GROUP) }
    }
}
