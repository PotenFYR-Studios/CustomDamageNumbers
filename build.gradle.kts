plugins {
    java
    id("com.gradleup.shadow") version "9.6.1" apply false
}

val javaRelease = property("javaRelease").toString().toInt()
val spigotVersion = property("spigotVersion").toString()
val packetEventsVersion = property("packetEventsVersion").toString()
val junitVersion = property("junitVersion").toString()

allprojects {
    group = property("group").toString()
    version = property("version").toString()
}

subprojects {

    apply(plugin = "java")

    repositories {

        mavenCentral()

        // spigot-api (snapshots)
        maven {
            name = "spigot-repo"
            url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        }

        // packetevents
        maven {
            name = "codemc-releases"
            url = uri("https://repo.codemc.io/repository/maven-releases/")
        }

        // placeholderapi
        maven {
            name = "placeholderapi"
            url = uri("https://repo.extendedclip.com/releases/")
        }
    }

    // Provided by the server and by the PacketEvents plugin. Declared for every
    // module because Gradle's compileOnly is not transitive.
    dependencies {
        add("compileOnly", "org.spigotmc:spigot-api:$spigotVersion")
        add("compileOnly", "com.github.retrooper:packetevents-spigot:$packetEventsVersion")

        // Available to every module's tests: junit, plus the server API so tests can
        // build real YamlConfiguration instances and load backend classes.
        add("testImplementation", "org.junit.jupiter:junit-jupiter:$junitVersion")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        add("testImplementation", "org.spigotmc:spigot-api:$spigotVersion")

        // PacketEvents types appear in the backend metadata payloads the tests assert on,
        // and compileOnly is not on the test compile classpath.
        add("testImplementation", "com.github.retrooper:packetevents-spigot:$packetEventsVersion")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(javaRelease)
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "failed", "skipped")
        }
    }
}
