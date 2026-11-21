#!/bin/bash
# Restart the previously deployed war.  With no argument it picks the most
# recent file in ~/arena/releases; pass a path to choose a specific one.
#
#   scripts/rollback.sh
#   scripts/rollback.sh ~/arena/releases/arena-2026-08-27-231500.war
set -e

RELEASES=~/arena/releases

war="$1"
if [ -z "$war" ]; then
    war=$(ls -1t "$RELEASES"/arena-*.war 2>/dev/null | head -1)
fi

if [ -z "$war" ] || [ ! -f "$war" ]; then
    echo "No war to roll back to in $RELEASES" >&2
    exit 1
fi

echo "Rolling back to $war"
cp "$war" ~/arena/arena.war
killall java || true
export JAVA_OPTS="-Xmx2g"
~/arena/arena.war >> ~/arena/stdout 2>> ~/arena/stderr &
echo "Started.  Check ~/arena/stderr to confirm it came up."
