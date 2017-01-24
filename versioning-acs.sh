#!/usr/bin/env bash

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

# This convenience script updates POM versions for all ACS modules

# Abort the script on failure of any command
set -ex

# Abort the script if the ACS version wasn't passed in
if [ -z "$1" ]; then
    echo "Please provide the ACS version."
    exit 2
fi

# Remove older ACS artifacts from the local repository and re-install the current version of all artifacts
# so that versions are correctly set by the Maven Versions plugin
rm -rf ~/.m2/repository/com/ge/predix/acs*
mvn clean install -s ../acs-ci-config/mvn_settings.xml -DskipTests

# Update the project versions in all POMs (main module and all submodules)
mvn versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$1
mvn clean install -s ../acs-ci-config/mvn_settings.xml -DskipTests

# Update the project version of the parent POM in the acs-integration-tests POM
cd acs-integration-tests
mvn versions:update-parent -DallowSnapshots=true -DgenerateBackupPoms=false -DparentVersion=$1
mvn versions:use-latest-versions -DallowSnapshots=true -DgenerateBackupPoms=false -Dincludes=com.ge.predix:acs-service
mvn clean install -s ../../acs-ci-config/mvn_settings.xml -Dmaven.test.skip
