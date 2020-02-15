#!/bin/sh

wget -q -t 5 -O /dev/null arena.olimpiici.com

if [ $? -ne 0  ]
then
    date
    echo "The site is down. Restarting."
    pushd ~/workspace/arena
    killall java
    ~/workspace/bin/arena.war > stdout 2> stderr &
    popd
fi

