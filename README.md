# github-zenhub-sdk

This library provides a convenient interface for interacting with GitHub and ZenHub's GraphQL APIs. It simplifies common operations and offers a structured approach to managing data.

# Build

```bash
./gradlew build
```

# Versioning

We follow [Semantic Versioning 2.0.0][semantic-versioning-2], as we do in most projects at ZIRO.

The version can be found in `version.txt`. YOU MUST BUMP THE VERSION WHEN MAKING CODE CHANGES.

We should follow the git-flow process as we do in other projects. 

# Usage

## Authentication

To use this SDK, you will need to set up a valid GitHub and Zenhub tokens via the following environment variables: `GITHUB_API_TOKEN` and `ZENHUB_GRAPHQL_TOKEN`, respectively.

This may have already been done. You can run the following commands to check:
```bash
echo $GITHUB_API_TOKEN # If nothing prints, then you need to set it up.
```
```bash
echo $ZENHUB_GRAPHQL_TOKEN # If nothing prints, then you need to set it up.
```
If they are not set up, then you can follow [these instructions][how-to-setup-github-zenhub-tokens]. 

## Making API Calls

There is a [GitHubClientSmokeTest.kt][github-client-smoke-test] file you can explore to better understand how to invoke the API. Furthermore, you can actually run the file. Similarly, there is a [ZenHubClientSmokeTest.kt][zenhub-client-smoke-test] file.

# Modifying the SDK

## Updating the schemas

The schemas are found in the corresponding [GraphQL folder][graphql-folder]. However, please note that we do not modify the schema files manually. There is a gradle task to fetch the latest schema from the web for you.
```bash
./gradlew updateSchemas
```

## Adding New GraphQL Queries and Mutations

You can write your own graphql queries and mutations by adding them to the corresponding [GraphQL folder][graphql-folder].

## Updating Existing GraphQL Queries and Mutations

You can modify the graphql queries and mutations as needed, however, you must be careful not to introduce breaking changes. In order to avoid breaking changes, you should follow the [Parallel Change Pattern][parallel-change-pattern]. 

# Generating SDK Code

When you build the project, kotlin code will be automatically generated from the GraphQL code and added to the classpath. You can use the smoke test classes (as described in [Making API Calls](#making-api-calls)) to verify the results.

[github-client-smoke-test]: src/main/kotlin/github/GitHubClientSmokeTest.kt
[github-client]: src/main/kotlin/github/GitHubClient.kt
[gradle-build-file]: build.gradle.kts
[graphql-folder]: src/main/graphql
[how-to-setup-github-zenhub-tokens]: https://stack8.atlassian.net/wiki/spaces/SDBP/pages/1212907850/DEV+Orientation#Configure-GitHub-and-Zenhub
[parallel-change-pattern]: https://martinfowler.com/bliki/ParallelChange.html
[semantic-versioning-2]: https://semver.org/
[zenhub-client-smoke-test]: src/main/kotlin/zenhub/ZenHubClientSmokeTest.kt

# Publishing

You will need to configure your environment variables for Sonatype. In your rc file, add the following:

```bash
export SONATYPE_USERNAME=gradle
export SONATYPE_PASSWORD=<get password from 1pass>
```

To publish the SDK, you can run the following command:
```bash
./gradlew publish
```

### Development:

To publish a snapshot for use during development, you can run the following command:
```bash
./gradlew publish -Psnapshot
```

This will publish a snapshot of the library to our internal Maven repository at https://repository.goziro.com/ with 
the name `<branchName>-SNAPSHOT`

### Merging Your Changes

Other projects ***SHOULD NOT*** be consuming snapshots (aside from locally during development). Instead, they should only consume
proper releases. Publishing and tagging releases is handled by Jenkins. When you merge your changes to `develop`, it triggers a Jenkins
job which builds and tests the repo, then attempts to tag the appropriate commit and publish a release on the nexus repository. 
ENSURE THAT YOU HAVE BUMPED THE VERSION IN `./version.txt` WHEN MAKING CODE CHANGES FOR THIS SYSTEM TO WORK. 

YOU MUST DO THESE TWO THING AFTER MERGING YOUR PR:

***ziro-cli repo needs to be updated*** to consume the version of github-zenhub-sdk that you are publishing

Once you merge your changes, CI will publish a new version to Nexus and tag the repo. The last thing to do is to go to:

https://github.com/Stack8/github-zenhub-sdk/releases

And ***publish a new release***. Click draft a new release, and select your tag, the previous tag, and click generate release notes. 
Make sure "Set as the latest release" is selected, and publish your release. Now you're good to go! 
