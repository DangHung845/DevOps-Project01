pipeline {
    agent any
    
    stages {
        stage('Service A - Test') {
            steps {
                echo "Running TEST for service-a"
            }
        }

        stage('Service A - Build') {
            steps {
                echo "Running BUILD for service-a"
            }
        }

        stage('Service B - Test') {
            steps {
                echo "Running TEST for service-b"
            }
        }

        stage('Service B - Build') {
            steps {
                echo "Running BUILD for service-b"
            }
        }
    }
}
