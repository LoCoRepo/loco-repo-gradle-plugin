package com.locorepo.client.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

const val EXTENSION_NAME = "locoRepoConfig"
const val TASK_NAME = "locoRepoGenerate"

@Suppress("UnnecessaryAbstractClass")
abstract class LoCoRepoPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        // Add the 'locoRepo' extension object
        val extension = extensions.create(EXTENSION_NAME, LoCoRepoExtension::class.java, this)

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

        // Add a task that uses configuration from the extension object
        val gen = tasks.register(TASK_NAME, LoCoRepoGeneratorTask::class.java) { genTask ->
            genTask.dependsOn(zipTask)
            genTask.outputFile.set(extension.outputFile)
            genTask.modelsZip.set(locoRepoDir.map { it.file("loco-model.zip") })
            genTask.serviceAccountJson.set(extension.serviceAccountJson)
        }
        tasks.register("generate", Copy::class.java) { copy ->
            copy.dependsOn(gen)
            copy.from(zipTree(extension.outputFile))
            copy.into(locoRepoDir.map { it.dir("generated") })
        }
        Unit
    }
}
