package ir.amv.enterprise.locorepo.client.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

abstract class LoCoRepoGeneratorTask : DefaultTask() {

    init {
        description = "Just a sample template task"

        // Don't forget to set the group here.
        group = "LoCoRepo Client"
    }

    @get:Input
    @get:Option(option = "message", description = "A message to be printed in the output file")
    abstract val message: Property<String>

    @get:InputFile
    abstract val modelsZip: RegularFileProperty

    @get:Input
    @get:Option(option = "tag", description = "A Tag to be used for debug and in the output file")
    @get:Optional
    abstract val tag: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun sampleAction() {
        val prettyTag = tag.orNull?.let { "[$it]" } ?: ""

        logger.lifecycle("$prettyTag message is: ${message.orNull}")
        logger.lifecycle("$prettyTag tag is: ${tag.orNull}")
        logger.lifecycle("$prettyTag outputFile is: ${outputFile.orNull}")

        outputFile.get().asFile.writeText("$prettyTag ${message.get()}")
    }
}
