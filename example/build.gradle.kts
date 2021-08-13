plugins {
    java
    idea
    id("ir.amv.enterprise.locorepo.client.gradle.plugin")
}

templateExampleConfig {
    message.set("Just trying this gradle plugin...")
}

sourceSets.getByName("main") {
    java.srcDir(buildDir.resolve("loco-repo/generated/src/main/mps/zargari/source_gen"))
}

tasks.compileJava.get().dependsOn(tasks.named("generate"))