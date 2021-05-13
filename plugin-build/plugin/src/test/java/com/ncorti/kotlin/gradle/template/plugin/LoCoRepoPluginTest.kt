package ir.amv.enterprise.locorepo.client.gradle.plugin

import ir.amv.enterprise.locorepo.client.gradle.plugin.LoCoRepoGeneratorTask
import ir.amv.enterprise.locorepo.client.gradle.plugin.TemplateExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class LoCoRepoPluginTest {

    @Test
    fun `plugin is applied correctly to the project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("ir.amv.enterprise.locorepo.client.gradle.plugin")

        assert(project.tasks.getByName("templateExample") is LoCoRepoGeneratorTask)
    }

    @Test
    fun `extension templateExampleConfig is created correctly`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("ir.amv.enterprise.locorepo.client.gradle.plugin")

        assertNotNull(project.extensions.getByName("templateExampleConfig"))
    }

    @Test
    fun `parameters are passed correctly from extension to task`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("ir.amv.enterprise.locorepo.client.gradle.plugin")
        val aFile = File(project.projectDir, ".tmp")
        (project.extensions.getByName("templateExampleConfig") as TemplateExtension).apply {
            tag.set("a-sample-tag")
            message.set("just-a-message")
            outputFile.set(aFile)
        }

        val task = project.tasks.getByName("templateExample") as LoCoRepoGeneratorTask

        assertEquals("a-sample-tag", task.tag.get())
        assertEquals("just-a-message", task.message.get())
        assertEquals(aFile, task.outputFile.get().asFile)
    }
}
