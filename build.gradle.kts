var currentVersion = "5.5.0"

if (project.hasProperty("snapshot")) {
    currentVersion = "${currentVersion}-SNAPSHOT"
}

plugins {
    java
    `maven-publish`
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "1.9.10"
    id("com.apollographql.apollo3") version "3.8.2"
    id("com.diffplug.spotless") version "7.0.2"
}

group = "com.ziro.engineering"
version = currentVersion

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
            artifactId = "github-zenhub-sdk"
            version = currentVersion
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://repository.goziro.com/repository/engineering/")
            credentials {
                try {
                    username = System.getenv("SONATYPE_USERNAME") as String
                    password = System.getenv("SONATYPE_PASSWORD") as String
                } catch (e: NullPointerException) {
                    throw Exception("SONATYPE_USERNAME and SONATYPE_PASSWORD environment variables are not set! Please see the README for instructions on how to do this.", e)
                }
            }
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
        mapScalar("JSON", "kotlin.String", "adapters.JsonAdapter")
    }
    service("github") {
        packageName.set("com.ziro.engineering.github.graphql.sdk")
        sourceFolder.set("./github")
        introspection {
            headers.put("Authorization", "Bearer ${System.getenv("GITHUB_API_TOKEN")}")
            endpointUrl.set("https://api.github.com/graphql")
            schemaFile.set(file("src/main/graphql/github/schema.json"))
        }
        mapScalar("URI", "java.net.URI", "adapters.UriAdapter")
    }
}

spotless {
    kotlin {
        ktfmt().configure {
            it.setBlockIndent(4)
            it.setContinuationIndent(4)
            it.setRemoveUnusedImports(true)
        }
        targetExclude("build/**")
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
