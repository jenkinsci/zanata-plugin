#!/usr/bin/env groovy

boolean onJenkinsCI = env.JENKINS_URL &&
    (env.JENKINS_URL == 'https://ci.jenkins.io/' ||
        env.JENKINS_URL == 'https://ci.jenkins.io')

if (onJenkinsCI) {
  /* running on ci.jenkins.io, we will have `buildPlugin` step provided by: https://github.com/jenkins-infra/pipeline-library */
  buildPlugin(['platforms': ['linux']])
  return
}

@Field
public static final String PROJ_URL = 'https://github.com/zanata/zanata-jenkins-plugin'

// Import pipeline library for utility methods & classes:
// ansicolor(), Notifier, PullRequests, Strings
@Field
public static final String PIPELINE_LIBRARY_BRANCH = 'ZNTA-2270-jenkins2'

@Field
public static final String testReports = 'target/surefire-reports/TEST-*.xml'


// GROOVY-3278:
//   Using referenced String constant as value of Annotation causes compile error
@Library('zanata-pipeline-library@ZNTA-2270-jenkins2')
import org.zanata.jenkins.Notifier
import org.zanata.jenkins.PullRequests
import org.zanata.jenkins.ScmGit

import groovy.transform.Field

PullRequests.ensureJobDescription(env, manager, steps)

@Field
def pipelineLibraryScmGit

@Field
def mainScmGit

@Field
def notify
// initialiser must be run separately (bindings not available during compilation phase)

// Define project properties: general properties for the Pipeline-defined jobs.
// 1. discard old artifacts and build logs
// 2. configure build parameters (eg requested labels for build nodes)
//
// Normally the project properties wouldn't go inside a node, but
// we need a node to access env.DEFAULT_NODE.
node {
  echo "running on node ${env.NODE_NAME}"
  pipelineLibraryScmGit = new ScmGit(env, steps, 'https://github.com/zanata/zanata-pipeline-library')
  pipelineLibraryScmGit.init(PIPELINE_LIBRARY_BRANCH)
  mainScmGit = new ScmGit(env, steps, PROJ_URL)
  mainScmGit.init(env.BRANCH_NAME)
  notify = new Notifier(env, steps, currentBuild,
    pipelineLibraryScmGit, mainScmGit, 'Jenkinsfile',
  )
  defaultNodeLabel = env.DEFAULT_NODE ?: 'master || !master'
  // eg github-zanata-org/zanata-platform/update-Jenkinsfile
  jobName = env.JOB_NAME
  def projectProperties = [
    [
      $class: 'BuildDiscarderProperty',
      strategy: [$class: 'LogRotator',
      daysToKeepStr: '30',       // keep records no more than X days
      numToKeepStr: '20',        // keep records for at most X builds
      artifactDaysToKeepStr: '', // keep artifacts no more than X days
      artifactNumToKeepStr: '4'] // keep artifacts for at most X builds
    ],
    [
      $class: 'GithubProjectProperty',
      projectUrlStr: PROJ_URL
    ],
    [
      $class: 'ParametersDefinitionProperty',
      parameterDefinitions: [
        [
          $class: 'LabelParameterDefinition',
          // Specify the default node in Jenkins env var DEFAULT_NODE
          // (eg kvm), or leave blank to build on any node.
          defaultValue: defaultNodeLabel,
          description: 'Jenkins node label to use for build',
          name: 'LABEL'
        ],
      ]
    ],
  ]

  properties(projectProperties)
}

String getLabel() {
  def labelParam = null
  try {
    labelParam = params.LABEL
  } catch (e1) {
    // workaround for https://issues.jenkins-ci.org/browse/JENKINS-38813
    echo '[WARNING] unable to access `params`'
    echo getStackTrace(e1)
    try {
      labelParam = LABEL
    } catch (e2) {
      echo '[WARNING] unable to access `LABEL`'
      echo getStackTrace(e2)
    }
  }

  if (labelParam == null) {
    echo "LABEL param is null; using default value."
  }
  def result = labelParam ?: defaultNodeLabel
  echo "Using build node label: $result"
  return result
}


/* Upto stash stage should fail fast:
 * Failed and stop the build
 * Yet able to create report
 */
timestamps {
  // allocate a node for build+unit tests
  node(getLabel()) {
    try {
      echo "running on node ${env.NODE_NAME}"
      ansicolor {
        stage('Checkout') {
          notify.started()

          // Shallow Clone does not work with RHEL7, which use git-1.8.3
          // https://issues.jenkins-ci.org/browse/JENKINS-37229
          checkout scm
        }

        // Build and Unit Tests
        // The result is archived
        stage('Build') {
          notify.startBuilding()
          def hpiFiles = 'target/*.hpi'

          sh "mvn clean verify"

          junit allowEmptyResults: true,
            keepLongStdio: false,
            testDataPublishers: [[$class: 'StabilityTestDataPublisher']],
            testResults: "**/${testReports}"

          // notify if compile+unit test successful
          notify.testResults(null)
          archive "**/${hpiFiles}"
        }

        stage('Report') {
           step([$class: 'JacocoPublisher'])
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
  }
}
