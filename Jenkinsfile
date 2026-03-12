pipeline {
  agent any

  parameters {
    booleanParam(
      name: 'FAST_SECURITY_ONLY',
      defaultValue: true,
      description: 'Skip Maven build/test. Run only Gitleaks + Sonar + Snyk on impacted modules.'
    )
  }

  environment {
    MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
    JAVA_HOME = tool 'JDK21'
    DEFAULT_BASE_BRANCH = 'main'

    // SonarCloud settings
    SONAR_HOST_URL    = 'https://sonarcloud.io'
    SONAR_ORG         = 'wan172005'
    SONAR_PROJECT_KEY = 'Wan172005_DevOps-Project01'

    // Tools cached inside workspace
    TOOLS_DIR = "${WORKSPACE}/.tools"
  }

  tools { maven 'Maven3' }

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Detect changed modules') {
      steps {
        script {
          sh "git fetch --no-tags --prune origin +refs/heads/*:refs/remotes/origin/*"

          def baseRef = env.CHANGE_ID
            ? "origin/${env.CHANGE_TARGET ?: env.DEFAULT_BASE_BRANCH}"
            : "origin/${env.DEFAULT_BASE_BRANCH}"

          def changedFilesRaw = sh(script: "git diff --name-only ${baseRef}...HEAD", returnStdout: true).trim()
          def changedFiles = changedFilesRaw ? changedFilesRaw.split('\n') : []

          echo "BRANCH_NAME: ${env.BRANCH_NAME}"
          echo "Base for diff: ${baseRef}"
          echo "Changed files:\n- " + (changedFiles ? changedFiles.join("\n- ") : "(none)")

          def modules = [
            'customer','cart','order','product','tax','media','search','webhook','common-library'
          ]

          def impacted = modules.findAll { m -> changedFiles.any { f -> f.startsWith("${m}/") } }

          if (impacted.isEmpty()) {
            echo "No impacted modules detected."
            env.IMPACTED_MODULES = ''
            // Không set NOT_BUILT ở đây để vẫn cho phép pipeline kết thúc “clean”
            // (Nếu muốn behavior cũ thì bạn set currentBuild.result='NOT_BUILT')
          } else {
            env.IMPACTED_MODULES = impacted.join(',')
            echo "Impacted modules: ${env.IMPACTED_MODULES}"
          }
        }
      }
    }

    // ====== FAST SECURITY PATH ======
    // Chạy nhanh: chỉ chạy security tools dựa trên IMPACTED_MODULES

    stage('Gitleaks (secrets scan) - impacted modules (no docker)') {
      when { expression { return env.IMPACTED_MODULES?.trim() } }
      steps {
        sh '''
          set -euxo pipefail

          mkdir -p "$TOOLS_DIR"
          GITLEAKS_VERSION="8.18.4"

          if [ ! -x "$TOOLS_DIR/gitleaks" ]; then
            curl -sSL -o "$TOOLS_DIR/gitleaks.tgz" \
              "https://github.com/gitleaks/gitleaks/releases/download/v${GITLEAKS_VERSION}/gitleaks_${GITLEAKS_VERSION}_linux_x64.tar.gz"
            tar -xzf "$TOOLS_DIR/gitleaks.tgz" -C "$TOOLS_DIR" gitleaks
            chmod +x "$TOOLS_DIR/gitleaks"
          fi

          "$TOOLS_DIR/gitleaks" version

          # Tạo report JSON để archive ổn định
          "$TOOLS_DIR/gitleaks" detect \
            --source="." \
            --redact \
            --no-git \
            --report-format json \
            --report-path gitleaks-report.json \
            || true
        '''
      }
      post {
        always {
          archiveArtifacts artifacts: 'gitleaks-report.json', allowEmptyArchive: true
        }
      }
    }

    stage('Build impacted modules') {
      when {
        allOf {
          expression { return env.IMPACTED_MODULES?.trim() }
          expression { return !params.FAST_SECURITY_ONLY }
        }
      }
      steps {
        sh "mvn -B clean install -pl ${env.IMPACTED_MODULES} -am -DskipTests"
      }
    }

    stage('Test impacted modules') {
      when {
        allOf {
          expression { return env.IMPACTED_MODULES?.trim() }
          expression { return !params.FAST_SECURITY_ONLY }
        }
      }
      steps {
        sh "mvn -B test jacoco:report -pl ${env.IMPACTED_MODULES} -am"
      }
      post {
        always {
          junit testResults: '**/target/surefire-reports/TEST-*.xml',
                allowEmptyResults: true,
                skipMarkingBuildUnstable: true
        }
      }
    }

    stage('Sonar Scan (impacted modules)') {
      when { expression { return env.IMPACTED_MODULES?.trim() } }
      steps {
        script {
          def mods = env.IMPACTED_MODULES.split(',') as List

          withCredentials([string(credentialsId: 'sonar_token', variable: 'SONAR_TOKEN')]) {
            mods.each { m ->
              def sonarExtra = ""
              if (env.CHANGE_ID) {
                sonarExtra = """
                  -Dsonar.pullrequest.key=${env.CHANGE_ID} \
                  -Dsonar.pullrequest.branch=${env.CHANGE_BRANCH} \
                  -Dsonar.pullrequest.base=${env.CHANGE_TARGET ?: env.DEFAULT_BASE_BRANCH} \
                """
              } else {
                sonarExtra = "-Dsonar.branch.name=${env.BRANCH_NAME} \\"
              }

              sh """
                set -euxo pipefail
                mvn -B -DskipTests \
                  org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar \
                  -f ${m} \
                  -Dsonar.login="$SONAR_TOKEN" \
                  -Dsonar.host.url="${SONAR_HOST_URL}" \
                  -Dsonar.organization="${SONAR_ORG}" \
                  -Dsonar.projectKey="${SONAR_PROJECT_KEY}" \
                  ${sonarExtra}
              """
            }
          }
        }
      }
    }

    stage('Snyk (dependency vulnerabilities) - impacted modules') {
      when { expression { return env.IMPACTED_MODULES?.trim() } }
      options { timeout(time: 15, unit: 'MINUTES') }
      steps {
        withCredentials([string(credentialsId: 'snyk_token', variable: 'SNYK_TOKEN')]) {
          sh '''#!/usr/bin/env bash
            set -euxo pipefail
    
            mkdir -p "$TOOLS_DIR" snyk-reports
            SNYK_VERSION="1.1295.0"
    
            if [ ! -x "$TOOLS_DIR/snyk" ]; then
              curl -sSL -o "$TOOLS_DIR/snyk" \
                "https://github.com/snyk/cli/releases/download/v${SNYK_VERSION}/snyk-linux"
              chmod +x "$TOOLS_DIR/snyk"
            fi
    
            "$TOOLS_DIR/snyk" --version
            export SNYK_TOKEN="$SNYK_TOKEN"
    
            IFS=',' read -ra MODS <<< "$IMPACTED_MODULES"
            for m in "${MODS[@]}"; do
              echo "===== SNYK scanning module: $m ====="
              (cd "$m" && "$TOOLS_DIR/snyk" test --file=pom.xml --severity-threshold=high --json) \
                > "snyk-reports/snyk-${m}.json" || true
            done
          '''
        }
      }
      post {
        always {
          archiveArtifacts artifacts: 'snyk-reports/*.json', allowEmptyArchive: true
        }
      }
    }

    stage('No impacted modules') {
      when { expression { return !env.IMPACTED_MODULES?.trim() } }
      steps {
        echo 'No impacted modules => skipping build/test/security stages.'
      }
    }
  }

  post {
    success { echo 'Monorepo CI Pipeline completed successfully!' }
    failure { echo 'Monorepo CI Pipeline failed!' }
  }
}
