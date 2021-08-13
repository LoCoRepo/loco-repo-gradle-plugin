package ir.amv.enterprise.locorepo.client.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

const val EXTENSION_NAME = "templateExampleConfig"
const val TASK_NAME = "templateExample"

abstract class LoCoRepoPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        // Add the 'template' extension object
        val extension = extensions.create(EXTENSION_NAME, TemplateExtension::class.java, this)

        // Zip loco models
        val locoRepoDir = layout.buildDirectory.dir("loco-repo")
        val zipTask = tasks.register("ZipLoCoModels", Zip::class.java) {
            it.archiveFileName.set("loco-model.zip")
            it.destinationDirectory.set(locoRepoDir)
            it
                .from(layout.projectDirectory)
                .include(".mps/**/*.*")
                .include("src/main/mps/**/*.*")
        }

        configurations.create("loco")

        // Add a task that uses configuration from the extension object
        val gen = tasks.register(TASK_NAME, LoCoRepoGeneratorTask::class.java) { genTask ->
            genTask.dependsOn(zipTask)
            genTask.tag.set(extension.tag)
            genTask.message.set(extension.message)
            genTask.outputFile.set(extension.outputFile)
            genTask.modelsZip.set(locoRepoDir.map { it.file("loco-model.zip") })
        }
        tasks.register("generate", Copy::class.java) { copy ->
            copy.dependsOn(gen)
            copy.from(zipTree(extension.outputFile))
            copy.into(locoRepoDir.map { it.dir("generated") })
        }
        Unit
    }
}
