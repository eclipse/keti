#!/bin/bash
#*******************************************************************************
# Copyright 2016 General Electric Company. 
#
# Licensed under the Apache License, Version 2.0 (the "License"); 
# you may not use this file except in compliance with the License. 
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software 
# distributed under the License is distributed on an "AS IS" BASIS, 
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
# See the License for the specific language governing permissions and 
# limitations under the License.
#*******************************************************************************

# Abort script on failure of any command.
set -e -x

# This script is a convenience to update POM versions for all acs mvn projects

if [ -z "$1" ]; then
    echo "Please provide mvn POM version to set for ACS."
    exit 1
fi

# Update pom versions for acs and all child projects
mvn versions:set -DnewVersion=$1 -DgenerateBackupPoms=false
mvn clean install -s ../acs-ci-config/mvn_settings.xml -DskipTests

# Update parent version in acs-integration-tests
cd acs-integration-tests
mvn versions:update-parent -DparentVersion=[$1] -DgenerateBackupPoms=false
mvn clean install -s ../../acs-ci-config/mvn_settings.xml -DskipTests
