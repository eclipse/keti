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
################################################################################

#!/usr/bin/env bash

export PORT_OFFSET=${PORT_OFFSET:-0}
export UAA_LOCAL_PORT=$(( 8080 + ${PORT_OFFSET} ))
export ACS_LOCAL_PORT=$(( 8181 + ${PORT_OFFSET} ))
export ZAC_LOCAL_PORT=$(( 8888 + ${PORT_OFFSET} ))

export ACS_URL="http://localhost:${ACS_LOCAL_PORT}"
export ZAC_URL="http://localhost:${ZAC_LOCAL_PORT}"
export ACS_TESTING_UAA="http://localhost:${UAA_LOCAL_PORT}/uaa"

export ENCRYPTION_KEY=1234567890123456

if [[ "$SPRING_PROFILES_ACTIVE" != *'predix'* ]]; then
    export ZAC_UAA_URL="http://localhost:${UAA_LOCAL_PORT}/uaa"
    export ACS_UAA_URL="http://localhost:${UAA_LOCAL_PORT}/uaa"
fi

python <<EOF
import re, shutil

input_file = './acs-integration-tests/uaa/config/uaa.yml'
output_file = input_file + '.tmp'

input = open(input_file)
output = open(output_file, 'w')

regex = re.compile(r'localhost:\d+/uaa')
for line in input.xreadlines():
    output.write(regex.sub('localhost:${UAA_LOCAL_PORT}/uaa', line))

input.close()
output.close()

shutil.move(output_file, input_file)
EOF
