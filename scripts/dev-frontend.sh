#!/bin/sh
# Serves the static frontend on http://localhost:5173 (needs python3).
cd "$(dirname "$0")/../frontend" && python3 -m http.server 5173
