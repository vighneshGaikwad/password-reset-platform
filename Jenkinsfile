pipeline {
    agent any

    // Ensure this matches the exact name you gave Maven in Jenkins Global Tool Configuration
    tools {
        maven 'Maven-3.9' 
    }

    // Parameterizing the deployment environment and Tomcat port
    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'prod'], description: 'Deployment Environment')
        string(name: 'APP_PORT', defaultValue: '8080', description: 'Embedded Tomcat Server Port')
    }

    stages {
        stage('Checkout') {
            steps {
                // Pulls the latest code from the branch configured in the Jenkins job
                checkout scm
            }
        }

        stage('Build & Package') {
            steps {
                dir('sspr-backend') {
                    echo "Compiling and packaging the application..."
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Deploy to Embedded Tomcat') {
            steps {
                dir('sspr-backend') {
                    script {
                        // Prevent Jenkins from killing the background Java process after the job finishes
                        env.JENKINS_NODE_COOKIE = 'dontKillMe'
                        
                        echo "Deploying to ${params.ENVIRONMENT} environment on port ${params.APP_PORT}..."
                        
                        sh '''
                        # 1. Gracefully stop the previously deployed instance (if it exists)
                        if [ -f app.pid ]; then
                            echo "Stopping existing application..."
                            kill $(cat app.pid) || true
                            rm app.pid
                        fi
                        
                        # 2. Start the new JAR file with parameterized port and environment profile
                        nohup java -Dserver.port=${APP_PORT} -Dspring.profiles.active=${ENVIRONMENT} -jar target/sspr-backend-0.0.1-SNAPSHOT.jar > application.log 2>&1 &
                        
                        # 3. Save the new Process ID (PID) to a file for future deployments
                        echo $! > app.pid
                        
                        echo "Application successfully deployed to embedded Tomcat!"
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            // Archive the compiled artifact regardless of pipeline success/failure
            archiveArtifacts artifacts: 'sspr-backend/target/*.jar', allowEmptyArchive: true
        }
        success {
            echo "Pipeline executed and deployed successfully!"
        }
        failure {
            echo "Pipeline failed. Check the logs for details."
        }
    }
}