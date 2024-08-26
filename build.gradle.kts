plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    java
    `maven-publish`
    id("com.apollographql.apollo3") version "3.8.2"
}

group = "com.ziro.engineering"
version = "1.4.1"

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
            version = "${project.version}"
            from(components["java"])
        }
    }
}

dependencies {
    implementation("com.apollographql.apollo3:apollo-runtime")
    implementation("com.apollographql.apollo3:apollo-api")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

apollo {
    service("zenhub") {
        packageName.set("com.ziro.engineering.zenhub.graphql.sdk")
        sourceFolder.set("./zenhub")
        introspection {
            headers.put("Authorization", "Bearer ${System.getenv("ZENHUB_GRAPHQL_TOKEN")}")
            endpointUrl.set("https://api.zenhub.com/public/graphql")
            schemaFile.set(file("src/main/graphql/zenhub/schema.json"))
        }
    }
    service("github") {
        packageName.set("com.ziro.engineering.github.graphql.sdk")
        sourceFolder.set("./github")
        introspection {
            headers.put("Authorization", "Bearer ${System.getenv("GITHUB_API_TOKEN")}")
            endpointUrl.set("https://api.github.com/graphql")
            schemaFile.set(file("src/main/graphql/github/schema.json"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("updateSchemas") {
    group = "GraphQL Schema Tasks"
    description = "Updates GraphQL Schemas"
    dependsOn(
        "downloadGithubApolloSchemaFromIntrospection",
        "downloadZenhubApolloSchemaFromIntrospection"
    )
}
