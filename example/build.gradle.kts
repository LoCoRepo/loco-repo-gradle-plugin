plugins {
    java
    idea
    id("ir.amv.enterprise.locorepo.client.gradle.plugin")
}

locoRepoConfig {
    serviceAccountJson.set(System.getenv("CI_GCP_SERVICE_ACCOUNT_JSON"))
}

sourceSets.getByName("main") {
    java.srcDir(buildDir.resolve("loco-repo/generated/src/main/mps/zargari/source_gen"))
}

tasks.compileJava.get().dependsOn(tasks.named("generate"))
