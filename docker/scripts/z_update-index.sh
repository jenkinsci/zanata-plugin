#!/bin/bash

# -q quiet -O output to file (- means stdin)
# sed '1d;$d' means remove first and last line
wget http://updates.jenkins-ci.org/update-center.json -qO- | sed '1d;$d'  > ${JENKINS_HOME}/updates/default.json

