#!/bin/bash

java -jar ${JENKINS_HOME}/war/WEB-INF/jenkins-cli.jar -s http://localhost:8080/ restart

