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

{ set -e; } 2> /dev/null

function usage {
    echo "Usage: $( python -c "import os; print os.path.abspath('${BASH_SOURCE[0]}')" ) [--debug] [(-d | --delete-copyright-headers) | (-u | --upsert-copyright-headers)] [(-f | --file) <filename>]"
}

function read_args {
    while (( "$#" )); do
        case "$1" in
            -d|--delete-copyright-headers)
                DELETE_COPYRIGHTS='true'
                ;;
            -u|--upsert-copyright-headers)
                UPSERT_COPYRIGHTS='true'
                ;;
            -a|--normalize-authors)
                NORMALIZE_AUTHORS='true'
                ;;
            --debug)
                DEBUG='true'
                ;;
            -f|--file)
                shift
                SRC_FILE="$1"
                echo -e "Modifying source file: ${SRC_FILE}\n"
                ;;
            *)
                echo "Unknown option: ${1}"
                usage
                exit 2
                ;;
        esac
        shift
    done
}

NEWLINE='
'
COPYRIGHT_HEADER_TITLE="Copyright $( date +'%Y' ) General Electric Company"
COPYRIGHT_HEADER_BODY="${COPYRIGHT_HEADER_TITLE}
\n
Licensed under the Apache License, Version 2.0 (the \"License\");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
\n
    http://www.apache.org/licenses/LICENSE-2.0
\n
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an \"AS IS\" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
\n
SPDX-License-Identifier: Apache-2.0
"

function generate_copyright_header {
    if [[ "$1" == 'java' || "$1" == 'groovy' ]]; then
        BEGINNING_COMMENT_MARKER='/'
        CONTINUING_COMMENT_MARKER='*'
        ENDING_COMMENT_MARKER='/'
        BEGINNING_COMMENT_LINE="$( printf -- "${CONTINUING_COMMENT_MARKER}%.0s" $( seq 1 79 ) )"
    elif [[ "$1" == 'xml' ]]; then
        BEGINNING_COMMENT_MARKER='<!'
        CONTINUING_COMMENT_MARKER='-'
        ENDING_COMMENT_MARKER='>'
        BEGINNING_COMMENT_LINE="$( printf -- "${CONTINUING_COMMENT_MARKER}%.0s" $( seq 1 2 ) )"
    elif [[ "$1" == 'sh' || "$1" == 'properties' ]]; then
        BEGINNING_COMMENT_MARKER='#'
        CONTINUING_COMMENT_MARKER="$BEGINNING_COMMENT_MARKER"
        ENDING_COMMENT_MARKER="$CONTINUING_COMMENT_MARKER"
        BEGINNING_COMMENT_LINE="$( printf -- "${CONTINUING_COMMENT_MARKER}%.0s" $( seq 1 79 ) )"
    else
        return
    fi

    ENDING_COMMENT_LINE="$BEGINNING_COMMENT_LINE"

    COPYRIGHT_HEADER="$BEGINNING_COMMENT_MARKER"
    COPYRIGHT_HEADER="${COPYRIGHT_HEADER}${BEGINNING_COMMENT_LINE}${NEWLINE}"

    IFS="$NEWLINE"
    for l in $COPYRIGHT_HEADER_BODY; do
        if [[ "$BEGINNING_COMMENT_MARKER" != "$CONTINUING_COMMENT_MARKER" ]]; then
            COPYRIGHT_HEADER="${COPYRIGHT_HEADER} "
        fi
        COPYRIGHT_HEADER="${COPYRIGHT_HEADER}${CONTINUING_COMMENT_MARKER}"
        if [[ "$l" == '\n' ]]; then
            COPYRIGHT_HEADER="${COPYRIGHT_HEADER}${NEWLINE}"
        else
            COPYRIGHT_HEADER="${COPYRIGHT_HEADER} ${l}${NEWLINE}"
        fi
    done

    if [[ "$BEGINNING_COMMENT_MARKER" != "$CONTINUING_COMMENT_MARKER" ]]; then
        COPYRIGHT_HEADER="${COPYRIGHT_HEADER} "
    fi
    COPYRIGHT_HEADER="${COPYRIGHT_HEADER}${ENDING_COMMENT_LINE}${ENDING_COMMENT_MARKER}${NEWLINE}"

    echo "$COPYRIGHT_HEADER"
}

function delete_copyright {
    if [[ "$1" == 'java' || "$1" == 'groovy' ]]; then
        perl -i -pe 'BEGIN{undef $/;} s/\/\*{79}?\s*?\*\s*?Copyright.*?(\n\s*?\*.*?)+?\n\s*?\*{79}?\/\n{1,}//' "$2"
    elif [[ "$1" == 'xml' ]]; then
        perl -i -pe 'BEGIN{undef $/;} s/<!-{2}?\s*?-\s*?Copyright.*?(\n\s*?-.*?)+?\n\s*?-{2}?>\n{1,}//' "$2"
    elif [[ "$1" == 'sh' || "$1" == 'properties' ]]; then
        perl -i -pe 'BEGIN{undef $/;} s/#{80}?\s*?#\s*?Copyright.*?(\n\s*?#.*?)+?\n\s*?#{80}?\n{1,}//' "$2"
    fi
}

function upsert_copyright {
    delete_copyright "$1" "$2"

    local TMP_FILE="${2}.__tmp__"

    if [[ "$1" == 'xml' ]]; then
        echo -e '<?xml version="1.0" encoding="UTF-8"?>\n' > "$TMP_FILE"
    fi

    echo "$( generate_copyright_header "$1" )" >> "$TMP_FILE"
    echo '' >> "$TMP_FILE"

    if [[ "$1" == 'xml' ]]; then
        cat "$2" | awk '/<\?xml/{y=1;next}y' | sed '/./,$!d' >> "$TMP_FILE"
    else
        cat "$2" >> "$TMP_FILE"
    fi

    mv "$TMP_FILE" "$2"

    if [[ "$1" == 'sh' ]]; then
        chmod a+x "$2"
    fi
}

function normalize_authors {
    perl -i -pe 's/(\@author\s*)\d{1,}/$1acs-engineers\@ge.com/' "$2"
}

function modify_copyright_in_file {
    FILENAME="$( basename "$1" )"
    EXTENSION="${FILENAME##*.}"
    if [[ -n "$DELETE_COPYRIGHTS" ]]; then
        echo "Deleting copyrights from file: ${1} with extension: .${EXTENSION}"
        delete_copyright "$EXTENSION" "$1"
    elif [[ -n "$UPSERT_COPYRIGHTS" ]]; then
        echo "Upserting copyrights in file: ${1} with extension: .${EXTENSION}"
        upsert_copyright "$EXTENSION" "$1"
    fi

    if [[ -n "$NORMALIZE_AUTHORS" ]]; then
        echo "Normalizing author information in file: ${1} with extension: .${EXTENSION}"
        normalize_authors "$EXTENSION" "$1"
    fi
}

unset DELETE_COPYRIGHTS
unset UPSERT_COPYRIGHTS
unset NORMALIZE_AUTHORS
unset DEBUG
unset SRC_FILE

read_args "$@"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [[ -n "$DEBUG" ]]; then
    echo 'The following options are set:'
    echo "  DELETE_COPYRIGHTS: ${DELETE_COPYRIGHTS}"
    echo "  UPSERT_COPYRIGHTS: ${UPSERT_COPYRIGHTS}"
    echo "  NORMALIZE_AUTHORS: ${NORMALIZE_AUTHORS}"
    echo "  DEBUG: ${DEBUG}"
    echo "  SRC_FILE: ${SRC_FILE}"
    echo "  DIR: ${DIR}"
    echo ''
fi

if [[ -n "$DELETE_COPYRIGHTS" && -n "$UPSERT_COPYRIGHTS" ]]; then
    echo "Can't specify the deletion and upsert options at the same time"
    usage
    exit 2
fi

if [[ -n "$DEBUG" ]]; then
    generate_copyright_header 'java'
    generate_copyright_header 'groovy'
    generate_copyright_header 'sh'
    generate_copyright_header 'xml'
fi

if [[ -z "$SRC_FILE" ]]; then
    for f in $( find "$DIR" \( -not -path '*/\.*' -and -not -path '*/failsafe*' -and -not -path '*/surefire*' \) -type f \( -iname '*.groovy' -or -iname '*.java' -or -iname '*.sh' -or -iname '*.properties' -or -iname '*.xml' \) ); do
        modify_copyright_in_file "$f"
    done
else
    modify_copyright_in_file "$SRC_FILE"
fi
