package org.limelightcreatives.gradle.tailwind

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvableConfiguration
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register
import org.limelightcreatives.gradle.tailwind.idea.TailwindIdeaPlugin
import org.limelightcreatives.gradle.tailwind.platform.TailwindPlatform
import org.limelightcreatives.gradle.tailwind.platform.tailwindPlatform
import org.limelightcreatives.gradle.tailwind.repository.TailwindReleases
import org.limelightcreatives.gradle.tailwind.repository.tailwindReleases
import org.limelightcreatives.gradle.tailwind.tasks.TailwindCompile
import org.limelightcreatives.gradle.tailwind.tasks.TailwindInstall
import javax.inject.Inject

/**
 * Configures Tailwind CSS compilation for Java source sets using the standalone CLI,
 * with IntelliJ IDEA integration.
 */
abstract class TailwindPlugin : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        pluginManager.apply(JavaPlugin::class.java)

        val extension = extensions.create<TailwindExtension>(TailwindExtension.NAME)
        val platform = providers.tailwindPlatform()

        repositories.tailwindReleases()
        val cli = releaseFile(
            CLI, "The standalone Tailwind CLI for this machine.",
            extension.version.zip(platform) { version, target -> TailwindReleases.cli(target, version) }
        )
        val checksums = releaseFile(
            CHECKSUMS, "The Tailwind release's sha256sums.txt.",
            extension.version.map(TailwindReleases::checksums)
        )

        val install = registerInstall(extension, platform, cli, checksums)
        val binary = extension.localBinary.orElse(install.flatMap { it.executable })

        // orElse drops the dep on tailwindInstall that flatmap carries so add it back (gradle/gradle#24141)
        val binaryDependency = extension.localBinary.map { emptyList<Any>() }.orElse(listOf(install))

        extensions.getByType<SourceSetContainer>().configureEach {
            registerTailwind(project, extension, binary, binaryDependency)
        }

        pluginManager.withPlugin("idea") {
            pluginManager.apply(TailwindIdeaPlugin::class.java)
        }
    }

    private fun SourceSet.registerTailwind(project: Project, extension: TailwindExtension, binary: Provider<RegularFile>, binaryDependency: Provider<List<Any>>) {
        val settings = addTailwindSettings(project)
        val compile = project.registerTailwindCompile(this, settings, extension, binary, binaryDependency)
        resources.srcDir(compile.flatMap { it.outputDirectory })
    }

    private fun SourceSet.addTailwindSettings(project: Project): TailwindSourceSetConfig {
        // created by projects obj factory as source set extensions cant inject proj services
        val settings = project.objects.newInstance<TailwindSourceSetConfig>(name)
        extensions.add(TailwindSourceSetConfig::class.java, TailwindSourceSetConfig.NAME, settings)

        // part of source set's sources so that IDEs and anything reading allSource see the stylesheets
        allSource.source(settings.css)
        return settings
    }

    private fun Project.registerTailwindCompile(
        sourceSet: SourceSet, settings: TailwindSourceSetConfig,
        extension: TailwindExtension, binary: Provider<RegularFile>,
        binaryDependency: Provider<List<Any>>
    ): TaskProvider<TailwindCompile> =
        tasks.register<TailwindCompile>(sourceSet.getTaskName("compile", "tailwind")) {
            group = TASK_GROUP
            description = "Builds the Tailwind stylesheet for the ${sourceSet.name} source set."
            this.binary.set(binary)
            dependsOn(binaryDependency)
            stylesheets.from(settings.css)
            entry.set(settings.entry)
            content.from(settings.content)
            minify.set(extension.minify)
            outputDirectory.set(layout.buildDirectory.dir("generated/tailwind/${sourceSet.name}"))
            outputPath.set(settings.outputPath)
        }

    private fun Project.releaseFile(name: String, description: String, notation: Provider<String>): NamedDomainObjectProvider<ResolvableConfiguration> {
        val scope = configurations.dependencyScope(name) {
            this.description = description
        }
        dependencies.addProvider(scope.name, notation)

        return configurations.resolvable("${name}Files") {
            extendsFrom(scope)
            this.description = "$description Resolved."
            isTransitive = false
        }
    }

    private fun Project.registerInstall(extension: TailwindExtension, platform: Provider<TailwindPlatform>,
                                cli: NamedDomainObjectProvider<ResolvableConfiguration>,
                                checksums: NamedDomainObjectProvider<ResolvableConfiguration>
    ): TaskProvider<TailwindInstall> {
        val executablePath = extension.version.zip(platform) { version, it ->
            "tailwind/$version/${it.assetName}"
        }

        return tasks.register<TailwindInstall>("tailwindInstall") {
            group = TASK_GROUP
            description = "Verifies the downloaded Tailwind CLI and makes it executable."
            downloaded.from(cli)
            this.checksums.from(checksums)
            assetName.set(platform.map(TailwindPlatform::assetName))
            executable.set(layout.buildDirectory.file(executablePath))
        }
    }

    private companion object {
        const val TASK_GROUP = "build"
        const val CLI = "tailwindCli"
        const val CHECKSUMS = "tailwindChecksums"
    }
}
