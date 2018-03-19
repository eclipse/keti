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

unset SHOW_ROUTER_LOGS
unset SHOW_STAGING_LOGS
unset SHOW_APPLICATION_LOGS
unset MULTILINE_OUTPUT

while 'true'; do
    case "$1" in
        --rtr)
            SHOW_ROUTER_LOGS='true'
            shift
            ;;
        --stg)
            SHOW_STAGING_LOGS='true'
            shift
            ;;
        --app)
            SHOW_APPLICATION_LOGS='true'
            shift
            ;;
        --multiline)
            MULTILINE_OUTPUT='true'
            shift
            ;;
        --)
            shift
            break
            ;;
        *)
            if [[ -n "$1" ]]; then
                echo "'${1}' is not a valid option"
                exit 2
            fi
            break
            ;;
    esac
done

if [[ -z "$SHOW_ROUTER_LOGS" ]] && \
   [[ -z "$SHOW_STAGING_LOGS" ]] && \
   [[ -z "$SHOW_APPLICATION_LOGS" ]]; then
    SHOW_ROUTER_LOGS='true'
    SHOW_STAGING_LOGS='true'
    SHOW_APPLICATION_LOGS='true'
fi

unset LOG_REGEX

function print_simple_log_entry() {
    if [[ -n "$1" ]]; then

        PYTHON_LOGIC="${PYTHON_LOGIC}
    if ${3}.search(line) and not matched:"

        if [[ -n "$MULTILINE_OUTPUT" ]]; then
            PYTHON_LOGIC="${PYTHON_LOGIC}
        sys.stdout.write(bold + '${2}: ' + normal + line + '\n\n')
"
        else
            PYTHON_LOGIC="${PYTHON_LOGIC}
        sys.stdout.write(line + '\n')
"
        fi

    fi
}

function add_to_log_regex() {
    if [[ -z "$LOG_REGEX" ]]; then
        LOG_REGEX='(?:'
    else
        LOG_REGEX="${LOG_REGEX}|"
    fi
    LOG_REGEX="${LOG_REGEX}${1}"
}

unset RTR_REGEX
unset STG_REGEX
unset APP_REGEX

if [[ -n "$SHOW_ROUTER_LOGS" ]]; then
    RTR_REGEX='RTR.*?'
    add_to_log_regex "$RTR_REGEX"
fi
if [[ -n "$SHOW_STAGING_LOGS" ]]; then
    STG_REGEX='STG.*?'
    add_to_log_regex "$STG_REGEX"
fi
if [[ -n "$SHOW_APPLICATION_LOGS" ]]; then
    APP_REGEX='APP.*?'
    add_to_log_regex "$APP_REGEX"
fi

if [[ -n "$LOG_REGEX" ]]; then
    LOG_REGEX="${LOG_REGEX})"
fi

PYTHON_LOGIC="
import sys, re, json

regex_prefix=r'^((?:-?(?:[1-9][0-9]*)??[0-9]{4})-(?:1[0-2]|0[1-9])-(?:3[01]|0[1-9]|[12][0-9])T(?:2[0-3]|[01][0-9]):(?:[0-5][0-9]):(?:[0-5][0-9])(?:\.[0-9]+)??(?:Z|[+-](?:2[0-3]|[01][0-9])[0-5][0-9])??\s*?)'
no_cf_regex = re.compile(r'^(.*?)(\{.*\})\$')
json_regex = re.compile(regex_prefix + r'(\[${LOG_REGEX}\](?:\s*?[A-Z]+)??(?:\s+?)??)(\{.*\})\$')
"

if [[ -n "$RTR_REGEX" ]]; then
PYTHON_LOGIC="${PYTHON_LOGIC}
rtr_regex = re.compile(regex_prefix + r'\[${RTR_REGEX}\]')"
fi
if [[ -n "$STG_REGEX" ]]; then
PYTHON_LOGIC="${PYTHON_LOGIC}
stg_regex = re.compile(regex_prefix + r'\[${STG_REGEX}\]')"
fi
if [[ -n "$APP_REGEX" ]]; then
PYTHON_LOGIC="${PYTHON_LOGIC}
app_regex = re.compile(regex_prefix + r'\[${APP_REGEX}\]')"
fi

PYTHON_LOGIC="${PYTHON_LOGIC}
bold='\033[1m'
normal='\033[0m'

for line in sys.stdin:
    line = line.strip()
    matched = json_regex.search(line)
"

print_simple_log_entry "$SHOW_ROUTER_LOGS" 'Router' 'rtr_regex'
print_simple_log_entry "$SHOW_STAGING_LOGS" 'Staging' 'stg_regex'

if [[ -n "$SHOW_APPLICATION_LOGS" ]]; then

    PYTHON_LOGIC="${PYTHON_LOGIC}
    if app_regex.search(line) and matched:
        captured_groups = json_regex.match(line)
        line_as_json = json.loads(json_regex.sub(r'\3', line))
"

    PRINT_MULTILINE="
        if 'time' in line_as_json:
            sys.stdout.write(bold + 'Date and time: ' + normal + str(line_as_json['time']) + '\n')
        if 'lvl' in line_as_json:
            sys.stdout.write(bold + 'Log level: ' + normal + str(line_as_json['lvl']) + '\n')
        if 'tnt' in line_as_json:
            sys.stdout.write(bold + 'Tenant: ' + normal + str(line_as_json['tnt']) + '\n')
        if 'inst' in line_as_json:
            sys.stdout.write(bold + 'Instance ID: ' + normal + str(line_as_json['inst']) + '\n')
        if 'msg' in line_as_json:
            sys.stdout.write(bold + 'Message: ' + normal + str(line_as_json['msg']) + '\n')
        if 'stck' in line_as_json:
            sys.stdout.write(bold + 'Call stack: ' + normal + json.dumps(line_as_json['stck'], indent=4) + '\n')
        sys.stdout.write('\n')
"

    function print_single_line() {
        echo "
        if 'stck' in line_as_json:
            line_json_wo_stck = line_as_json.copy()
            del line_json_wo_stck['stck']
            sys.stdout.write(${1})
            sys.stdout.write(json.dumps(line_json_wo_stck).rstrip('}') + ',\n')
            sys.stdout.write('\"stck\": ' + json.dumps(line_as_json['stck'], indent=4) + '}\n')
        else:
            sys.stdout.write(line + '\n')
"
    }

    if [[ -n "$MULTILINE_OUTPUT" ]]; then
        PYTHON_LOGIC="${PYTHON_LOGIC}${PRINT_MULTILINE}"
    else
        PYTHON_LOGIC="${PYTHON_LOGIC}$(print_single_line 'captured_groups.group(1) + captured_groups.group(2)')"
    fi

    PYTHON_LOGIC="${PYTHON_LOGIC}
    elif no_cf_regex.search(line):
        captured_groups = no_cf_regex.match(line)
        line_as_json = json.loads(no_cf_regex.sub(r'\2', line))
"

    if [[ -n "$MULTILINE_OUTPUT" ]]; then
        PYTHON_LOGIC="${PYTHON_LOGIC}${PRINT_MULTILINE}"
    else
        PYTHON_LOGIC="${PYTHON_LOGIC}$(print_single_line 'captured_groups.group(1)')"
    fi

fi

python -c "$PYTHON_LOGIC"
