#!/usr/bin/env bash
set -x

CMD='mvn'
while getopts ":dH" opt; do
  case ${opt} in
    d)
      CMD='mvnDebug'
      echo 'will run mvnDebug'
      ;;
    H)
      echo "========   HELP   ========="
      echo "-d  : run mavenDebug instead"
      echo "-H  : display help"
      exit
      ;;
    \?)
      echo "Invalid option: -${OPTARG}. Use -H for help" >&2
      exit 1
      ;;
  esac
done
${CMD} org.jenkins-ci.tools:maven-hpi-plugin:run