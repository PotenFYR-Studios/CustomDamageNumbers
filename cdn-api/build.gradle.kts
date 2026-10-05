plugins {
    `maven-publish`
}

// The shared subprojects block already provides compileOnly spigot-api and the test
// setup. The API surface deliberately depends on nothing else: no PacketEvents, no
// Adventure, so consumer builds stay light and version-safe.

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "cdn-api"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
