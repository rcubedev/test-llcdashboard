package org.limelightcreatives.gradle.tailwind

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class TailwindSourceSetConfig @Inject constructor(sourceSetName: String, objects: ObjectFactory) {

    val css: SourceDirectorySet = objects.sourceDirectorySet("${sourceSetName}Css", "$sourceSetName CSS").apply {
        srcDir("src/$sourceSetName/css")
        include("**/*.css")
    }

    abstract val entry: Property<String>

    abstract val content: ConfigurableFileCollection

    abstract val outputPath: Property<String>

    init {
        entry.convention("app.css")
        outputPath.convention(entry.map { "static/css/$it" })
    }

    companion object {
        const val NAME = "tailwind"
    }
}
