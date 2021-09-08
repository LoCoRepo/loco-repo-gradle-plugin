package com.locorepo.client.gradle.plugin

import com.locorepo.client.gradle.common.LoCoGeneratorCommand
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.internal.logging.text.StyledTextOutputFactory
import kotlin.io.path.ExperimentalPathApi

@CacheableTask
abstract class LoCoRepoGeneratorTask : DefaultTask() {

    init {
        description = "Calls remote generator service from LoCoRepo"

        // Don't forget to set the group here.
        group = BasePlugin.BUILD_GROUP
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val modelsZip: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    @get:Option(option = "serviceAccountJson", description = "The service account JSON")
    @get:Optional
    abstract val serviceAccountJson: Property<String>

    @ExperimentalPathApi
    @TaskAction
    fun sampleAction() {
        val styledTextOutput =
            services.get(StyledTextOutputFactory::class.java)
                ?.create(javaClass)
        val zipPath = this.modelsZip
            .asFile
            .orNull
            ?: throw NullPointerException()
        val downloadedFile = LoCoGeneratorCommand.Builder()
            .modelsZip(zipPath.toPath())
            .outputFile(outputFile.get().asFile.toPath())
            .logger(styledTextOutput)
            .serviceAccountJson(serviceAccountJson.orNull)
            .build()
            .execute()
        logger.lifecycle("Downloaded file: $downloadedFile")
    }
}
