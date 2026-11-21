#!/bin/bash
set -e

RELEASES=~/arena/releases
mkdir -p "$RELEASES"

pushd ~/arena/src/arena
git stash
git fetch
git pull
git stash apply

mvn clean
# npm and webpack are driven by frontend-maven-plugin inside the pom (it uses the
# node/npm pinned there, not the system ones), so there is no separate npm step
# here.  The pom uses `npm ci`, which installs package-lock.json exactly as
# committed and never rewrites it.
./mvnw -Pprod -Dmaven.test.skip=true -DskipTests package
popd

# Keep the war that is currently serving traffic before overwriting it, so that
# a rollback is a file copy rather than a rebuild.  See scripts/rollback.sh.
if [ -f ~/arena/arena.war ]; then
    cp ~/arena/arena.war "$RELEASES/arena-$(date '+%Y-%m-%d-%H%M%S').war"
fi

cp ~/arena/src/arena/target/arena-0.0.1-SNAPSHOT.war ~/arena/arena.war
killall java || true
export JAVA_OPTS="-Xmx2g"
~/arena/arena.war >> ~/arena/stdout 2>> ~/arena/stderr &
