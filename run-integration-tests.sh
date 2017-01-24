#!/usr/bin/env bash

function usage {
    echo "Usage: source ./$( basename "$( readlink -f "${BASH_SOURCE[0]}" )" ) [-s <maven_settings_file>]"
}

unset MVN_SETTINGS_FILE_LOC

while getopts ':s:' option; do
    case "$option" in
        s)
            export MVN_SETTINGS_FILE_LOC="$OPTARG"
            ;;
        '?' | ':')
            usage
            return 2
            ;;
    esac
done

unset PORT_OFFSET
source ./set-env-local.sh

if [ -z "$MVN_SETTINGS_FILE_LOC" ]; then
    mvn clean package -D skipTests
    cd acs-integration-tests
    mvn clean verify -P public
    cd -
else
    mvn clean package -D skipTests -s "$MVN_SETTINGS_FILE_LOC"
    cd acs-integration-tests
    mvn clean verify -P public -s "../${MVN_SETTINGS_FILE_LOC}"
    cd -
fi