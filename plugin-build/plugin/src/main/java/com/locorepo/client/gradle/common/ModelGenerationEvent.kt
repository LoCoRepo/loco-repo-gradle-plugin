package com.locorepo.client.gradle.common

import org.gradle.internal.impldep.com.fasterxml.jackson.annotation.JsonCreator
import org.gradle.internal.impldep.com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class ModelGenerationEvent @JsonCreator constructor(
    @JsonProperty("generatedFileName")
    val generatedFileName: UUID,
    @JsonProperty("type")
    val type: ModelGenerationEventType,
    @JsonProperty("attachment")
    val attachment: String? = null
) {
    enum class ModelGenerationEventType {
        GENERATION_STARTED,
        GENERATION_LOG_RECEIVED,
        GENERATION_FINISHED
    }
}
