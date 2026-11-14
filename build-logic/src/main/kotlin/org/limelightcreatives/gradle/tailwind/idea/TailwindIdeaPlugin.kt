package org.limelightcreatives.gradle.tailwind.idea

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginManager
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.limelightcreatives.gradle.tailwind.TailwindPlugin
import org.limelightcreatives.gradle.tailwind.TailwindSourceSetConfig

/**
 * Integrates Tailwind CSS source directories with the IntelliJ IDEA project model.
 *
 * Requires both the Tailwind and IDEA plugins to be applied. The integration is
 * automatically applied by [TailwindPlugin] when the IDEA plugin is present.
 */
abstract class TailwindIdeaPlugin : Plugin<Project> {
    override fun apply(project: Project) = with (project) {
        pluginManager.ensurePlugins()
        configureIdea()
    }

    private fun PluginManager.ensurePlugins() {
        requirePlugin("org.limelightcreatives.tailwind")
        requirePlugin("idea")
    }

    private fun PluginManager.requirePlugin(id: String) {
        if (!hasPlugin(id)) throw GradleException("Tailwind IDEA integration requires the '$id' plugin to be applied")
    }

    private fun Project.configureIdea() {
        val sourceSets = extensions.getByType<SourceSetContainer>()

        extensions.configure<IdeaModel> {
            module {
                val ideaModule = this
                sourceSets.configureEach {
                    val tailwind = extensions.findByType<TailwindSourceSetConfig>() ?: return@configureEach
                    ideaModule.sourceDirs = ideaModule.sourceDirs + tailwind.css.srcDirs
                }
            }
        }
    }
}
