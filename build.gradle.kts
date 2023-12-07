plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    java
    `maven-publish`
    id("com.apollographql.apollo3") version "3.8.2"
}

group = "com.ziro.engineering"
version = "1.1.0"

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
    implementation("com.apollographql.apollo3:apollo-runtime:3.8.2")
    implementation("com.apollographql.apollo3:apollo-api:3.8.2")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

apollo {
    service("zenhub") {
        packageName.set("com.ziro.engineering.zenhub.graphql.sdk")
    }
}

tasks.test {
    useJUnitPlatform()
}

