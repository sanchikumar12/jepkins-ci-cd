pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    parameters {
        booleanParam(name: 'RUN_SMOKE_TESTS', defaultValue: true, description: 'Run tests named *Smoke* after the full Maven verification stage.')
    }

    environment {
        SERVICES = 'Discovery-Server auth-service course-service lesson-service enrollment-service discussion-service notification-service payment-service'
        MAVEN_OPTS = '-Dmaven.test.failure.ignore=false'
        DOCKERHUB_NAMESPACE = 'sanchitkumarsingh098931'
        DOCKERHUB_CREDENTIALS_ID = 'dockerhub-token'
        GITHUB_CREDENTIALS_ID = 'github-token'
        SONARQUBE_ENV = 'sonarqube'
        HELM_VALUES_FILE = 'charts/edulearn/values.yaml'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.IMAGE_TAG = sh(
                        script: 'git rev-parse --short=12 HEAD',
                        returnStdout: true
                    ).trim()
                }
            }
        }

        stage('Build and Test Services') {
            steps {
                script {
                    env.SERVICES.split().each { service ->
                        dir(service) {
                            sh 'mvn -B clean verify'
                        }
                    }
                }
            }
        }

        stage('Smoke Tests') {
            when {
                expression { return params.RUN_SMOKE_TESTS }
            }
            steps {
                script {
                    env.SERVICES.split().each { service ->
                        dir(service) {
                            sh 'mvn -B -Dtest="*Smoke*" -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test'
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv("${SONARQUBE_ENV}") {
                    script {
                        env.SERVICES.split().each { service ->
                            dir(service) {
                                sh """
                                    mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                                      -Dsonar.projectKey=edulearn-${service} \
                                      -Dsonar.projectName=edulearn-${service}
                                """
                            }
                        }
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build and Push Docker Images') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', "${DOCKERHUB_CREDENTIALS_ID}") {
                        env.SERVICES.split().each { service ->
                            def image = docker.build("${DOCKERHUB_NAMESPACE}/${service.toLowerCase()}:${IMAGE_TAG}", "${service}")
                            retry(3) {
                                image.push()
                            }
                            retry(3) {
                                image.push('latest')
                            }
                        }
                    }
                }
            }
        }

        stage('Update Helm Image Tags') {
            when {
                anyOf {
                    branch 'main'
                    branch 'master'
                }
            }
            steps {
                sh 'chmod +x ci/update-image-tags.sh'
                sh './ci/update-image-tags.sh "$HELM_VALUES_FILE" "$IMAGE_TAG"'
                withCredentials([usernamePassword(credentialsId: "${GITHUB_CREDENTIALS_ID}", usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_TOKEN')]) {
                    sh '''
                        git config user.email "jenkins@local"
                        git config user.name "Jenkins CI"
                        git add "$HELM_VALUES_FILE"
                        if git diff --cached --quiet; then
                          echo "No Helm image tag changes to commit."
                        else
                          git commit -m "ci: update image tags to ${IMAGE_TAG}"
                          git push "https://${GIT_USERNAME}:${GIT_TOKEN}@${GIT_URL#https://}" HEAD:${BRANCH_NAME}
                        fi
                    '''
                }
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: '**/target/*.jar, **/target/lib/*.jar', fingerprint: true, allowEmptyArchive: true
                junit testResults: '**/target/surefire-reports/TEST-*.xml', allowEmptyResults: true
            }
        }
    }
}
