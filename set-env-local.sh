#!/usr/bin/env bash

export PORT_OFFSET=${PORT_OFFSET:-0}
export UAA_LOCAL_PORT=$(( 8080 + ${PORT_OFFSET} ))
export ACS_LOCAL_PORT=$(( 8181 + ${PORT_OFFSET} ))
export ZAC_LOCAL_PORT=$(( 8888 + ${PORT_OFFSET} ))

export ZAC_UAA_URL="http://localhost:${UAA_LOCAL_PORT}/uaa"
export ACS_UAA_URL="http://localhost:${UAA_LOCAL_PORT}/uaa"
export TRUSTED_ISSUER_ID="${ACS_UAA_URL}/oauth/token"
export ACS_URL="http://localhost:${ACS_LOCAL_PORT}"
export ZAC_URL="http://localhost:${ZAC_LOCAL_PORT}"

# Assumes GNU sed
sed -i "s/localhost:[0-9]\+\/uaa/localhost:${UAA_LOCAL_PORT}\/uaa/" ./acs-integration-tests/uaa/config/login.yml