package ir.amv.enterprise.locorepo.client.gradle.common

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ir.amv.enterprise.locorepo.client.gradle.plugin.AuthenticationService
import org.apache.hc.client5.http.async.methods.AbstractCharResponseConsumer
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.http.*
import org.apache.hc.core5.http.impl.Http1StreamListener
import org.apache.hc.core5.http.impl.bootstrap.AsyncRequesterBootstrap
import org.apache.hc.core5.http.message.BasicHttpRequest
import org.apache.hc.core5.http.message.RequestLine
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.http.nio.AsyncRequestProducer
import org.apache.hc.core5.http.nio.entity.BasicAsyncEntityConsumer
import org.apache.hc.core5.http.nio.entity.FileEntityProducer
import org.apache.hc.core5.http.nio.support.BasicRequestProducer
import org.apache.hc.core5.http.nio.support.BasicResponseConsumer
import org.apache.hc.core5.http.support.BasicRequestBuilder
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.reactor.IOReactorConfig
import org.gradle.api.BuildCancelledException
import org.gradle.internal.logging.text.StyledTextOutput
import java.nio.CharBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit


class LoCoGeneratorCommand private constructor(builder: Builder) {

    private val modelsZip: Path
    private val outputZip: Path
    private val logger: StyledTextOutput?
    private val objectMapper = jacksonObjectMapper()
    private val genEventRespConsumer: AbstractCharResponseConsumer<List<ModelGenerationEvent>>

    init {
        this.modelsZip = builder.modelsZip
        this.outputZip = builder.outputZip
        this.logger = builder.logger
        genEventRespConsumer = object : AbstractCharResponseConsumer<List<ModelGenerationEvent>>() {
            val events = mutableListOf<ModelGenerationEvent>()
            val strBuffer = StringBuffer()

            override fun releaseResources() {
                logger
                    ?.withStyle(StyledTextOutput.Style.ProgressStatus)
                    ?.println("Releasing SSE connection to server")
            }

            override fun capacityIncrement(): Int = Integer.MAX_VALUE

            override fun data(src: CharBuffer, endOfStream: Boolean) {
                while (src.hasRemaining()) {
                    strBuffer.append(src.get())
                    if (strBuffer.indexOf('0') > 0 &&
                        strBuffer.count { it == '{' } == strBuffer.count { it == '}' }
                    ) {
                        strBuffer
                            .toString()
//                            .also { logger?.withStyle(StyledTextOutput.Style.Normal)?.println(it) }
                            .substringAfter("data:")
                            .trim()
                            .takeIf { it.isNotEmpty() }
                            ?.let { objectMapper.readValue<ModelGenerationEvent>(it) }
                            ?.let {
                                logger?.let { styled ->
                                    val logLines = it.attachment?.replace("\n", "\nREMOTE LOG:\t\t")
                                    styled.withStyle(StyledTextOutput.Style.ProgressStatus)
                                        .println("REMOTE LOG:\t\t$logLines")
                                }
                                events.add(it)
                            }
                        strBuffer.delete(0, strBuffer.length)
                    }
                }
            }

            override fun start(response: HttpResponse, contentType: ContentType?) {
                response.ensureSuccess()
            }

            override fun buildResult(): List<ModelGenerationEvent> {
                return events
            }

        }
    }

    fun execute() =
        HttpAsyncClients.createDefault().use { client ->
            client.start()
            val token = AuthenticationService.authentication
                .accessToken
            startIOReactor()
            val host = HttpHost("https", "master-experience-api-4qa2c3clla-ew.a.run.app")

            val request: BasicHttpRequest = BasicRequestBuilder
                .post().also {
                    it.setHttpHost(host)
                    it.path = "/models/e5cb53b0-0c40-49bb-9984-e5442cda2ed3/generations"
                    it.addHeader("Authorization", "Bearer $token")
                }.build()
            val reqProducer: AsyncRequestProducer =
                BasicRequestProducer(
                    request,
                    FileEntityProducer(modelsZip.toFile())
                )
            val events = client.execute(
                reqProducer,
                genEventRespConsumer,
                null
            ).get()
            val id = events.first().generatedFileName
            retryUntilFinishEventReceived(events, id, token, client)

            val get: BasicHttpRequest = BasicRequestBuilder
                .get("https://master-experience-api-4qa2c3clla-ew.a.run.app/models/e5cb53b0-0c40-49bb-9984-e5442cda2ed3/generations/$id")
                .also {
                    it.addHeader("Authorization", "Bearer $token")
                }.build()
            val requestProducer = BasicRequestProducer(get, null)

            client.execute(
                requestProducer,
                BasicResponseConsumer(BasicAsyncEntityConsumer()),
                HttpClientContext.create(),
                null
            ).get().let {
                it.head.ensureSuccess()
                it.body
            }.inputStream().use { input ->
                Files.newOutputStream(outputZip).use { output ->
                    input.copyTo(output)
                }
            }
            val delete: BasicHttpRequest = BasicRequestBuilder
                .delete("https://master-experience-api-4qa2c3clla-ew.a.run.app/models/e5cb53b0-0c40-49bb-9984-e5442cda2ed3/generations/$id")
                .also {
                    it.addHeader("Authorization", "Bearer $token")
                }.build()
            client.execute(
                BasicRequestProducer(delete, null),
                BasicResponseConsumer(BasicAsyncEntityConsumer()),
                HttpClientContext.create(),
                null
            ).get()
        }

    private fun retryUntilFinishEventReceived(
        events: List<ModelGenerationEvent>,
        id: UUID,
        token: String?,
        client: CloseableHttpAsyncClient
    ) {
        var events1 = events
        while (
            events1.isNotEmpty() &&
            !events1.any { it.type == ModelGenerationEvent.ModelGenerationEventType.GENERATION_FINISHED }
        ) {
            val get: BasicHttpRequest = BasicRequestBuilder
                .get("https://master-experience-api-4qa2c3clla-ew.a.run.app/models/e5cb53b0-0c40-49bb-9984-e5442cda2ed3/generations/logs?generatedFileName=$id")
                .also {
                    it.addHeader("Authorization", "Bearer $token")
                }.build()
            val requestProducer = BasicRequestProducer(get, null)
            events1 = client.execute(
                requestProducer,
                genEventRespConsumer,
                null
            ).get()
        }
    }

    private fun startIOReactor() {
        val ioReactorConfig = IOReactorConfig.custom()
            .setSoTimeout(10, TimeUnit.MINUTES)
            .build()
        // Create and start requester
        val requester = AsyncRequesterBootstrap.bootstrap()
            .setIOReactorConfig(ioReactorConfig)
            .setStreamListener(object : Http1StreamListener {
                override fun onRequestHead(connection: HttpConnection, request: HttpRequest) {
                    println(connection.remoteAddress.toString() + " " + RequestLine(request))
                }

                override fun onResponseHead(connection: HttpConnection, response: HttpResponse?) {
                    println(connection.remoteAddress.toString() + " " + StatusLine(response))
                }

                override fun onExchangeComplete(connection: HttpConnection, keepAlive: Boolean) {
                    if (keepAlive) {
                        println(
                            connection.remoteAddress.toString() + " exchange completed (connection kept alive)"
                        )
                    } else {
                        println(
                            connection.remoteAddress.toString() + " exchange completed (connection closed)"
                        )
                    }
                }
            })
            .create()

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                println("HTTP requester shutting down")
                requester.close(CloseMode.GRACEFUL)
            }
        })
        requester.start()
    }

    class Builder {
        lateinit var modelsZip: Path
        lateinit var outputZip: Path
        var logger: StyledTextOutput? = null

        fun modelsZip(modelsZip: Path) = apply { this.modelsZip = modelsZip }
        fun logger(logger: StyledTextOutput?) = apply { this.logger = logger }

        fun build() = LoCoGeneratorCommand(this)
        fun outputFile(outputZip: Path) = apply { this.outputZip = outputZip }
    }
}

private fun HttpResponse.ensureSuccess() {
    if (code < 200 || code >= 300) {
        throw BuildCancelledException("Unsuccessful http response for Code Generation: $this")
    }
}

