pipeline {
    agent any

    options {
        timestamps()
        // Disable default checkout to manage workspace manually for a clean build
        skipDefaultCheckout(true)
    }

    environment {
        // --- CONSTANTS ---
        DOCKERHUB_NAMESPACE    = "danghung845"
        DOCKERHUB_CREDENTIALS  = "dockerhub-creds"
        
        // List of all YAS microservices to be processed
        SERVICES_LIST          = "customer,cart,order,product,tax,media,search,rating,location,inventory"
    }

    tools { 
        // Ensure these names match your "Global Tool Configuration" in Jenkins Local
        maven 'Maven3' 
        jdk   'JDK25' 
    }

    stages {
        stage('Checkout & Metadata') {
            steps {
                // Clean the workspace before starting
                deleteDir()
                checkout scm
                script {
                    // Requirement #3: Capture the 12-character Git Commit ID for tagging
                    env.GIT_SHA = sh(script: "git rev-parse --short=12 HEAD", returnStdout: true).trim()
                    echo "Global Tag for this build session: ${env.GIT_SHA}"
                }
            }
        }

        stage('Maven Build All') {
            steps {
                echo "Compiling and installing all YAS modules..."
                // Build all modules from the root pom.xml
                sh "mvn -B clean install -DskipTests"
            }
        }

        stage('Dockerize & Push to Hub') {
            steps {
                script {
                    def services = env.SERVICES_LIST.split(',')
                    
                    // Use the DH_USER and DH_PASS injected from Jenkins Credentials
                    withCredentials([usernamePassword(
                        credentialsId: "${DOCKERHUB_CREDENTIALS}", 
                        usernameVariable: 'DH_USER', 
                        passwordVariable: 'DH_PASS'
                    )]) {
                        // Execution of secure Docker login
                        sh 'echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin'
                        
                        services.each { svc ->
                            dir(svc) {
                                echo "Building Docker image for service: ${svc}"
                                def fullImageName = "${DOCKERHUB_NAMESPACE}/yas-${svc}:${env.GIT_SHA}"
                                
                                // Build using the Dockerfile in each sub-folder
                                sh "docker build -t ${fullImageName} ."
                                sh "docker push ${fullImageName}"
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            // Logout to secure the local environment
            sh 'docker logout || true'
            script {
                // Remove local images to save disk space on your local machine
                def services = env.SERVICES_LIST.split(',')
                services.each { svc ->
                    sh "docker rmi ${DOCKERHUB_NAMESPACE}/yas-${svc}:${env.GIT_SHA} || true"
                }
            }
        }
        success {
            echo "Successfully built and pushed 10 microservices to Docker Hub under namespace: ${DOCKERHUB_NAMESPACE}"
        }
    }
}