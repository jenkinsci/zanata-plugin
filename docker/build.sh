#!/usr/bin/env bash

# determine directory containing this script
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

PLUGIN=$(ls $DIR/../target/*.hpi)

if [ -f "$PLUGIN" ]
then
    # we have to link the file here so that docker build can copy the file into the image
    ln ${PLUGIN} ${DIR}/zanata.hpi
else
    echo please build the plugin first
    exit 1
fi

docker build -t zjenkins/dev ${DIR}

# clean up the file (only needed for docker build)
rm ${DIR}/zanata.hpi

