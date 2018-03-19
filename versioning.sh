################################################################################
# Copyright 2017 General Electric Company
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
################################################################################

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
