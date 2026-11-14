package org.limelightcreatives.gradle.tailwind.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.limelightcreatives.gradle.tailwind.verification.ChecksumList
import org.limelightcreatives.gradle.tailwind.verification.sha256

@DisableCachingByDefault(because = "Restoring the binary from the cache costs about the same as copying it")
abstract class TailwindInstall : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val downloaded: ConfigurableFileCollection

    @get:Input
    abstract val assetName: Property<String>

    /**
     * The resolved `tailwindChecksumsFiles` configuration, which contains the release's `sha256sums.txt`
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val checksums: ConfigurableFileCollection

    @get:OutputFile
    abstract val executable: RegularFileProperty

    @TaskAction
    fun install() {
        val source = downloaded.singleFile
        val asset = assetName.get()
        val checksumFile = checksums.singleFile

        val expected = ChecksumList.read(checksumFile)[asset]
            ?: throw GradleException(
                "No checksum for $asset in the release's sha256sums.txt. " +
                        "Tailwind may not publish a binary for this platform.")
        val actual = source.sha256()
        if (actual != expected) {
            throw GradleException(
                """
                Checksum mismatch for $asset
                  expected $expected
                  got      $actual
                The download may be corrupt or tampered with so it wasn't used.
                Run with --refresh-dependencies to fetch it again.
                """.trimIndent()
            )
        }

        val target = executable.get().asFile
        source.copyTo(target, overwrite = true)
        if (!target.setExecutable(true)) {
            throw GradleException("Could not make $target executable")
        }
    }
}
