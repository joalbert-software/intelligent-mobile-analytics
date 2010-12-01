pipeline {
    agent any
    tools {
        jdk 'java8'
        maven 'maven3'
    }
    options {
        timestamps()
        disableConcurrentBuilds()
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Java verification') {
            steps {
                sh 'mvn --batch-mode clean verify'
            }
        }
        stage('Python verification') {
            steps {
                sh 'PYTHONPATH=batch python -m unittest discover -s batch/tests -v'
            }
        }
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'target/*.war', fingerprint: true
            }
        }
    }
    post {
        always {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
        }
    }
}
