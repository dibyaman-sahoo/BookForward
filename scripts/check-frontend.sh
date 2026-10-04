#!/bin/sh
# Syntax-checks every frontend module. Requires Node 18+.
set -e
for f in $(find "$(dirname "$0")/../frontend/js" -name '*.js'); do node --input-type=module --check < "$f" && echo "ok $f"; done
