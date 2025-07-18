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
        stage('build') {
            steps {
                script {
                    sh "./gradlew build"
                }
            }
            post {
                always {
                   archiveArtifacts allowEmptyArchive: true,
                           artifacts: '**/reports/tests/**'
               }
            }
        }

        // Tag before publishing to prevent duplicate versions
        stage('tag-and-push') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    withCredentials([gitUsernamePassword(credentialsId: 'github-http', gitToolName: 'Default')]) {
                        // Clean up any stale local tags from previous failed builds
                        sh "git tag -d \$(git tag -l)"
                        sh "git fetch --tags"
                        sh "git tag -a v${VERSION} -m 'github-zenhub-sdk version v${VERSION}'"
                        sh "git push origin v${VERSION}"
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
                        sh "CI_MODE=true ./gradlew publish"
                    }
                }

            }
        }
    }
}
