import org.jetbrains.kotlin.backend.wasm.lower.excludeDeclarationsFromCodegen

plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.10"
    java
    `maven-publish`
}

group = "com.ziro.engineering"
version = "1.0.1"

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "com.ziro.engineering"
            artifactId = "library"
            version = "1.0.1"

            from(components["java"])
        }
    }
}

dependencies {
    val ktorVersion = "2.2.3"

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-java:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}
