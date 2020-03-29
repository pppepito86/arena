#!/bin/bash
set -e

pushd ~/arena/src/arena
git stash
git fetch
git pull
git stash apply
mvn clean
npm install
npm run webpack:build
npm run webpack:prod
./mvnw -Pprod -Dmaven.test.skip=true -DskipTests package
cp ~/arena/src/arena/target/arena-0.0.1-SNAPSHOT.war ~/arena/arena.war
killall java || true
~/arena/arena.war > ~/arena/stdout 2> ~/arena/stderr &
popd
