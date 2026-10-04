plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":cdn-core"))
}

// The plain jar is an intermediate; only the shaded jar is shipped.
tasks.named<Jar>("jar") {
    archiveClassifier.set("thin")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {

    archiveBaseName.set("CustomDamageNumbers")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    relocate("net.kyori", "in.potenfyr.cdn.libs.kyori")
    relocate("org.bstats", "in.potenfyr.cdn.libs.bstats")
    relocate("com.google.gson", "in.potenfyr.cdn.libs.gson")

    mergeServiceFiles()

    manifest {
        attributes(
            "Implementation-Title" to "CustomDamageNumbers",
            "Implementation-Version" to project.version.toString()
        )
    }
}

tasks.named<ProcessResources>("processResources") {

    // Tracked explicitly: expand() inside filesMatching is evaluated at execution time, so
    // without this the task stays UP-TO-DATE across a version bump and ships a stale
    // plugin.yml with the previous version baked in.
    val pluginVersion = project.version.toString()
    inputs.property("pluginVersion", pluginVersion)

    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.named("build") {
    dependsOn("shadowJar")
}
