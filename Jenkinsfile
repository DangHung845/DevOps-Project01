pipeline {
    agent any

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        DEFAULT_BASE_BRANCH = 'main'
        // Quality gates
        COVERAGE_MIN_LINE = '0.70'
        // Tooling defaults (can be overridden at job level)
        SONARQUBE_ENV = 'SonarQube'
        SNYK_TOKEN_CRED_ID = 'snyk-token'
    }

    tools {
        jdk 'JDK21'
        maven 'Maven3'
    }

    options {
        timestamps()
        disableConcurrentBuilds()   
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Gitleaks (secrets scan)') {
            steps {
                script {
                    // Run local gitleaks CLI if available; otherwise, skip without failing the build
                    def hasGitleaks = sh(script: 'command -v gitleaks >/dev/null 2>&1', returnStatus: true) == 0
                    if (!hasGitleaks) {
                        echo 'gitleaks CLI not found on agent, skipping secrets scan.'
                        return
                    }

                    sh 'gitleaks detect --source=. --redact --verbose'
                }
            }
        }

        stage('Detect changed modules') {
            steps {
                script {
                    // Always fetch all remote branches so origin/main exists locally
                    sh "git fetch --no-tags --prune origin +refs/heads/*:refs/remotes/origin/*"

                    // 1) Choose base for diff
                    // - PR build: compare with PR target branch (usually main)
                    // - Branch build: compare with origin/main
                    def baseRef = ''
                    if (env.CHANGE_ID) {
                        baseRef = "origin/${env.CHANGE_TARGET ?: env.DEFAULT_BASE_BRANCH}"
                    } else {
                        baseRef = "origin/${env.DEFAULT_BASE_BRANCH}"
                    }

                    // 2) Diff and collect changed files
                    def diffCmd = "git diff --name-only ${baseRef}...HEAD"
                    def changedFilesRaw = sh(script: diffCmd, returnStdout: true).trim()
                    def changedFiles = changedFilesRaw ? changedFilesRaw.split('\n') : []

                    echo "BRANCH_NAME: ${env.BRANCH_NAME}"
                    echo "Base for diff: ${baseRef}"
                    echo "origin/${env.DEFAULT_BASE_BRANCH}: " + sh(script: "git rev-parse ${baseRef}", returnStdout: true).trim()
                    echo "HEAD: " + sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    echo "Changed files:\n- " + (changedFiles ? changedFiles.join("\n- ") : "(none)")

                    // 3) Declare Maven modules (folder names)
                    def modules = [
                        'customer',
                        'cart',
                        'order',
                        'product',
                        'tax',
                        'media',
                        'search',
                        'webhook',
                        'common-library'
                    ]

                    // 4) Decide impacted modules (Option A: ONLY folder-based changes)
                    def impacted = modules.findAll { m ->
                        changedFiles.any { f -> f.startsWith("${m}/") }
                    }

                    if (impacted.isEmpty()) {
                        echo "No impacted modules detected (only service-folder changes are considered). Marking build as NOT_BUILT."
                        currentBuild.result = 'NOT_BUILT'
                        env.IMPACTED_MODULES = ''
                    } else {
                        env.IMPACTED_MODULES = impacted.join(',')
                        echo "Impacted modules: ${env.IMPACTED_MODULES}"
                    }
                }
            }
        }

        stage('Build impacted modules') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                script {
                    def mods = env.IMPACTED_MODULES.split(',') as List
                    def pl = mods.join(',')
                    sh "mvn -B clean install -pl ${pl} -am -DskipTests"
                }
            }
        }

        stage('Test impacted modules') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                script {
                    def mods = env.IMPACTED_MODULES.split(',') as List
                    def pl = mods.join(',')
                    // Ensure JaCoCo reports are generated even when tests fail
                    sh "mvn -B test jacoco:report -pl ${pl} -am -Dmaven.test.failure.ignore=true"
                }
            }
            post {
                always {
                    // Mark build UNSTABLE if tests failed, but keep pipeline running
                    junit testResults: '**/target/surefire-reports/TEST-*.xml', allowEmptyResults: true

                    script {
                        def mods = (env.IMPACTED_MODULES?.trim() ? env.IMPACTED_MODULES.split(',') : []) as List
                        mods.each { m ->
                            jacoco(
                                execPattern: "${m}/target/jacoco.exec",
                                classPattern: "${m}/target/classes",
                                sourcePattern: "${m}/src/main/java",
                                exclusionPattern: '**/*Test*.class'
                            )

                            publishHTML([
                                allowMissing: true,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: "${m}/target/site/jacoco",
                                reportFiles: 'index.html',
                                reportName: "${m} Coverage Report",
                                reportTitles: "Code Coverage Report (${m})"
                            ])
                        }
                    }
                }
            }
        }

        stage('SonarQube (code quality)') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    script {
                        def mods = env.IMPACTED_MODULES.split(',') as List
                        def pl = mods.join(',')
                        withSonarQubeEnv(env.SONARQUBE_ENV) {
                            sh """
                                mvn -B sonar:sonar \
                                  -pl ${pl} -am \
                                  -Dsonar.organization=devops-org-newnol \
                                  -Dsonar.projectKey=devops-org-newnol_devops-org-newnol \
                                  -Dsonar.projectName="devops-org-newnol"
                            """.stripIndent()
                        }
                    }
                }
            }
        }

        stage('Snyk (dependency vulnerabilities)') {
            steps {
                script {
                    // Run local snyk CLI if available; otherwise, skip without failing the build
                    def hasSnyk = sh(script: 'command -v snyk >/dev/null 2>&1', returnStatus: true) == 0
                    if (!hasSnyk) {
                        echo 'snyk CLI not found on agent, skipping vulnerability scan.'
                        return
                    }

                    withCredentials([string(credentialsId: env.SNYK_TOKEN_CRED_ID, variable: 'SNYK_TOKEN')]) {
                        sh '''
                            export SNYK_TOKEN="${SNYK_TOKEN}"
                            snyk test --all-projects
                        '''
                    }
                }
            }
        }

        stage('Coverage gate (> 70%)') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                script {
                    def minLine = (env.COVERAGE_MIN_LINE ?: '0.70') as BigDecimal
                    def mods = env.IMPACTED_MODULES.split(',') as List

                    def failures = []
                    mods.each { m ->
                        def reportPath = "${m}/target/site/jacoco/jacoco.xml"
                        if (!fileExists(reportPath)) {
                            failures << "${m}: missing ${reportPath}"
                            return
                        }

                        // Use default XmlSlurper constructor so it is allowed in Jenkins sandbox
                        def xml = new XmlSlurper().parseText(readFile(reportPath))
                        def lineCounter = xml.counter.find { it.@type?.toString() == 'LINE' }
                        if (!lineCounter) {
                            failures << "${m}: LINE counter not found in jacoco.xml"
                            return
                        }

                        def missed = (lineCounter.@missed?.toString() ?: '0') as BigDecimal
                        def covered = (lineCounter.@covered?.toString() ?: '0') as BigDecimal
                        def total = missed + covered
                        def ratio = total > 0 ? (covered / total) : 0

                        echo "Coverage (LINE) ${m}: ${(ratio * 100).setScale(2, java.math.RoundingMode.HALF_UP)}%"
                        if (ratio <= minLine) {
                            failures << "${m}: ${(ratio * 100).setScale(2, java.math.RoundingMode.HALF_UP)}% <= ${(minLine * 100).setScale(0, java.math.RoundingMode.HALF_UP)}%"
                        }
                    }

                    if (!failures.isEmpty()) {
                        error "Coverage gate failed (LINE must be > ${(minLine * 100).setScale(0, java.math.RoundingMode.HALF_UP)}%).\\n- " + failures.join("\\n- ")
                    }
                }
            }
        }
    }

    post {
        success { echo 'Monorepo CI Pipeline completed successfully!' }
        failure { echo 'Monorepo CI Pipeline failed!' }
    }
}