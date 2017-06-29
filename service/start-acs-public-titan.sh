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

unset PROXY_OPTS
export SPRING_PROFILES_ACTIVE='h2,public,simple-cache,titan'
export DIR=$( dirname "$( python -c "import os; print os.path.abspath('${BASH_SOURCE[0]}')" )" )
source "${DIR}/start-acs.sh" "$@"
