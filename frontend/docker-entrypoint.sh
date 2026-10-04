#!/bin/sh
set -e
API="${API_BASE_URL:-http://localhost:8080}"
printf "window.BOOKFORWARD_CONFIG = { apiBase: '%s' };\n" "$API" > /usr/share/nginx/html/env.js
exec nginx -g 'daemon off;'
