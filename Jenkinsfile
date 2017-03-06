#!/usr/bin/env groovy

boolean onJenkinsCI = false
node {
  println env.JENKINS_URL
  println env.JENKINS_URL.startWith('https://ci.jenkins.io')
  onJenkinsCI = env.JENKINS_URL && env.JENKINS_URL.startWith('https://ci.jenkins.io'
}

if (onJenkinsCI) {
  /* running on ci.jenkins.io, we will have `buildPlugin` step provided by: https://github.com/jenkins-infra/pipeline-library */
  buildPlugin()
  return
}


@Library('github.com/zanata/zanata-pipeline-library@master')

/* Only keep the 10 most recent builds. */
def projectProperties = [
  [
    $class: 'BuildDiscarderProperty',
    strategy: [$class: 'LogRotator', numToKeepStr: '10']
  ],
]

properties(projectProperties)

/* Upto stash stage should fail fast:
 * Failed and stop the build
 * Yet able to create report
 */
try {
  timestamps {
    node {
      ansicolor {
        stage('Checkout') {
          info.printNode()
          notify.started()

          // Shallow Clone does not work with RHEL7, which use git-1.8.3
          // https://issues.jenkins-ci.org/browse/JENKINS-37229
          checkout scm
        }

        // Build and Unit Tests
        // The result is archived
        stage('Build') {
          info.printNode()
          info.printEnv()
          def testReports = 'target/surefire-reports/TEST-*.xml'
          def hpiFiles = 'target/*.hpi'

          withEnv(["MVN_HOME=${ tool name: 'mvn', type: 'hudson.tasks.Maven$MavenInstallation' }"]) {
            sh '$MVN_HOME/bin/mvn clean verify'
          }

          junit allowEmptyResults: true,
            keepLongStdio: false,
            testDataPublishers: [[$class: 'StabilityTestDataPublisher']],
            testResults: "**/${testReports}"

          // notify if compile+unit test successful
          notify.testResults(null)
          archive "**/${hpiFiles},**/target/site/jacoco/**"
        }

        stage('Report') {
          // this is not working properly yet
          // step([$class: 'JacocoPublisher'])
        }
      }
    }
  }
} catch (e) {
  notify.failed()
  junit allowEmptyResults: true,
      keepLongStdio: false,
      testDataPublishers: [[$class: 'StabilityTestDataPublisher']],
      testResults: "**/${testReports}"
  throw e
}