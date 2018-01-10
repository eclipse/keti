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

function usage {
    echo "Usage: source ./$( basename "$( python -c "import os; print os.path.abspath('${BASH_SOURCE[0]}')" )" ) [-t] [-s <maven_settings_file>]"
}

unset MVN_SETTINGS_FILE_LOC
unset TITAN_SUFFIX

while getopts ':s:t' option; do
    case "$option" in
        s)
            export MVN_SETTINGS_FILE_LOC="$OPTARG"
            ;;
        t)
            export TITAN_SUFFIX='-titan'
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
    mvn clean package -P "public${TITAN_SUFFIX}" -D skipTests
    cd acs-integration-tests
    mvn clean verify -P "public${TITAN_SUFFIX}"
    cd -
else
    mvn clean package -P "public${TITAN_SUFFIX}" -D skipTests -s "$MVN_SETTINGS_FILE_LOC"
    cd acs-integration-tests
    mvn clean verify -P "public${TITAN_SUFFIX}" -s "../${MVN_SETTINGS_FILE_LOC}"
    cd -
fi