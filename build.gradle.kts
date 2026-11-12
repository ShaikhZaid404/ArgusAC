plugins {
    `java`
    id("com.gradleup.shadow") version "8.3.5"
}

group = "dev.argus"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    // Paper API (Provided by server runtime)
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")

    // PacketEvents 2.x
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")

    // Database Pooling & SQLite
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")

    // Discord JDA (Minimal core without redundant audio)
    implementation("net.dv8tion:JDA:5.2.1") {
        exclude(module = "opus-java")
    }

    // Annotations
    compileOnly("org.jetbrains:annotations:26.0.1")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier.set("all")
    archiveBaseName.set("ArgusAC")
    
    // Relocate shaded dependencies (NEVER relocate org.sqlite as JNI native bindings break)
    relocate("com.zaxxer.hikari", "dev.argus.libs.hikari")
    relocate("net.dv8tion.jda", "dev.argus.libs.jda")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
