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

{ set -e; } 2> /dev/null

function usage {
    echo "Usage: $( python -c "import os; print os.path.abspath('${BASH_SOURCE[0]}')" ) [-h | -help | --help] [--debug] [-s <path_to_mvn_settings_file>]"
}

function read_args {
    while (( "$#" )); do
        case "$1" in
            -h|-help|--help)
                usage
                exit 0
                ;;
            --debug)
                DEBUG='true'
                ;;
            -s)
                shift
                MVN_SETTINGS_FILE_PATH="$1"
                ;;
            *)
                echo "Unknown option: ${1}"
                usage
                exit 2
                ;;
        esac
        shift
    done
}

function archive_artifact_sources {
    MVN_COMMAND="mvn dependency:get -D groupId=$1 -D artifactId=$2 -D packaging=$3 -D version=$4 -D transitive=false -D maven.repo.local=$5"

    local GRAB_SOURCES="$6"
    if [[ "$GRAB_SOURCES" == 'true' ]]; then
        MVN_COMMAND="${MVN_COMMAND} -D classifier=sources"
    fi

    if [[ -n "$MVN_SETTINGS_FILE_PATH" ]]; then
        MVN_COMMAND="${MVN_COMMAND} -s ${MVN_SETTINGS_FILE_PATH}"
    fi

    { set +e; } 2> /dev/null
    eval "$MVN_COMMAND"
    { set -e; } 2> /dev/null
}

function build_source_artifact {
    MVN_COMMAND='mvn source:jar'
    if [[ -n "$MVN_SETTINGS_FILE_PATH" ]]; then
        MVN_COMMAND="${MVN_COMMAND} -s ${MVN_SETTINGS_FILE_PATH}"
    fi
    eval "$MVN_COMMAND"
}

function install_artifact_file {
    MVN_COMMAND="mvn install:install-file -D groupId=$1 -D artifactId=$2 -D packaging=$3 -D version=$4 -D classifier=sources -D localRepositoryPath=$5 -D file=${6}"
    if [[ -n "$MVN_SETTINGS_FILE_PATH" ]]; then
        MVN_COMMAND="${MVN_COMMAND} -s ${MVN_SETTINGS_FILE_PATH}"
    fi
    eval "$MVN_COMMAND"
}

unset DEBUG
unset MVN_SETTINGS_FILE_PATH

read_args "$@"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [[ -n "$MVN_SETTINGS_FILE_PATH" ]]; then
    MVN_SETTINGS_FILE_PATH="$( python -c "import os; print os.path.abspath('${MVN_SETTINGS_FILE_PATH}')" )"
fi

if [[ -n "$DEBUG" ]]; then
    echo 'The following options are set:'
    echo "  DEBUG: ${DEBUG}"
    echo "  DIR: ${DIR}"
    echo "  MVN_SETTINGS_FILE_PATH: ${MVN_SETTINGS_FILE_PATH}"
    echo ''
fi

LOCAL_MAVEN_REPO="${DIR}/dependency_sources"
mkdir -p "$LOCAL_MAVEN_REPO"

TITAN_ARTIFACT_PREFIX='com.thinkaurelius.titan:titan'
THRIFT_ARTIFACT_PREFIX='org.apache.thrift:libthrift'
SWAGGER_UI_ARTIFACT_PREFIX='io.springfox:springfox-swagger-ui'

# NOTE: These dependencies either don't have a corresponding sources JAR or are not found in the standard location in Maven Central so they need to be manually downloaded
JSON_LIB_ARTIFACT_PREFIX='net.sf.json-lib:json-lib'
SERVICEMIX_ARTIFACT_PREFIX='org.apache.servicemix.bundles:org.apache.servicemix.bundles.commons-csv'
BSH_ARTIFACT_PREFIX='org.beanshell:bsh'

MVN_DEPENDENCY_TREE="$( mvn dependency:tree | grep '^\[INFO\] [|+]' | \sed 's/\[INFO\] [-\+| ]*//' | sort -u )"
for a in $( echo "$MVN_DEPENDENCY_TREE" | grep -v "com\.ge\.predix:acs-\|org\.springframework\.boot:spring-boot-starter-\|${TITAN_ARTIFACT_PREFIX}\|${THRIFT_ARTIFACT_PREFIX}\|${SWAGGER_UI_ARTIFACT_PREFIX}\|${SERVICEMIX_ARTIFACT_PREFIX}\|${BSH_ARTIFACT_PREFIX}\|${JSON_LIB_ARTIFACT_PREFIX}" ); do
    GROUP_ID=$( echo "$a" | awk -F ':' '{print $1}' )
    ARTIFACT_ID=$( echo "$a" | awk -F ':' '{print $2}' )
    PACKAGING=$( echo "$a" | awk -F ':' '{print $3}' )

    if [[ -n "$( echo "$a" | awk -F ':' '{print $6}' )" ]]; then
        VERSION=$( echo "$a" | awk -F ':' '{print $5}' )
    else
        VERSION=$( echo "$a" | awk -F ':' '{print $4}' )
    fi

    archive_artifact_sources "$GROUP_ID" "$ARTIFACT_ID" "$PACKAGING" "$VERSION" "$LOCAL_MAVEN_REPO" 'true'

    if [[ $( find "${LOCAL_MAVEN_REPO}" -name "${ARTIFACT_ID}-${VERSION}-sources.jar" | grep -q '.'; echo "$?" ) -ne 0 ]]; then
        ARTIFACTS_WITHOUT_SOURCES="${ARTIFACTS_WITHOUT_SOURCES} ${a}"
        echo -e "\nCouldn't download sources for artifact: ${a}\n"
    else
        echo -e "\nSuccessfully downloaded sources for artifact: ${a}\n"
    fi
done

# Download Thrift-related source JARs
for a in $( echo "$MVN_DEPENDENCY_TREE" | grep "$THRIFT_ARTIFACT_PREFIX" ); do
    GROUP_ID=$( echo "$a" | awk -F ':' '{print $1}' )
    ARTIFACT_ID=$( echo "$a" | awk -F ':' '{print $2}' )
    PACKAGING=$( echo "$a" | awk -F ':' '{print $3}' )
    VERSION=$( echo "$a" | awk -F ':' '{print $4}' )

    curl -L "https://repo.maven.apache.org/maven2/$( echo "$GROUP_ID" | tr '.' '/' )/${ARTIFACT_ID}/${VERSION}/${ARTIFACT_ID}-${VERSION}-sources.src" -o "${ARTIFACT_ID}-${VERSION}-sources.jar"
    install_artifact_file "$GROUP_ID" "$ARTIFACT_ID" "$PACKAGING" "$VERSION" "$LOCAL_MAVEN_REPO" "${ARTIFACT_ID}-${VERSION}-sources.jar"

    if [[ $( find "${LOCAL_MAVEN_REPO}" -name "${ARTIFACT_ID}-${VERSION}-sources.jar" | grep -q '.'; echo "$?" ) -ne 0 ]]; then
        echo -e "\nCouldn't download sources for artifact: ${a}\n"
    else
        echo -e "\nSuccessfully downloaded sources for artifact: ${a}\n"
    fi

    rm -f "${ARTIFACT_ID}-${VERSION}-sources.jar"
done

# Download Titan-related source JARs
git clone https://github.com/thinkaurelius/titan.git
cd titan
git checkout titan11

for a in $( echo "$MVN_DEPENDENCY_TREE" | grep "$TITAN_ARTIFACT_PREFIX" ); do
    GROUP_ID=$( echo "$a" | awk -F ':' '{print $1}' )
    ARTIFACT_ID=$( echo "$a" | awk -F ':' '{print $2}' )
    PACKAGING=$( echo "$a" | awk -F ':' '{print $3}' )
    VERSION=$( echo "$a" | awk -F ':' '{print $4}' )

    cd "$ARTIFACT_ID"
    build_source_artifact
    install_artifact_file "$GROUP_ID" "$ARTIFACT_ID" "$PACKAGING" "$VERSION" "$LOCAL_MAVEN_REPO" "target/${ARTIFACT_ID}-${VERSION}-sources.jar"
    cd ..

    if [[ $( find "${LOCAL_MAVEN_REPO}" -name "${ARTIFACT_ID}-${VERSION}-sources.jar" | grep -q '.'; echo "$?" ) -ne 0 ]]; then
        echo -e "\nCouldn't download sources for artifact: ${a}\n"
    else
        echo -e "\nSuccessfully downloaded sources for artifact: ${a}\n"
    fi
done

cd ..
rm -rf titan

ARTIFACTS_WITHOUT_SOURCES="${ARTIFACTS_WITHOUT_SOURCES} $( echo "$MVN_DEPENDENCY_TREE" | grep "$SWAGGER_UI_ARTIFACT_PREFIX" )"

for a in $ARTIFACTS_WITHOUT_SOURCES; do
    GROUP_ID=$( echo "$a" | awk -F ':' '{print $1}' )
    ARTIFACT_ID=$( echo "$a" | awk -F ':' '{print $2}' )
    PACKAGING=$( echo "$a" | awk -F ':' '{print $3}' )
    VERSION=$( echo "$a" | awk -F ':' '{print $4}' )

    archive_artifact_sources "$GROUP_ID" "$ARTIFACT_ID" "$PACKAGING" "$VERSION" "$LOCAL_MAVEN_REPO"

    if [[ $( find "${LOCAL_MAVEN_REPO}" -name "${ARTIFACT_ID}-${VERSION}.jar" | grep -q '.'; echo "$?" ) -ne 0 ]]; then
        echo -e "\nCouldn't download artifact: ${a} . Please check that it exists in any Maven repository.\n"
    else
        JAR_FILE="$( find "${LOCAL_MAVEN_REPO}" -name "${ARTIFACT_ID}-${VERSION}.jar" | head -1 )"
        JAR_FILE_DIRNAME="$( echo "$JAR_FILE" | xargs dirname )"
        SOURCES_JAR_FILE="${JAR_FILE_DIRNAME}/${ARTIFACT_ID}-${VERSION}-sources.jar"
        cp -a "$JAR_FILE" "$SOURCES_JAR_FILE"
        echo -e "\nSuccessfully downloaded artifact: ${a} . A copy of the artifact with sources is located at ${SOURCES_JAR_FILE}\n"
    fi
done
