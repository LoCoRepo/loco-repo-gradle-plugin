# LoCoRepo generator gradle plugin

A gradle plugin which can be used to generate code from models using LoCoRepo languages.

## How to use 👣

Based on [Example](example/build.gradle.kts).

```kotlin
plugins {
    ... // your other plugins 
    id("com.locorepo.client.gradle.plugin")
}

locoRepoConfig {
    serviceAccountJson.set(System.getenv("CI_GCP_SERVICE_ACCOUNT_JSON"))
}

sourceSets.getByName("main") {
    java.srcDir(buildDir.resolve("loco-repo/generated/src/main/mps/MODEL_FOLDER_NAME/source_gen"))
}

tasks.compileJava.get().dependsOn(tasks.named("generate"))
```

## Features 🎨

- Allows you to generate using a service account (useful for CI)
- Allows you to generate using a user account (useful for developers)

## Contributing 🤝

Feel free to open a issue or submit a pull request for any bugs/improvements.
