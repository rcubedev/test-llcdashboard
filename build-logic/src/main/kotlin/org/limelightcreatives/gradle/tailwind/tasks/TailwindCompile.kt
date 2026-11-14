package org.limelightcreatives.gradle.tailwind.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Compiles a source set's Tailwind CSS stylesheets using the standalone CLI.
 *
 * The task tracks its stylesheets, content files, CLI binary and compilation
 * options as inputs.
 *
 * The task is skipped with `NO-SOURCE` when no stylesheets are configured.
 */
@CacheableTask
abstract class TailwindCompile @Inject constructor(private val exec: ExecOperations) : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val binary: RegularFileProperty

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stylesheets: ConfigurableFileCollection

    @get:Input
    abstract val entry: Property<String>

    /**
     * Files scanned by Tailwind to detect utility classes.
     */
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val content: ConfigurableFileCollection

    @get:Input
    abstract val minify: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /**
     * Relative path of the generated CSS file within [outputDirectory], e.g. `static/css/app.css`
     */
    @get:Input
    abstract val outputPath: Property<String>

    @TaskAction
    fun compile() {
        val entryFile = findEntry()

        // clear output folder to ensure no stale CSS
        outputDirectory.get().asFile.deleteRecursively()
        val output = outputDirectory.file(outputPath).get().asFile
        output.parentFile.mkdirs()

        val wrapper = File(temporaryDir, "tailwind-input.css")
        wrapper.writeText(buildString {
            appendLine("@import \"${cssPath(entryFile)}\";")
            content.files.filter { it.exists() }.sortedBy { it.path }.forEach {
                appendLine("@source \"${cssPath(it)}\";")
            }
        })

        exec.exec {
            executable(binary.get().asFile.absolutePath)
            // empty cwd so v4's automatic scan finds nothing and content is only source
            workingDir(temporaryDir)
            args("--input", wrapper.absolutePath, "--output", output.absolutePath)
            if (minify.get()) args("--minify")
        }
    }

    private fun cssPath(file: File): String = file.absoluteFile.invariantSeparatorsPath.replace("\"", "\\\"")

    private fun findEntry(): File {
        val wanted = entry.get()
        val found = mutableListOf<File>()
        stylesheets.asFileTree.visit {
            if (!isDirectory && relativePath.pathString == wanted) found += file
        }
        return when (found.size) {
            1 -> found.single()
            0 -> throw GradleException(
                "No entry stylesheet '$wanted' among ${stylesheets.files.joinToString { it.path }}. " +
                        "Set tailwind { entry = ... } on the source set."
            )
            else -> throw GradleException("More than one '$wanted': ${found.joinToString { it.path }}")
        }
    }
}
