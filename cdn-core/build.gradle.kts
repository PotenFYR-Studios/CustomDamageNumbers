val spigotVersion = property("spigotVersion").toString()
val packetEventsVersion = property("packetEventsVersion").toString()
val adventureVersion = property("adventureVersion").toString()
val bStatsVersion = property("bStatsVersion").toString()
val placeholderApiVersion = property("placeholderApiVersion").toString()
val junitVersion = property("junitVersion").toString()

dependencies {

    // Internal text pipeline only; shaded and relocated into the platform jars.
    implementation("net.kyori:adventure-api:$adventureVersion")
    implementation("net.kyori:adventure-text-minimessage:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-gson:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-plain:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-legacy:$adventureVersion")

    implementation("org.bstats:bstats-bukkit:$bStatsVersion")

    // Optional soft dependency.
    compileOnly("me.clip:placeholderapi:$placeholderApiVersion")
}
