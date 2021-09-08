object PluginCoordinates {
    const val ID = "com.locorepo.client.gradle.plugin"
    const val GROUP = "com.locorepo.client.gradle"
    const val VERSION = "1.0.0"
    const val IMPLEMENTATION_CLASS = "com.locorepo.client.gradle.plugin.LoCoRepoPlugin"
}

object PluginBundle {
    const val VCS = "https://github.com/LoCoRepo/loco-repo-gradle-plugin"
    const val WEBSITE = "https://github.com/LoCoRepo/loco-repo-gradle-plugin"
    const val DESCRIPTION = "A gradle plugin used for generating LoCoRepo languages"
    const val DISPLAY_NAME = "LoCoRepo Gradle Plugin"
    val TAGS = listOf(
        "plugin",
        "gradle",
        "locorepo",
        "language",
        "code-generator",
        "generator"
    )
}

