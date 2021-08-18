package ir.amv.enterprise.locorepo.client.gradle.plugin

import ir.amv.enterprise.locorepo.client.gradle.common.LoCoGeneratorCommand
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.internal.logging.text.StyledTextOutputFactory
import kotlin.io.path.ExperimentalPathApi

@CacheableTask
abstract class LoCoRepoGeneratorTask : DefaultTask() {

    init {
        description = "Just a sample template task"

        // Don't forget to set the group here.
        group = "LoCoRepo Client"
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val modelsZip: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @ExperimentalPathApi
    @TaskAction
    fun sampleAction() {
        val styledTextOutput =
            services.get(StyledTextOutputFactory::class.java)
                ?.create(javaClass)
        val downloadedFile = LoCoGeneratorCommand.Builder()
            .modelsZip(
                modelsZip
                    .asFile
                    .orNull!!
                    .toPath()
            )
            .outputFile(outputFile.get().asFile.toPath())
            .logger(styledTextOutput)
            .build()
            .execute()
        logger.lifecycle("Downloaded file: $downloadedFile")
    }
}
