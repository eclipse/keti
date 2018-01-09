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

echo "JAVA_LOCAL_OPTS: ${JAVA_LOCAL_OPTS}"

HTTP_VALIDATION_SPRING_PROFILE='httpValidation'
if [[ -z "$SPRING_PROFILES_ACTIVE" ]]; then
    export SPRING_PROFILES_ACTIVE="$HTTP_VALIDATION_SPRING_PROFILE"
elif [[ "$SPRING_PROFILES_ACTIVE" != *"$HTTP_VALIDATION_SPRING_PROFILE"* ]]; then
    export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE},${HTTP_VALIDATION_SPRING_PROFILE}"
fi
echo "SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}"

unset PORT_OFFSET
source ./set-env-local.sh

export DIR=$( dirname "$( python -c "import os; print os.path.abspath('${BASH_SOURCE[0]}')" )" )

if [ "$#" -eq 0 ]; then
    unset JAVA_DEBUG_OPTS
    unset LOGGING_OPTS
fi

main() {
    while [ "$1" != '' ]; do
        case $1 in
            'debug')
                JAVA_DEBUG_OPTS='-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005'
                shift
                ;;
            'human-readable-logging')
                LOGGING_OPTS="-Dlog4j.debug -Dlog4j.configuration=file:${DIR}/src/main/resources/log4j-dev.xml"
                shift
                ;;
            *)
                break
                ;;
        esac
    done

    cp "${DIR}"/target/acs-service-*-exec.jar "${DIR}"/.acs-service-copy.jar
    java -Xms1g -Xmx1g $JAVA_LOCAL_OPTS $JAVA_DEBUG_OPTS $LOGGING_OPTS $PROXY_OPTS -jar "${DIR}"/.acs-service-copy.jar
}

main "$@"
