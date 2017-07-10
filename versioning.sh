#!/usr/bin/env bash

set -ex

if [[ -z "$1" ]]; then
    echo "Please provide the version to set in all POM files"
    exit 2
fi

XMLSTARLET_VERSION='1.6.1'
XMLSTARLET_DIRNAME="xmlstarlet-${XMLSTARLET_VERSION}"
XMLSTARLET_ARCHIVE_NAME="${XMLSTARLET_DIRNAME}.tar.gz"
if [[ ! -f "${XMLSTARLET_DIRNAME}/xml" ]]; then
    curl -OL "https://downloads.sourceforge.net/project/xmlstar/xmlstarlet/${XMLSTARLET_VERSION}/${XMLSTARLET_ARCHIVE_NAME}"
    tar -xvzf "${XMLSTARLET_ARCHIVE_NAME}"
    cd "$XMLSTARLET_DIRNAME"
    ./configure && make
else
    cd "$XMLSTARLET_DIRNAME"
fi

./xml ed -P -L -N x='http://maven.apache.org/POM/4.0.0' -u '/x:project/x:version' -v "$1" '../pom.xml'

for f in 'commons/pom.xml' 'model/pom.xml' 'service/pom.xml' 'acs-integration-tests/pom.xml'; do
    ./xml ed -P -L -N x='http://maven.apache.org/POM/4.0.0' -u '/x:project/x:parent/x:version' -v "$1" "../${f}"
done

./xml ed -P -L -N x='http://maven.apache.org/POM/4.0.0' -u '/x:project/x:dependencies/x:dependency[x:artifactId="acs-service"]/x:version' -v "$1" '../acs-integration-tests/pom.xml'
