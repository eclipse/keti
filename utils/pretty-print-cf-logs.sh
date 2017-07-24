#!/usr/bin/env bash

# Based on: https://devcloud.swcoe.ge.com/devspace/display/RMOCM/Standard+Application+Logging+Pattern

unset SHOW_ROUTER_LOGS
unset SHOW_STAGING_LOGS
unset SHOW_APPLICATION_LOGS

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

unset LOG_REGEX

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

regex_prefix=r'^(?:-?(?:[1-9][0-9]*)??[0-9]{4})-(?:1[0-2]|0[1-9])-(?:3[01]|0[1-9]|[12][0-9])T(?:2[0-3]|[01][0-9]):(?:[0-5][0-9]):(?:[0-5][0-9])(?:\.[0-9]+)??(?:Z|[+-](?:2[0-3]|[01][0-9])[0-5][0-9])??\s*?'
json_regex = re.compile(regex_prefix + r'\[${LOG_REGEX}\](?:\s*?[A-Z]+)??(?:\s+?)??(\{.*\})\$')
"

if [[ -n "$RTR_REGEX" ]]; then
PYTHON_LOGIC="${PYTHON_LOGIC}
rtr_regex = re.compile(regex_prefix + r'\[${RTR_REGEX}.*?\]')
"
fi
if [[ -n "$STG_REGEX" ]]; then
PYTHON_LOGIC="${PYTHON_LOGIC}
stg_regex = re.compile(regex_prefix + r'\[${STG_REGEX}.*?\]')
"
fi
if [[ -n "$APP_REGEX" ]]; then
PYTHON_LOGIC="${PYTHON_LOGIC}
app_regex = re.compile(regex_prefix + r'\[${APP_REGEX}.*?\]')
"
fi

PYTHON_LOGIC="${PYTHON_LOGIC}
bold='\033[1m'
normal='\033[0m'

for line in sys.stdin:
    line = line.strip()
    matched = json_regex.search(line)
"

if [[ -n "$SHOW_ROUTER_LOGS" ]]; then
PYTHON_LOGIC="${PYTHON_LOGIC}
    if rtr_regex.search(line) and not matched:
        sys.stdout.write(bold + 'Router: ' + normal + line + '\n\n')
"
fi

if [[ -n "$SHOW_STAGING_LOGS" ]]; then
PYTHON_LOGIC="${PYTHON_LOGIC}
    if stg_regex.search(line) and not matched:
        sys.stdout.write(bold + 'Staging: ' + normal + line + '\n\n')
"
fi

if [[ -n "$SHOW_APPLICATION_LOGS" ]]; then
PYTHON_LOGIC="${PYTHON_LOGIC}
    if app_regex.search(line) and matched:
        line = json.loads(json_regex.sub(r'\1', line))
        if 'time' in line:
            sys.stdout.write(bold + 'Date and time: ' + normal + str(line['time']) + '\n')
        if 'lvl' in line:
            sys.stdout.write(bold + 'Log level: ' + normal + str(line['lvl']) + '\n')
        if 'tnt' in line:
            sys.stdout.write(bold + 'Tenant: ' + normal + str(line['tnt']) + '\n')
        if 'inst' in line:
            sys.stdout.write(bold + 'Instance ID: ' + normal + str(line['inst']) + '\n')
        if 'msg' in line:
            sys.stdout.write(bold + 'Message: ' + normal + str(line['msg']) + '\n')
        if 'stck' in line:
            sys.stdout.write(bold + 'Call stack: ' + normal + json.dumps(line['stck'], indent=4) + '\n')
        sys.stdout.write('\n')
"
fi

python -c "$PYTHON_LOGIC"
