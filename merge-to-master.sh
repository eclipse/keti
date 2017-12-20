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

YES='yes'
SKIP='skip'
NO='n'
CONFIRMATION_MESSAGE="Type '${YES}' to continue, '${SKIP}' to skip or '${NO}' to abort"

unset STEP_SKIPPED

function prompt {
    echo -e "\n\n${1}\n\n"
    read -p "${CONFIRMATION_MESSAGE}. [${YES}/${SKIP}/${NO}]: " ANSWER

    if [[ "$ANSWER" == "$SKIP" ]]; then
        STEP_SKIPPED='true'
    elif [[ "$ANSWER" == "$YES" ]]; then
        unset STEP_SKIPPED
    else
        echo -e '\nExiting...'
        exit 1
    fi
}

function check_internet {
    { set +e; } 2> /dev/null
    curl -k "$1" > /dev/null 2>&1
    if [[ "$?" -ne 0 ]]; then
        if [[ -n "$http_proxy" || -n "$https_proxy" ]]; then
            unset http_proxy
            unset HTTP_PROXY
            unset https_proxy
            unset HTTPS_PROXY
        else
            export http_proxy='http://proxy-src.research.ge.com:8080'
            export HTTP_PROXY="$http_proxy"
            export https_proxy="$http_proxy"
            export HTTPS_PROXY="$https_proxy"
        fi
        curl -k "$1" > /dev/null 2>&1
        if [[ "$?" -ne 0 ]]; then
            exit 1
        fi
    fi
    { set -e; } 2> /dev/null
}

function run_versioning_script {
    check_internet 'https://downloads.sourceforge.net'
    ./versioning.sh "$1"
    check_internet "https://${GIT_API_URI_PATH}"
}

function disable_pull_request_reviews_on_development_branch {
    { set -x; } 2>/dev/null
    local DISABLE_PROTECTION_RESPONSE="$(curl "https://${GIT_API_URI_PATH}/repos/${REPOSITORY_URI_PATH}/branches/${1}/protection/required_pull_request_reviews" -sI -X DELETE \
    -H "Authorization: token ${GIT_ACCESS_TOKEN}")"
    { set +x; } 2>/dev/null

    if [[ $(echo "$DISABLE_PROTECTION_RESPONSE" | grep -q '^HTTP.*204'; echo "$?") -ne 0 ]]; then
        echo -e "\nCouldn't disable protection on the ${1} branch. Exiting..."
        exit 1
    fi
}

function enable_pull_request_reviews_on_development_branch {
    { set -x; } 2>/dev/null
    local ENABLE_PROTECTION_RESPONSE="$(curl "https://${GIT_API_URI_PATH}/repos/${REPOSITORY_URI_PATH}/branches/${1}/protection/required_pull_request_reviews" -s -D - -X PATCH \
    -H "Authorization: token ${GIT_ACCESS_TOKEN}" \
    -H 'Content-Type: application/json' \
    -d '{ "dismiss_stale_reviews" : false, "require_code_owner_reviews" : false }')"
    { set +x; } 2>/dev/null

    if [[ $(echo "$ENABLE_PROTECTION_RESPONSE" | grep -q '^HTTP.*200'; echo "$?") -ne 0 ]]; then
        echo -e "\nCouldn't enable protection on the ${1} branch. Exiting..."
        exit 1
    fi
}

function update_version {
    if [[ -n "$1" ]]; then
        local SNAPSHOT_VERSION="${1}-SNAPSHOT"
        local DEVELOPMENT_BRANCH='develop'
        disable_pull_request_reviews_on_development_branch "$DEVELOPMENT_BRANCH"
        run_versioning_script "$SNAPSHOT_VERSION"
        git commit -am "Bumped up the version to ${SNAPSHOT_VERSION}"
        git push
        enable_pull_request_reviews_on_development_branch "$DEVELOPMENT_BRANCH"
        RELEASE_VERSION="$1"
    fi
}

{ set -ex; } 2>/dev/null

REPOSITORY_URI_PATH=$(git ls-remote --get-url | sed -n 's|.*[:/]\(.*\)/\(.*\)\.git|\1/\2|p')

# Set the GitHub API domain/URI path:
GIT_DOMAIN=$(git ls-remote --get-url | sed -n 's|\(.*\)[:/].*/.*\.git|\1|;s|.*[/@]\(.*\)|\1|p')
if [[ $(echo "$GIT_DOMAIN" | grep -q 'github\.com'; echo "$?") -eq 0 ]]; then
    GIT_API_URI_PATH="api.${GIT_DOMAIN}"
else
    GIT_API_URI_PATH="${GIT_DOMAIN}/api/v3"
fi

check_internet "https://${GIT_API_URI_PATH}"

# Get the personal access token necessary for using the GitHub API:
{ set +x; } 2>/dev/null
read -p "Please go to https://${GIT_DOMAIN}/settings/tokens, click the 'Edit' button next to your personal access token, click the 'Regenerate token' button towards the top, and enter the token shown: " GIT_ACCESS_TOKEN

if [[ -z "$GIT_ACCESS_TOKEN" ]]; then
    echo -e '\nYour personal access token is required before running this script. Exiting...'
    exit 1
else
    { set -x; } 2>/dev/null
    PR_GET_ALL="$(curl "https://${GIT_API_URI_PATH}/repos/${REPOSITORY_URI_PATH}/pulls" -sI -X GET \
    -H "Authorization: token ${GIT_ACCESS_TOKEN}")"
    { set +x; } 2>/dev/null
    if [[ $(echo "$PR_GET_ALL" | grep -q '^HTTP.*200'; echo "$?") -ne 0 ]]; then
        echo -e '\nYour personal access token is invalid. Please try again. Exiting...'
        exit 1
    fi
fi

prompt "About to merge changes from the develop to the master branch. Please stash any changes now via 'git stash save -u <stash_message>'."
{ set -x; } 2>/dev/null

# Checkout the develop branch as follows (note that any local and untracked changes will be removed so they should be stashed prior to running these commands):
git clean -fd
git checkout -f develop
git pull --all --prune --tags --verbose

# Cut a new release branch from develop, grabbing the current version from the top-level pom.xml file as follows:
RELEASE_VERSION=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml | sed 's/-SNAPSHOT//')

if [[ -z "$RELEASE_VERSION" ]]; then
    echo -e "\nThe release version can't be empty. Exiting..."
    exit 1
fi

if [[ -z "$STEP_SKIPPED" ]]; then
    { set +x; } 2>/dev/null
    echo -e "\n\nThe current version that will be used in the 'release-${RELEASE_VERSION}' branch is '${RELEASE_VERSION}'.\n\n"
    read -p "If you would like to update the version, please enter the new version (without the '-SNAPSHOT' suffix) or press Enter to continue using the existing version: " UPDATED_RELEASE_VERSION
    { set -x; } 2>/dev/null

    update_version "$UPDATED_RELEASE_VERSION"

    if [[ $(git branch -a | grep -q "remotes/origin/release-${RELEASE_VERSION}"; echo "$?") -ne 0 ]]; then
        git checkout -B "release-${RELEASE_VERSION}" develop

        # Remove the -SNAPSHOT suffix from all versions, verify the changes and commit them as follows:
        run_versioning_script "$RELEASE_VERSION"
        git commit -am "Changed the version to ${RELEASE_VERSION} (removes the '-SNAPSHOT' suffix)"

        # Merge the master branch into the release branch as follows:
        git checkout -f master
        git pull --all --prune --tags --verbose
        git checkout -
        { set +e; } 2>/dev/null
        git merge -X ours --no-edit master
        { set -e; } 2>/dev/null

        # Push the release branch upstream as follows:
        git push -u origin "release-${RELEASE_VERSION}"
    else
        { set +x; } 2>/dev/null
        echo -e '\nRelease branch has already been pushed.\n\n'
        { set -x; } 2>/dev/null
    fi
fi

{ set +x; } 2>/dev/null
prompt "About to create a pull request from this release branch and merge it in."
{ set -x; } 2>/dev/null

if [[ -z "$STEP_SKIPPED" ]]; then
    PR_GET_RESPONSE="$(curl "https://${GIT_API_URI_PATH}/repos/${REPOSITORY_URI_PATH}/pulls?state=open" -s -X GET \
    -H "Authorization: token ${GIT_ACCESS_TOKEN}")"

    RELEASE_TITLE="Release ${RELEASE_VERSION}"

    if [[ -z $(echo "$PR_GET_RESPONSE" | python -c "import sys, json; pull_requests = json.load(sys.stdin); title = [pr['title'] for pr in pull_requests if pr['title'] == '${RELEASE_TITLE}']; print title[0] if title else '';") ]]; then
        # Create a pull request for the release branch:
        PR_CREATE_RESPONSE="$(curl "https://${GIT_API_URI_PATH}/repos/${REPOSITORY_URI_PATH}/pulls" -s -X POST \
        -H "Authorization: token ${GIT_ACCESS_TOKEN}" \
        -H 'Content-Type: application/json' \
        -d '{ "title": "'"${RELEASE_TITLE}"'", "head": "release-'"$RELEASE_VERSION"'", "base": "master" }')"

        PR_JSON_RESPONSE="$PR_CREATE_RESPONSE"
    else
        PR_JSON_RESPONSE="$PR_GET_RESPONSE"

        { set +x; } 2>/dev/null
        echo -e '\nPull request was already created.\n\n'
        { set -x; } 2>/dev/null
    fi

    { set +x; } 2>/dev/null
    PULL_REQUEST_NUMBER=$(echo "$PR_JSON_RESPONSE" | python -c "import sys, json; print json.load(sys.stdin)['number']")
    PULL_REQUEST_TITLE=$(echo "$PR_JSON_RESPONSE" | python -c "import sys, json; print json.load(sys.stdin)['title']")
    PULL_REQUEST_HEAD_LABEL=$(echo "$PR_JSON_RESPONSE" | python -c "import sys, json; print json.load(sys.stdin)['head']['label']" | tr ':' '/')
    PULL_REQUEST_HEAD_SHA=$(echo "$PR_JSON_RESPONSE" | python -c "import sys, json; print json.load(sys.stdin)['head']['sha']")
    PULL_REQUEST_URL=$(echo "$PR_JSON_RESPONSE" | python -c "import sys, json; print json.load(sys.stdin)['url']")

    prompt "About to merge the pull request located at '${PULL_REQUEST_URL}'."
    { set -x; } 2>/dev/null

    if [[ -z "$STEP_SKIPPED" ]]; then
        PR_GET_MERGE_RESPONSE="$(curl "${PULL_REQUEST_URL}/merge" -sI -X GET \
        -H "Authorization: token ${GIT_ACCESS_TOKEN}")"

        if [[ $(echo "$PR_GET_MERGE_RESPONSE" | grep -q '^HTTP.*204'; echo "$?") -ne 0 ]]; then
            # Merge the pull request to master as follows:
            PR_MERGE_RESPONSE="$(curl "${PULL_REQUEST_URL}/merge" -s -X PUT \
            -H "Authorization: token ${GIT_ACCESS_TOKEN}" \
            -H 'Content-Type: application/json' \
            -d '{ "commit_title": "Merge pull request #'"$PULL_REQUEST_NUMBER"' from '"$PULL_REQUEST_HEAD_LABEL"'", "commit_message": "'"$PULL_REQUEST_TITLE"'", "sha": "'"$PULL_REQUEST_HEAD_SHA"'", "merge_method": "merge" }')"
        else
            { set +x; } 2>/dev/null
            echo -e '\nPull request was already merged.\n\n'
            { set -x; } 2>/dev/null
        fi
    fi
fi

{ set +x; } 2>/dev/null
prompt "About to delete the release branch remotely and locally."
{ set -x; } 2>/dev/null

if [[ -z "$STEP_SKIPPED" ]]; then
    # Delete the release branch remotely and locally as follows:
    git checkout -f master
    git pull --all --prune --tags --verbose

    if [[ $(git branch -a | grep -q "remotes/origin/release-${RELEASE_VERSION}"; echo "$?") -eq 0 ]]; then
        git push --delete origin "release-${RELEASE_VERSION}"
        git branch -D "release-${RELEASE_VERSION}"
    else
        { set +x; } 2>/dev/null
        echo -e '\nRelease branch has already been deleted.\n\n'
        { set -x; } 2>/dev/null
    fi
fi

{ set +x; } 2>/dev/null
prompt "About to tag the release."
{ set -x; } 2>/dev/null

if [[ -z "$STEP_SKIPPED" ]]; then
    # Pull the merged changes and tag the merge commit on master for future reference:
    git checkout -f master
    git pull --all --prune --tags --verbose

    if [[ $(git ls-remote --tags | grep -q "refs/tags/$RELEASE_VERSION"; echo "$?") -ne 0 ]]; then
        { set +e; } 2>/dev/null
        git tag -d "$RELEASE_VERSION"
        { set -e; } 2>/dev/null
        git tag "$RELEASE_VERSION"
        git show "$RELEASE_VERSION"
        git push origin "$RELEASE_VERSION"
    else
        { set +x; } 2>/dev/null
        echo -e '\nRelease has already been tagged.\n\n'
        { set -x; } 2>/dev/null
    fi
fi

git checkout -f develop
git pull --all --prune --tags --verbose

{ set +x; } 2>/dev/null
echo -e "\n\nThe current version that will be used in the 'develop' branch is '${RELEASE_VERSION}-SNAPSHOT'.\n\n"
read -p "If you would like to update the version, please enter the new version (without the '-SNAPSHOT' suffix) or press Enter to continue using the existing version: " UPDATED_RELEASE_VERSION
{ set -x; } 2>/dev/null

update_version "$UPDATED_RELEASE_VERSION"
