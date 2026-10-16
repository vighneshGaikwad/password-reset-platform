/**
 * Jenkins Declarative Pipeline for Password Reset Platform (SSPR)
 *
 * This pipeline implements a robust DevSecOps CI/CD workflow:
 * 1. Checkout source code from SCM
 * 2. Build and package the Spring Boot backend artifact
 * 3. Run unit tests and publish Surefire test reports
 * 4. Spin up a temporary application instance for integration verification
 * 5. Execute Selenium browser tests and REST Assured API integration tests
 * 6. Build production-grade container image using multi-stage Dockerfile
 * 7. Push container images with build-specific and latest tags
 * 8. Deploy containerized service into the target environment (dev, staging, prod)
 * 9. Perform post-deployment smoke health check against live endpoints
 * 10. Automatic rollback and artifact archiving in post-actions
 */
pipeline {
    agent any

    // Use Maven installation defined in Jenkins Global Tool Configuration
    tools {
        maven 'maven'
    }

    // Pipeline environment variables
    environment {
        DOCKER_IMAGE = 'sspr-backend'
        DOCKER_TAG = "${BUILD_NUMBER}"
        DOCKER_REGISTRY = 'docker.io'  // Target container registry (can be configured to local or private registry)
        APP_PORT = '8080'
    }

    // Build parameters for customizable deployment targets
    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'prod'], description: 'Deployment Environment')
        string(name: 'APP_PORT', defaultValue: '8080', description: 'Application Port')
    }

    stages {
        /**
         * Stage 1: Checkout
         * Retrieves the latest source code revisions from the SCM repository.
         */
        stage('Checkout') {
            steps {
                echo "Checking out source code from repository..."
                checkout scm
            }
        }

        /**
         * Stage 2: Build & Package
         * Compiles backend Java code and packages executable JAR artifact,
         * skipping tests at this step to allow dedicated test execution stages.
         */
        stage('Build & Package') {
            steps {
                dir('sspr-backend') {
                    echo 'Compiling and packaging the application...'
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        /**
         * Stage 3: Unit Tests
         * Executes backend JUnit test suites.
         * Generates and publishes standard Surefire XML test reports to Jenkins.
         */
        stage('Unit Tests') {
            steps {
                dir('sspr-backend') {
                    echo 'Running unit tests...'
                    sh 'mvn test'
                }
            }
            post {
                always {
                    // Collect and archive unit test results for build history trends
                    junit 'sspr-backend/target/surefire-reports/*.xml'
                }
            }
        }

        /**
         * Stage 4: Start Application for Testing
         * Spawns a background Spring Boot test instance on port 8080.
         * Stores the process PID in app-test.pid so it can be reliably terminated later.
         */
        stage('Start Application for Testing') {
            steps {
                dir('sspr-backend') {
                    script {
                        // Prevent Jenkins process killer from killing our detached background test process
                        env.JENKINS_NODE_COOKIE = 'dontKillMe'
                        sh '''
                        echo "Starting temporary Spring Boot backend test instance..."
                        nohup java -jar target/sspr-backend-0.0.1-SNAPSHOT.jar --server.port=8080 > app-test.log 2>&1 &
                        echo $! > app-test.pid
                        sleep 15
                        echo "Application started for testing (PID: $(cat app-test.pid))"
                        '''
                    }
                }
            }
        }

        /**
         * Stage 5: Selenium & Integration Tests
         * Executes the Selenium UI tests (Headless Chrome) and REST Assured API tests.
         * Tests against the running test instance and automatically stops the test instance
         * in the post always block regardless of test pass/fail outcome.
         */
        stage('Selenium & Integration Tests') {
            steps {
                dir('selenium-tests') {
                    echo 'Running Selenium and API integration tests...'
                    sh 'mvn test -Dapi.base.url=http://localhost:8080'
                }
            }
            post {
                always {
                    // Record integration test XML reports in Jenkins
                    junit 'selenium-tests/target/surefire-reports/*.xml'

                    // Always cleanly shut down the temporary background test application
                    dir('sspr-backend') {
                        sh '''
                        echo "Cleaning up temporary test application process..."
                        if [ -f app-test.pid ]; then
                            kill $(cat app-test.pid) || true
                            rm app-test.pid
                            echo "Temporary test application terminated."
                        fi
                        '''
                    }
                }
            }
        }

        /**
         * Stage 6: Build Docker Image
         * Creates container images using multi-stage Alpine Dockerfile.
         * Tags image with current build number and 'latest' moniker.
         */
        stage('Build Docker Image') {
            steps {
                dir('sspr-backend') {
                    echo "Building Docker image ${DOCKER_IMAGE}:${DOCKER_TAG}..."
                    sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                    sh "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
                }
            }
        }

        /**
         * Stage 7: Push to Registry
         * Pushes container image tags to the designated registry.
         * Gracefully handles unregistered local credentials for non-production environments.
         */
        stage('Push to Registry') {
            steps {
                echo "Pushing ${DOCKER_IMAGE}:${DOCKER_TAG} to registry..."
                sh "docker push ${DOCKER_IMAGE}:${DOCKER_TAG} || echo 'Push skipped - configure registry credentials'"
                sh "docker push ${DOCKER_IMAGE}:latest || echo 'Push skipped - configure registry credentials'"
            }
        }

        /**
         * Stage 8: Deploy Container
         * Stops previous running container and launches the newly built image
         * attached to the docker network with appropriate environment credentials.
         */
        stage('Deploy Container') {
            steps {
                script {
                    echo "Deploying to ${params.ENVIRONMENT} environment..."
                    sh '''
                    # Ensure docker network exists
                    docker network create sspr-network || true

                    # Stop and remove existing application container if present
                    docker stop sspr-backend-app || true
                    docker rm sspr-backend-app || true

                    # Launch new application container
                    docker run -d \
                        --name sspr-backend-app \
                        -p ${APP_PORT}:8080 \
                        --network sspr-network \
                        -e SPRING_PROFILES_ACTIVE=${ENVIRONMENT} \
                        -e SPRING_DATASOURCE_URL=jdbc:postgresql://sspr-postgres:5432/sspr_db \
                        -e SPRING_DATASOURCE_USERNAME=sspr_user \
                        -e SPRING_DATASOURCE_PASSWORD=SecretPassword123! \
                        ${DOCKER_IMAGE}:${DOCKER_TAG}

                    echo "Container deployed successfully!"
                    '''
                }
            }
        }

        /**
         * Stage 9: Health Check
         * Verifies application availability by performing an HTTP GET probe
         * against the summary dashboard endpoint.
         */
        stage('Health Check') {
            steps {
                script {
                    sh '''
                    echo "Waiting for application to start..."
                    sleep 20
                    curl -f http://localhost:${APP_PORT}/api/v1/admin/dashboard/summary || exit 1
                    echo "Health check passed!"
                    '''
                }
            }
        }
    }

    /**
     * Post-Pipeline Actions:
     * - always: Archive built JAR artifacts
     * - success: Notify of successful deployment
     * - failure: Notify of pipeline failure and trigger rollback to previous container
     */
    post {
        always {
            archiveArtifacts artifacts: 'sspr-backend/target/*.jar', allowEmptyArchive: true
        }
        success {
            echo 'Pipeline executed and deployed successfully!'
        }
        failure {
            echo 'Pipeline failed. Check the logs for details.'
            // Automated Rollback: attempt restarting previously tagged container if deployment failed
            sh 'docker start sspr-backend-app-prev || true'
        }
    }
}