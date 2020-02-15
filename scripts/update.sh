#!/bin/bash
set -e

pushd ~/arena/src/arena
git stash
git fetch
git pull
git stash apply
killall java
./mvnw -Pprod -Dmaven.test.skip=true -DskipTests package
cp ~/arena/src/arena/target/arena-0.0.1-SNAPSHOT.war ~/arena/arena.war
~/arena/arena.war > ~/arena/stdout 2> ~/arena/stderr &
popd

