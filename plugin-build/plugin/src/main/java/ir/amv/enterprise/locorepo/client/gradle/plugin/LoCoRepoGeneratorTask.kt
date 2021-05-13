package ir.amv.enterprise.locorepo.client.gradle.plugin

import com.google.api.client.auth.openidconnect.IdTokenResponse
import com.google.auth.oauth2.IdToken
import com.google.auth.oauth2.IdTokenCredentials
import com.google.auth.oauth2.IdTokenProvider
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import ir.amv.enterprise.locorepo.client.gradle.common.LoCoGeneratorCommand
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.internal.logging.text.StyledTextOutputFactory
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.Date


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
        val styledTextOutput =
            services.get(StyledTextOutputFactory::class.java)
                ?.create(javaClass)
        val response = LoCoGeneratorCommand.Builder()
            .modelsZip(
                modelsZip
                    .asFile
                    .orNull!!
                    .toPath()
            )
            .logger(styledTextOutput)
            .build()
            .execute()
        logger.lifecycle("Response: $response")

        // Instantiates a client
        val authentication: IdTokenResponse = AuthenticationService.authentication
        val expireDate: Date = Timestamp.valueOf(
            LocalDateTime.now()
                .plusSeconds(authentication.expiresInSeconds)
        )
        val storage: Storage = StorageOptions.newBuilder()
            .setCredentials(
                IdTokenCredentials.newBuilder()
                    .setIdTokenProvider(object : IdTokenProvider {
                        override fun idTokenWithAudience(
                            targetAudience: String?,
                            options: MutableList<IdTokenProvider.Option>?
                        ): IdToken {
                            return IdToken.create(authentication.accessToken)
                        }
                    })
                    .setTargetAudience("609321703527-7uddmgdd8i0src8i8jlnbu593goj2lek.apps.googleusercontent.com")
                    .build()
            )
            .build()
            .service
//        val fileName = response.get()
//            .first { it.type == ModelGenerationEvent.ModelGenerationEventType.GENERATION_STARTED }
//            .attachment!!
//        var fetchGeneratedModels = runCatching {
//            storage.get(BlobId.of("loco-generate-models", fileName))
//        }
//        while (fetchGeneratedModels.isFailure) {
//            TimeUnit.SECONDS.sleep(10)
//            fetchGeneratedModels = runCatching {
//                storage.get(BlobId.of("loco-generate-models", fileName))
//            }.onFailure {
//                logger.lifecycle("Problem fetching generated models", it)
//            }
//        }
//        val tempFile = Files.createTempFile("generated", "model")
//        fetchGeneratedModels.getOrThrow().downloadTo(tempFile)

        val prettyTag = tag.orNull?.let { "[$it]" } ?: ""

        logger.lifecycle("$prettyTag message is: ${message.orNull}")
        logger.lifecycle("$prettyTag tag is: ${tag.orNull}")
        logger.lifecycle("$prettyTag outputFile is: ${outputFile.orNull}")

        outputFile.get().asFile.writeText("$prettyTag ${message.get()}")
    }
}
