package org.limelightcreatives.gradle.tailwind

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

abstract class TailwindExtension @Inject constructor(providers: ProviderFactory) {

    abstract val version: Property<String>

    abstract val minify: Property<Boolean>

    abstract val localBinary: RegularFileProperty

    init {
        version.convention(providers.provider {
            throw GradleException("The Tailwind version must be configured.")
        })
        minify.convention(true)
    }

    companion object {
        const val NAME = "tailwind"
    }
}
