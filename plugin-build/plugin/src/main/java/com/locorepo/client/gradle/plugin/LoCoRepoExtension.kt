package com.locorepo.client.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

const val DEFAULT_OUTPUT_FILE = "loco-repo/generated.zip"

@Suppress("UnnecessaryAbstractClass")
abstract class LoCoRepoExtension @Inject constructor(project: Project) {

    private val objects = project.objects

    // Example of a property with a default set with .convention
    val outputFile: RegularFileProperty = objects.fileProperty().convention(
        project.layout.buildDirectory.file(DEFAULT_OUTPUT_FILE)
    )

    val modelsDir: DirectoryProperty = objects.directoryProperty().convention(
        project.layout.projectDirectory.dir("src/main/mps")
    )

    val serviceAccountJson: Property<String> = objects.property(String::class.java)
}
