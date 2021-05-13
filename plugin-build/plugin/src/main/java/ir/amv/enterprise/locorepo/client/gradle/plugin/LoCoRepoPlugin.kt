package ir.amv.enterprise.locorepo.client.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip

const val EXTENSION_NAME = "templateExampleConfig"
const val TASK_NAME = "templateExample"

abstract class LoCoRepoPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        // Add the 'template' extension object
        val extension = extensions.create(EXTENSION_NAME, TemplateExtension::class.java, this)

        // Zip loco models
        val zipTask = tasks.register("ZipLoCoModels", Zip::class.java) {
            it.archiveFileName.set("loco-model.zip")
            it.destinationDirectory.set(layout.buildDirectory.dir("loco-repo"))
            it
                .from(layout.projectDirectory)
                .include(".mps/**/*.*")
                .include("src/main/mps/**/*.*")
        }

        // Add a task that uses configuration from the extension object
        tasks.register(TASK_NAME, LoCoRepoGeneratorTask::class.java) {
            it.dependsOn(zipTask)
            it.tag.set(extension.tag)
            it.message.set(extension.message)
            it.outputFile.set(extension.outputFile)
            it.modelsZip.set(layout.buildDirectory.file("loco-repo/loco-model.zip"))
        }
        Unit
    }
}
