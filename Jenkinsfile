pipeline {
    agent any
    
    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        JAVA_HOME = tool 'JDK21'
    }
    
    tools {
        maven 'Maven3'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean install -pl customer -am -DskipTests'
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test jacoco:report -pl customer -am'
            }
            post {
                always {
                    // Upload test results
                    junit testResults: 'customer/**/surefire-reports/TEST-*.xml', allowEmptyResults: true
                    
                    // Upload code coverage report
                    jacoco(
                        execPattern: 'customer/target/jacoco.exec',
                        classPattern: 'customer/target/classes',
                        sourcePattern: 'customer/src/main/java',
                        exclusionPattern: '**/*Test*.class'
                    )
                    
                    // Publish HTML coverage report
                    publishHTML([
                        allowMissing: true,  // Đổi sang true để không fail nếu không có report
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'customer/target/site/jacoco',
                        reportFiles: 'index.html',
                        reportName: 'Customer Coverage Report',
                        reportTitles: 'Code Coverage Report'
                    ])
                }
            }
        }
    }
    
    post {
        success {
            echo 'Customer Service CI Pipeline completed successfully!'
        }
        failure {
            echo 'Customer Service CI Pipeline failed!'
        }
    }
}