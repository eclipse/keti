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

CF_OUTPUT=$(cf t)
if [[ $CF_OUTPUT == *"Not logged in"* ]]; then
  echo $'\nYou must be logged into cloud foundry for this script to execute successfully.\n'
  exit 1
fi

CF_OUTPUT=$(cf s)
if [[ $CF_OUTPUT != *"predix-acs"* ]]; then
  echo $'\nPlease create an ACS service instance to continue.\n'
  exit 1
fi


echo $'\nWhat is the ACS trusted issuer? followed by [ENTER]:'

read ACS_UAA_URL

echo $'\nWhat is the UAA admin secret? followed by [ENTER]:'

read -s UAA_ADMIN_SECRET

echo $'\nWhat is the ACS service instance these clients should be created for? followed by [ENTER]:'

read ACS_SERVICE_NAME

ACS_GUID=$(cf service $ACS_SERVICE_NAME --guid)

if [[ $ACS_GUID == *"FAILED"* ]]; then
	echo $ACS_GUID
	exit 1
fi



uaac target $ACS_UAA_URL

uaac token client get admin -s $UAA_ADMIN_SECRET

ACS_ADMIN_SECRET=$(openssl rand -base64 25 | cut -c 1-25)

ACS_POLICY_EVALUATOR_CLIENT_ID=$ACS_SERVICE_NAME-policy-evaluator
ACS_ADMIN_CLIENT_ID=acs-admin

UAAC_OUTPUT=$(uaac client add $ACS_ADMIN_CLIENT_ID \
    --scope acs.attributes.read,acs.attributes.write,acs.policies.read,acs.policies.write,predix-acs.zones.$ACS_GUID.user \
    --authorized_grant_types authorization_code,client_credentials,password,refresh_token \
    --authorities acs.policies.read,acs.policies.write,acs.attributes.write,acs.attributes.read,predix-acs.zones.$ACS_GUID.user \
    --secret ${ACS_ADMIN_SECRET})
if [[ $UAAC_OUTPUT == *"error response"* ]]; then
	echo $UAAC_OUTPUT
	exit 1
fi


echo ""
echo "ACS admin client: $ACS_ADMIN_CLIENT_ID"
echo "ACS admin client secret: $ACS_ADMIN_SECRET"

ACS_POLICY_EVALUATOR_SECRET=$(openssl rand -base64 25 | cut -c 1-25)

UAAC_OUTPUT=$(uaac client add $ACS_POLICY_EVALUATOR_CLIENT_ID \
    --scope predix-acs.zones.$ACS_GUID.user \
    --authorized_grant_types client_credentials \
    --authorities predix-acs.zones.$ACS_GUID.user \
    --secret ${ACS_POLICY_EVALUATOR_SECRET})
if [[ $UAAC_OUTPUT == *"error response"* ]]; then
	echo $UAAC_OUTPUT
	exit 1
fi

echo ""
echo "ACS policy evaluator client: $ACS_POLICY_EVALUATOR_CLIENT_ID"
echo "ACS policy evaluator client secret: $ACS_POLICY_EVALUATOR_SECRET"