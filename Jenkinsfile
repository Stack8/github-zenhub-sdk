pipeline {
    agent {
        label 'master'
    }
    tools {
        jdk 'default'
    }
    environment {
        VERSION = readFile("${WORKSPACE}/version.txt").trim()
    }

    stages {
        stage('create-tag') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    withCredentials([gitUsernamePassword(credentialsId: 'github-http', gitToolName: 'Default')]) {
                        // Clear all tags locally and fetch from origin. If pushing a tag fails for some reason, it will
                        // continue to exist in jenkins even though it won't be present in origin
                        sh "git tag | xargs git tag -d"
                        sh "git fetch --tags"
                        sh "git tag -a v${env.VERSION} -m \"github-zenhub-sdk version v${env.VERSION}\""
                    }
                }
            }
        }

        stage('build') {
            steps {
                script {
                    sh "./gradlew clean build"
                }
            }
            post {
                always {
                   archiveArtifacts allowEmptyArchive: true,
                           artifacts: '**/reports/tests/**'
               }
            }
        }

        // Note: tagging stage MUST come before publishing to prevent a new artifact from overwriting an existing
        // artifact for a specific version. Tagging will fail if you attempt to duplicate a tag, and prevents a duplicated
        // artifact from being published
        stage('push-tag') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    withCredentials([gitUsernamePassword(credentialsId: 'github-http', gitToolName: 'Default')]) {
                        sh "git push origin v${env.VERSION}"
                    }
                }
            }
        }

        stage('publish') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: 'sonatype-creds', usernameVariable: 'SONATYPE_USERNAME', passwordVariable: 'SONATYPE_PASSWORD')
                    ]) {
                        sh "./gradlew publish"
                    }
                }

            }
        }

        stage('create-draft-release') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    withCredentials([gitUsernamePassword(credentialsId: 'github-http', gitToolName: 'Default', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')]) {
                        sh """
                            # Check if GitHub CLI is available
                            if ! command -v gh &> /dev/null; then
                                echo "GitHub CLI (gh) not found - skipping draft release creation"
                                echo "Please install GitHub CLI on Jenkins agent or create release manually"
                                exit 0
                            fi
                            
                            gh release create v${env.VERSION} \\
                                --title "Release v${env.VERSION}" \\
                                --generate-notes \\
                                --draft \\
                                --repo Stack8/github-zenhub-sdk
                        """
                    }
                }
            }
        }
    }
}
