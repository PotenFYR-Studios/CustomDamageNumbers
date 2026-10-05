rootProject.name = "CustomDamageNumbers"

plugins {
    // Auto-provisions the pinned JDK 17 toolchain when no local install matches.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include("cdn-core")
include("cdn-modern")
include("cdn-legacy")
